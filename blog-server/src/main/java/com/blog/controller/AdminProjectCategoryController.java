package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ProjectCategoryDTO;
import com.blog.entity.ProjectCategory;
import com.blog.service.ProjectCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台 - 项目分类")
@RestController
@RequestMapping("/api/v1/admin/project-categories")
public class AdminProjectCategoryController {
    private final ProjectCategoryService categoryService;

    public AdminProjectCategoryController(ProjectCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "分页(支持 keyword 模糊搜索 name/slug)")
    public R<Page<ProjectCategory>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(categoryService.page(page, size, keyword));
    }

    @GetMapping("/all")
    @Operation(summary = "全部(含下架，供项目表单下拉)")
    public R<List<ProjectCategory>> all() {
        return R.ok(categoryService.listAllIncludingUnpublished());
    }

    @GetMapping("/{id}")
    public R<ProjectCategory> byId(@PathVariable Long id) { return R.ok(categoryService.byId(id)); }

    @PostMapping
    public R<ProjectCategory> create(@Valid @RequestBody ProjectCategoryDTO dto) {
        return R.ok(categoryService.saveFromDTO(dto));
    }

    @PutMapping("/{id}")
    public R<ProjectCategory> update(@PathVariable Long id, @Valid @RequestBody ProjectCategoryDTO dto) {
        dto.setId(id);
        return R.ok(categoryService.updateFromDTO(dto));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "上架/下架(只改状态，不影响其它字段)")
    public R<Void> updateStatus(@PathVariable Long id,
                                @RequestParam @Min(0) @Max(1) Integer status) {
        categoryService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
