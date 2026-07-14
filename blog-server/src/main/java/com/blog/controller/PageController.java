package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.Page;
import com.blog.service.PageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 独立页面")
@RestController
@RequestMapping("/api/v1/pages")
public class PageController {
    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    @Operation(summary = "全部页面（导航）")
    public R<List<Page>> all() { return R.ok(pageService.listAll()); }

    @GetMapping("/{slug}")
    @Operation(summary = "按 slug 查询")
    public R<Page> bySlug(@PathVariable String slug) { return R.ok(pageService.bySlug(slug)); }
}
