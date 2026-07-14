package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.Page;
import com.blog.service.PageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台 - 页面")
@RestController
@RequestMapping("/api/v1/admin/pages")
public class AdminPageController {
    private final PageService pageService;

    public AdminPageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    @Operation(summary = "分页")
    public R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Page>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(pageService.page(page, size));
    }

    @GetMapping("/{id}")
    public R<Page> byId(@PathVariable Long id) { return R.ok(pageService.byId(id)); }

    @PostMapping
    public R<Page> create(@RequestBody Page p) { return R.ok(pageService.save(p)); }

    @PutMapping("/{id}")
    public R<Page> update(@PathVariable Long id, @RequestBody Page p) {
        p.setId(id);
        return R.ok(pageService.update(p));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        pageService.delete(id);
        return R.ok();
    }
}
