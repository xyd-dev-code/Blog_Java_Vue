package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.PageQuery;
import com.blog.entity.Category;
import com.blog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台 - 分类")
@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "分页")
    public R<Page<Category>> page(PageQuery q) { return R.ok(categoryService.page(q)); }

    @GetMapping("/all")
    @Operation(summary = "全部")
    public R<List<Category>> all() { return R.ok(categoryService.listAll()); }

    @GetMapping("/{id}")
    public R<Category> byId(@PathVariable Long id) { return R.ok(categoryService.byId(id)); }

    @PostMapping
    public R<Category> create(@RequestBody Category c) { return R.ok(categoryService.save(c)); }

    @PutMapping("/{id}")
    public R<Category> update(@PathVariable Long id, @RequestBody Category c) {
        c.setId(id);
        return R.ok(categoryService.update(c));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
