package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.LoginDTO;
import com.blog.entity.User;
import com.blog.security.SecurityUtil;
import com.blog.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "登录获取 token")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户")
    public R<User> me() {
        return R.ok(authService.me(SecurityUtil.require()));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（前端清 token 即可）")
    public R<Void> logout() {
        return R.ok();
    }
}
