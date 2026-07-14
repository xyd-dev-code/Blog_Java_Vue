package com.blog.controller;

import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "后台 - 个人资料")
@RestController
@RequestMapping("/api/v1/admin/profile")
public class AdminProfileController {
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;

    public AdminProfileController(UserMapper userMapper, PasswordEncoder encoder) {
        this.userMapper = userMapper;
        this.encoder = encoder;
    }

    @GetMapping
    @Operation(summary = "当前管理员信息")
    public R<User> me() {
        Long uid = SecurityUtil.require().getId();
        User u = userMapper.selectById(uid);
        u.setPassword(null);
        return R.ok(u);
    }

    @PutMapping
    @Operation(summary = "修改资料")
    public R<Void> update(@RequestBody User body) {
        Long uid = SecurityUtil.require().getId();
        User u = new User();
        u.setId(uid);
        u.setNickname(body.getNickname());
        u.setAvatar(body.getAvatar());
        u.setEmail(body.getEmail());
        userMapper.updateById(u);
        return R.ok();
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || newPwd == null || newPwd.length() < 6) throw new BizException("参数错误");
        Long uid = SecurityUtil.require().getId();
        User u = userMapper.selectById(uid);
        if (!encoder.matches(oldPwd, u.getPassword())) throw new BizException("旧密码错误");
        u.setPassword(encoder.encode(newPwd));
        userMapper.updateById(u);
        return R.ok();
    }
}
