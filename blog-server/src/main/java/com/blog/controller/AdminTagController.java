package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.PageQuery;
import com.blog.service.TagService;
import com.blog.entity.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "后台 - 标签")
@RestController
@RequestMapping("/api/v1/admin/tags")
public class AdminTagController {
    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "分页")
    public R<Page<Tag>> page(PageQuery q) { return R.ok(tagService.page(q)); }

    @GetMapping("/all")
    public R<List<Tag>> all() { return R.ok(tagService.listAll()); }

    @GetMapping("/{id}")
    public R<Tag> byId(@PathVariable Long id) { return R.ok(tagService.byId(id)); }

    @PostMapping
    public R<Tag> create(@RequestBody Tag t) { return R.ok(tagService.save(t)); }

    @PutMapping("/{id}")
    public R<Tag> update(@PathVariable Long id, @RequestBody Tag t) {
        t.setId(id);
        return R.ok(tagService.update(t));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return R.ok();
    }
}
