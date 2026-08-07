package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Category;
import com.blog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Operation(summary = "分页(支持 keyword 模糊搜索 name/slug)")
    public R<Page<Category>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(categoryService.page(page, size, keyword));
    }

    @GetMapping("/all")
    @Operation(summary = "全部")
    public R<List<Category>> all() { return R.ok(categoryService.listAll()); }

    @GetMapping("/{id}")
    public R<Category> byId(@PathVariable Long id) { return R.ok(categoryService.byId(id)); }

    @PostMapping
    public R<Category> create(@Valid @RequestBody CategoryDTO dto) { return R.ok(categoryService.saveFromDTO(dto)); }

    @PutMapping("/{id}")
    public R<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        dto.setId(id);
        return R.ok(categoryService.updateFromDTO(dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
