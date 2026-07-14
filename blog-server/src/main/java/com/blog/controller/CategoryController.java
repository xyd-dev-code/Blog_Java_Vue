package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.Category;
import com.blog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 分类")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "全部分类")
    public R<List<Category>> all() { return R.ok(categoryService.listAll()); }

    @GetMapping("/{slug}")
    @Operation(summary = "按 slug 查询")
    public R<Category> bySlug(@PathVariable String slug) { return R.ok(categoryService.bySlug(slug)); }
}
