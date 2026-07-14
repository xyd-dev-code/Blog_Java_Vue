package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.Tag;
import com.blog.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "前台 - 标签")
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "全部标签")
    public R<List<Tag>> all() { return R.ok(tagService.listAll()); }

    @GetMapping("/{slug}")
    @io.swagger.v3.oas.annotations.Operation(summary = "按 slug 查标签")
    public R<Tag> bySlug(@PathVariable String slug) { return R.ok(tagService.bySlug(slug)); }
}
