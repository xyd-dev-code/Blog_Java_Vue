package com.blog.controller;

import com.blog.common.R;
import com.blog.common.BizException;
import com.blog.dto.CommentDTO;
import com.blog.dto.CommentReportDTO;
import com.blog.security.ClientIpResolver;
import com.blog.security.LoginUser;
import com.blog.security.RateLimiter;
import com.blog.security.SecurityUtil;
import com.blog.service.CaptchaService;
import com.blog.service.CommentService;
import com.blog.vo.CommentPublicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "前台 - 评论")
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;
    private final ClientIpResolver ipResolver;
    private final RateLimiter rateLimiter;
    private final CaptchaService captchaService;

    public CommentController(CommentService commentService, ClientIpResolver ipResolver,
                             RateLimiter rateLimiter, CaptchaService captchaService) {
        this.commentService = commentService;
        this.ipResolver = ipResolver;
        this.rateLimiter = rateLimiter;
        this.captchaService = captchaService;
    }

    @GetMapping("/article/{articleId}")
    @Operation(summary = "某文章的评论树 (仅已通过)")
    public R<List<CommentPublicVO>> tree(@PathVariable Long articleId) {
        return R.ok(commentService.treeByArticle(articleId, false));
    }

    @GetMapping("/guestbook")
    @Operation(summary = "留言板公共评论树 (仅已通过)")
    public R<List<CommentPublicVO>> guestbook() {
        return R.ok(commentService.guestbook());
    }

    @GetMapping("/captcha")
    @Operation(summary = "获取留言验证码（算术题）")
    public R<Map<String, Object>> captcha() {
        return R.ok(captchaService.generate());
    }

    @PostMapping
    @Operation(summary = "发表/提交评论")
    public R<CommentPublicVO> create(@Valid @RequestBody CommentDTO dto, HttpServletRequest req) {
        String ip = ipResolver.resolve(req);
        // 验证码校验（开关走站点配置 captcha_enabled）
        if (!captchaService.verify(dto.getCaptchaToken(), dto.getCaptchaAnswer())) {
            throw new BizException("验证码错误，请重新计算");
        }
        // 公开接口必须有 RateLimit,否则攻击者可高频灌水/撑爆审核表
        try {
            rateLimiter.acquireOrThrow("comment:ip:" + ip, 5, 60);
        } catch (BizException e) {
            // 透传 429,前端好处理
            throw e;
        }
        String ua = req.getHeader("User-Agent");
        return R.ok(commentService.toPublic(commentService.create(dto, ip, ua)));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞 / 取消点赞留言或评论")
    public R<Map<String, Object>> like(@PathVariable Long id, HttpServletRequest req) {
        String ip = ipResolver.resolve(req);
        Long userId = null;
        LoginUser u = SecurityUtil.current();
        if (u != null) userId = u.getId();
        return R.ok(commentService.like(id, ip, userId));
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "举报留言或评论")
    public R<Void> report(@PathVariable Long id, @Valid @RequestBody CommentReportDTO dto,
                          HttpServletRequest req) {
        String ip = ipResolver.resolve(req);
        // 举报限频:同 IP 1 小时内最多 10 次,防刷举报
        try {
            rateLimiter.acquireOrThrow("report:ip:" + ip, 10, 3600);
        } catch (BizException e) {
            throw e;
        }
        commentService.report(id, dto.getReason(), dto.getDetail(), dto.getEmail(), ip);
        return R.ok();
    }
}
