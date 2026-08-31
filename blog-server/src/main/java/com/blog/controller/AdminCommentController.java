package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.AdminReplyDTO;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.service.CommentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@io.swagger.v3.oas.annotations.tags.Tag(name = "后台 - 评论")
@RestController
@RequestMapping("/api/v1/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;
    private final ArticleMapper articleMapper;

    public AdminCommentController(CommentService commentService, ArticleMapper articleMapper) {
        this.commentService = commentService;
        this.articleMapper = articleMapper;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "分页评论")
    public R<Page<Comment>> page(@RequestParam(defaultValue = "1") @Min(1) long page,
                                 @RequestParam(defaultValue = "15") @Min(1) @Max(200) long size,
                                 @RequestParam(required = false) @Min(0) @Max(2) Integer status) {
        Page<Comment> p = Page.of(page, size);
        LambdaQueryWrapper<Comment> w = new LambdaQueryWrapper<Comment>()
                .ne(Comment::getTargetType, CommentService.GUESTBOOK)
                .orderByDesc(Comment::getCreateTime);
        if (status != null) w.eq(Comment::getStatus, status);
        Page<Comment> result = commentService.page(p, w);
        // 填充文章标题
        List<Comment> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> ids = records.stream().map(Comment::getArticleId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                Map<Long, String> titleMap = new HashMap<>();
                articleMapper.selectBatchIds(ids).forEach(a -> titleMap.put(a.getId(), a.getTitle()));
                records.forEach(c -> c.setArticleTitle(titleMap.get(c.getArticleId())));
            }
            // 填充回复对象昵称
            commentService.fillParentNames(records);
        }
        return R.ok(result);
    }

    @GetMapping("/all")
    @io.swagger.v3.oas.annotations.Operation(summary = "所有评论（最近 200 条）")
    public R<List<Comment>> all() { return R.ok(commentService.treeAll()); }

    @GetMapping("/stats")
    @io.swagger.v3.oas.annotations.Operation(summary = "评论统计")
    public R<Map<String, Long>> stats() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("pending", commentService.countPending());
        map.put("approved", commentService.countApproved());
        map.put("spam", commentService.countSpam());
        return R.ok(map);
    }

    @GetMapping("/pending-count")
    @io.swagger.v3.oas.annotations.Operation(summary = "待审核评论数")
    public R<Map<String, Long>> pendingCount() {
        return R.ok(Map.of("count", commentService.countPending()));
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

    @PutMapping("/{id}/featured")
    @io.swagger.v3.oas.annotations.Operation(summary = "切换精选留言")
    public R<Void> featured(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        commentService.setFeatured(id, body != null && Boolean.TRUE.equals(body.get("featured")));
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "删除")
    public R<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/reply")
    @io.swagger.v3.oas.annotations.Operation(summary = "管理员回复评论")
    public R<Comment> reply(@PathVariable Long id, @RequestBody AdminReplyDTO dto) {
        return R.ok(commentService.adminReply(id, dto.getContent()));
    }
}