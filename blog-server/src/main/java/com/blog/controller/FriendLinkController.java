package com.blog.controller;

import com.blog.common.R;
import com.blog.common.BizException;
import com.blog.dto.FriendLinkApplyDTO;
import com.blog.entity.FriendLink;
import com.blog.security.ClientIpResolver;
import com.blog.security.RateLimiter;
import com.blog.service.FriendLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 友链")
@RestController
@RequestMapping("/api/v1/friend-links")
public class FriendLinkController {
    private final FriendLinkService friendLinkService;
    private final ClientIpResolver ipResolver;
    private final RateLimiter rateLimiter;

    public FriendLinkController(FriendLinkService friendLinkService,
                                ClientIpResolver ipResolver,
                                RateLimiter rateLimiter) {
        this.friendLinkService = friendLinkService;
        this.ipResolver = ipResolver;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "已通过友链列表")
    @GetMapping
    public R<List<FriendLink>> list() {
        return R.ok(friendLinkService.listPublished());
    }

    @Operation(summary = "申请友链")
    @PostMapping("/apply")
    public R<FriendLink> apply(@Valid @RequestBody FriendLinkApplyDTO dto, HttpServletRequest req) {
        String ip = ipResolver.resolve(req);
        try {
            // IP 维度:挡批量灌水/撑爆审核表
            rateLimiter.acquireOrThrow("friendlink:apply:ip:" + ip, 3, 3600);
            // 邮箱维度:挡同一邮箱反复刷
            if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                rateLimiter.acquireOrThrow("friendlink:apply:email:" + dto.getEmail().toLowerCase(), 1, 86400);
            }
        } catch (BizException e) {
            throw e;
        }
        return R.ok(friendLinkService.apply(dto));
    }
}