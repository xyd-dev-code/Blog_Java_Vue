package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.LoginDTO;
import com.blog.entity.User;
import com.blog.security.JwtUtil;
import com.blog.security.SecurityUtil;
import com.blog.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "登录获取 token")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest req) {
        return R.ok(authService.login(dto, req));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户")
    public R<User> me() {
        return R.ok(authService.me(SecurityUtil.require()));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出(撤销 token 黑名单,前端清 token 即可)")
    public R<Void> logout(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        String token = jwtUtil.resolve(header);
        authService.logout(token);
        return R.ok();
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改当前用户密码(撤销旧 token)")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto, HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        String token = jwtUtil.resolve(header);
        authService.changePassword(SecurityUtil.require(), dto, token);
        return R.ok();
    }
}
