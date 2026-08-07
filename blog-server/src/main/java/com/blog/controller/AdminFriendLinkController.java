package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.FriendLinkDTO;
import com.blog.dto.FriendLinkQuery;
import com.blog.entity.FriendLink;
import com.blog.service.FriendLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台 - 友链")
@RestController
@RequestMapping("/api/v1/admin/friend-links")
public class AdminFriendLinkController {
    private final FriendLinkService friendLinkService;

    public AdminFriendLinkController(FriendLinkService friendLinkService) {
        this.friendLinkService = friendLinkService;
    }

    @GetMapping
    @Operation(summary = "分页查询")
    public R<Page<FriendLink>> page(FriendLinkQuery q) {
        return R.ok(friendLinkService.page(q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "详情")
    public R<FriendLink> byId(@PathVariable Long id) {
        return R.ok(friendLinkService.byId(id));
    }

    @PostMapping
    @Operation(summary = "创建")
    public R<FriendLink> create(@Valid @RequestBody FriendLinkDTO dto) {
        return R.ok(friendLinkService.save(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新")
    public R<FriendLink> update(@PathVariable Long id, @Valid @RequestBody FriendLinkDTO dto) {
        dto.setId(id);
        return R.ok(friendLinkService.update(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public R<Void> delete(@PathVariable Long id) {
        friendLinkService.delete(id);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "批量删除")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return R.ok();
        if (ids.size() > 100) {
            throw new com.blog.common.BizException("批量删除最多 100 条");
        }
        ids.stream().filter(java.util.Objects::nonNull).forEach(friendLinkService::delete);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "审核 0待审核 1已通过 2已拒绝")
    public R<Void> review(@PathVariable Long id,
                          @RequestParam @Min(value = 0, message = "status 不合法") @Max(value = 2, message = "status 不合法") Integer status) {
        friendLinkService.updateStatus(id, status);
        return R.ok();
    }

    @PutMapping("/{id}/recommended")
    @Operation(summary = "设为/取消推荐")
    public R<Void> setRecommended(@PathVariable Long id,
                                  @RequestParam Boolean recommended) {
        friendLinkService.setRecommended(id, Boolean.TRUE.equals(recommended));
        return R.ok();
    }
}
