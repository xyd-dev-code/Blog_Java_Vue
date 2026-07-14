package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.entity.Comment;
import com.blog.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "后台 - 留言管理")
@RestController
@RequestMapping("/api/v1/admin/guestbook")
public class AdminGuestbookController {
    private final CommentService commentService;

    public AdminGuestbookController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "分页留言")
    public R<Page<Comment>> page(@RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "15") long size,
                                 @RequestParam(required = false) Integer status) {
        Page<Comment> p = Page.of(page, size);
        LambdaQueryWrapper<Comment> w = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, CommentService.GUESTBOOK_ARTICLE_ID)
                .orderByDesc(Comment::getCreateTime);
        if (status != null) w.eq(Comment::getStatus, status);
        Page<Comment> result = commentService.page(p, w);
        // 填充回复对象昵称
        List<Comment> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            commentService.fillParentNames(records);
        }
        return R.ok(result);
    }

    @GetMapping("/stats")
    @io.swagger.v3.oas.annotations.Operation(summary = "留言统计")
    public R<Map<String, Long>> stats() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("pending", commentService.countPendingGuestbook());
        map.put("approved", commentService.countApprovedGuestbook());
        map.put("spam", commentService.countSpamGuestbook());
        return R.ok(map);
    }

    @PutMapping("/{id}/approve")
    @io.swagger.v3.oas.annotations.Operation(summary = "通过")
    public R<Void> approve(@PathVariable Long id) {
        commentService.approve(id);
        return R.ok();
    }

    @PutMapping("/{id}/spam")
    @io.swagger.v3.oas.annotations.Operation(summary = "标记垃圾")
    public R<Void> spam(@PathVariable Long id) {
        commentService.markSpam(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "删除")
    public R<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return R.ok();
    }
}
