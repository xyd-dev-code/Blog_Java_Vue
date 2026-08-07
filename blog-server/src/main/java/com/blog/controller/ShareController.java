package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.ShareClickReq;
import com.blog.dto.ShareClickResp;
import com.blog.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "分享")
@RestController
@RequestMapping("/api/v1/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/click")
    @Operation(summary = "记录一次分享点击(每日同 IP+渠道去重)")
    public R<ShareClickResp> click(@Valid @RequestBody ShareClickReq req, HttpServletRequest http) {
        ShareService.ShareResult r = shareService.recordShare(req.getArticleId(), req.getChannel(), http);
        return R.ok(new ShareClickResp(r.shareCount, r.counted));
    }
}