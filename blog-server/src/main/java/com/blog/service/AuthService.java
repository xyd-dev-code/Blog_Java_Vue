package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.dto.LoginDTO;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtUtil;
import com.blog.security.LoginUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(LoginDTO dto) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (u == null) throw new BizException(401, "用户名或密码错误");
        if (u.getStatus() != null && u.getStatus() == 0) throw new BizException(403, "账号已禁用");
        if (!encoder.matches(dto.getPassword(), u.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", sanitize(u));
        data.put("tokenInfo", jwtUtil.summary());
        return data;
    }

    public User me(LoginUser u) {
        if (u == null) throw new BizException(401, "未登录");
        User user = userMapper.selectById(u.getId());
        if (user == null) throw new BizException(404, "用户不存在");
        return sanitize(user);
    }

    private User sanitize(User u) {
        u.setPassword(null);
        return u;
    }
}
