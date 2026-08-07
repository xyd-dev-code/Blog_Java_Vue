package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.TagDTO;
import com.blog.dto.PageQuery;
import com.blog.service.TagService;
import com.blog.entity.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @io.swagger.v3.oas.annotations.Operation(summary = "分页(支持 keyword 模糊搜索 name/slug)")
    public R<Page<Tag>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(500) long size,
            @RequestParam(required = false) String keyword) {
        PageQuery q = new PageQuery();
        q.setPage(page);
        q.setSize(size);
        return R.ok(tagService.page(q, keyword));
    }

    @GetMapping("/all")
    public R<List<Tag>> all() { return R.ok(tagService.listAll()); }

    @GetMapping("/{id}")
    public R<Tag> byId(@PathVariable Long id) { return R.ok(tagService.byId(id)); }

    @PostMapping
    public R<Tag> create(@Valid @RequestBody TagDTO dto) { return R.ok(tagService.saveFromDTO(dto)); }

    @PutMapping("/{id}")
    public R<Tag> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        dto.setId(id);
        return R.ok(tagService.updateFromDTO(dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return R.ok();
    }
}
