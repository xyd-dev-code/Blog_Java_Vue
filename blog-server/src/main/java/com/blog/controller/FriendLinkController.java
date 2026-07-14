package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.FriendLinkApplyDTO;
import com.blog.entity.FriendLink;
import com.blog.service.FriendLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 友链")
@RestController
@RequestMapping("/api/v1/friend-links")
public class FriendLinkController {
    private final FriendLinkService friendLinkService;

    public FriendLinkController(FriendLinkService friendLinkService) {
        this.friendLinkService = friendLinkService;
    }

    @Operation(summary = "已通过友链列表")
    @GetMapping
    public R<List<FriendLink>> list() {
        return R.ok(friendLinkService.listPublished());
    }

    @Operation(summary = "申请友链")
    @PostMapping("/apply")
    public R<FriendLink> apply(@Valid @RequestBody FriendLinkApplyDTO dto) {
        return R.ok(friendLinkService.apply(dto));
    }
}
