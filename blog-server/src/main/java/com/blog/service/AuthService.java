package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.dto.LoginDTO;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtUtil;
import com.blog.security.LoginUser;
import com.blog.security.RateLimiter;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RateLimiter rateLimiter;
    private final com.blog.security.ClientIpResolver ipResolver;
    private final com.blog.security.JwtBlacklist blacklist;

    /** 用于"用户不存在时也跑一次"恒定时间的假 hash(预生成合法 BCrypt 摘要,任意明文都失败) */
    private final String dummyHash;
    private final boolean dummyHashValid;

    public AuthService(UserMapper userMapper, PasswordEncoder encoder, JwtUtil jwtUtil,
                       RateLimiter rateLimiter, com.blog.security.ClientIpResolver ipResolver,
                       com.blog.security.JwtBlacklist blacklist,
                       @Value("${blog.security.dummy-bcrypt-hash:!BOOTSTRAP_REQUIRED!u}") String dummyHash) {
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
        this.ipResolver = ipResolver;
        this.blacklist = blacklist;
        this.dummyHash = dummyHash;
        // 启动时验证 dummy hash 格式合法;若不能,降级 — 避免部署时哑配置导致时序侧信道失效
        this.dummyHashValid = dummyHash.startsWith("$2");
    }

    public Map<String, Object> login(LoginDTO dto, HttpServletRequest req) {
        // 1) IP 维度限流:挡扫端口/撞库
        String ip = ipResolver.resolve(req);
        rateLimiter.acquireOrThrow("login:ip:" + ip, 10, 60);
        // 2) username 维度限流:挡定向撞某账号
        String key = "login:user:" + dto.getUsername().toLowerCase() + ":" + ip;
        rateLimiter.acquireOrThrow(key, 5, 60);

        // 3) 找用户;找不到也跑 dummy bcrypt 恒定时间,防时序侧信道枚举用户名
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (u == null) {
            // 跑一次假比对,消耗跟真比对一样的时间(~250ms @ cost=12)
            if (dummyHashValid) encoder.matches(dto.getPassword(), dummyHash);
            throw new BizException(401, "用户名或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            // 同样跑 dummy 保持时序一致
            if (dummyHashValid) encoder.matches(dto.getPassword(), dummyHash);
            throw new BizException(403, "账号已禁用");
        }
        if (!encoder.matches(dto.getPassword(), u.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(u);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", sanitize(u));
        data.put("tokenInfo", jwtUtil.summary());
        return data;
    }

    /**
     * 登出:把当前 token 的 jti 加进黑名单直到自然过期。
     * 撤销记录持久化至数据库，重启及多实例均生效。
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        Claims claims;
        try { claims = jwtUtil.parse(token); }
        catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) { return; }
        blacklist.revoke(claims.getId(), claims.getExpiration().getTime());
    }

    public User me(LoginUser u) {
        if (u == null) throw new BizException(401, "未登录");
        User user = userMapper.selectById(u.getId());
        if (user == null) throw new BizException(404, "用户不存在");
        return sanitize(user);
    }

    /**
     * 修改密码:更新 BCrypt 后，所有旧 JWT 的凭据版本立即失效。
     * 调用方需把当前请求的 Authorization Bearer token 传进来,用于 revoke。
     */
    public void changePassword(LoginUser u, com.blog.dto.ChangePasswordDTO dto, String currentToken) {
        if (u == null) throw new BizException(401, "未登录");
        if (dto == null) throw new BizException("参数缺失");
        if (dto.getOldPassword() == null || dto.getOldPassword().isBlank()) {
            throw new BizException("旧密码不能为空");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new BizException("新密码不能为空");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BizException("新密码不能与旧密码相同");
        }
        User user = userMapper.selectById(u.getId());
        if (user == null) throw new BizException(404, "用户不存在");
        if (!encoder.matches(dto.getOldPassword(), user.getPassword())) {
            // 当前 JWT 已通过认证；这里只是业务输入不匹配，不能返回 401 触发全局登出。
            throw new BizException("旧密码错误");
        }
        User upd = new User();
        upd.setId(user.getId());
        upd.setPassword(encoder.encode(dto.getNewPassword()));
        int changed = userMapper.update(upd, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId()).eq(User::getPassword, user.getPassword()));
        if (changed != 1) throw new BizException(409, "密码已发生变更，请重新登录");
        // 撤销当前 token 的 jti:即便前端没 logout,旧 token 也不能再访问
        if (currentToken != null && !currentToken.isBlank()) {
            try {
                Claims c = jwtUtil.parse(currentToken);
                String jti = c.getId();
                long expMs = c.getExpiration() == null ? 0L : c.getExpiration().getTime();
                if (jti != null) blacklist.revoke(jti, expMs);
            } catch (Exception ignore) {
                // token 无效不影响改密成功
            }
        }
    }

    private User sanitize(User u) {
        u.setPassword(null);
        return u;
    }
}
