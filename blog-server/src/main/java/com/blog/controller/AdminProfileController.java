package com.blog.controller;

import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.ProfileUpdateDTO;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtil;
import com.blog.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台 - 个人资料")
@RestController
@RequestMapping("/api/v1/admin/profile")
public class AdminProfileController {
    private final UserMapper userMapper;
    private final com.blog.service.AuthService authService;
    private final CommentService commentService;

    public AdminProfileController(UserMapper userMapper, com.blog.service.AuthService authService,
                                  CommentService commentService) {
        this.userMapper = userMapper;
        this.authService = authService;
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "当前管理员信息")
    public R<User> me() {
        Long uid = SecurityUtil.require().getId();
        User u = userMapper.selectById(uid);
        if (u == null) throw new BizException(404, "当前管理员账号不存在");
        u.setPassword(null);
        return R.ok(u);
    }

    @PutMapping
    @Operation(summary = "修改资料(昵称/头像/邮箱)")
    @Transactional
    public R<Void> update(@Valid @RequestBody ProfileUpdateDTO dto) {
        Long uid = SecurityUtil.require().getId();
        User previous = userMapper.selectById(uid);
        if (previous == null) throw new BizException("当前管理员账号不存在");

        User u = new User();
        u.setId(uid);
        u.setNickname(dto.getNickname());
        u.setAvatar(dto.getAvatar());
        u.setEmail(dto.getEmail());
        userMapper.updateById(u);

        // 使用数据库实际会保存的完整资料同步历史后台回复。
        User current = new User();
        current.setId(uid);
        current.setUsername(previous.getUsername());
        current.setNickname(dto.getNickname() != null ? dto.getNickname() : previous.getNickname());
        current.setAvatar(dto.getAvatar() != null ? dto.getAvatar() : previous.getAvatar());
        current.setEmail(dto.getEmail() != null ? dto.getEmail() : previous.getEmail());
        commentService.syncAdminReplyProfile(previous, current);
        return R.ok();
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(SecurityUtil.require(), dto, null);
        return R.ok();
    }
}
