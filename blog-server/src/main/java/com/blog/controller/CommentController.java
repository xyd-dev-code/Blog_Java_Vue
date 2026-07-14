package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.CommentDTO;
import com.blog.entity.Comment;
import com.blog.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 评论")
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/article/{articleId}")
    @Operation(summary = "某文章的评论树 (仅已通过)")
    public R<List<Comment>> tree(@PathVariable Long articleId) {
        return R.ok(commentService.treeByArticle(articleId, false));
    }

    @GetMapping("/guestbook")
    @Operation(summary = "留言板公共评论树 (仅已通过)")
    public R<List<Comment>> guestbook() {
        return R.ok(commentService.guestbook());
    }

    @PostMapping
    @Operation(summary = "发表/提交评论")
    public R<Comment> create(@Valid @RequestBody CommentDTO dto, HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null) ip = req.getRemoteAddr();
        return R.ok(commentService.create(dto, ip));
    }
}
