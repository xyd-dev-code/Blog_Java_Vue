package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.ProjectCategory;
import com.blog.service.ProjectCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台 - 项目分类")
@RestController
@RequestMapping("/api/v1/project-categories")
public class ProjectCategoryController {
    private final ProjectCategoryService categoryService;

    public ProjectCategoryController(ProjectCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "全部分类(已发布)")
    public R<List<ProjectCategory>> all() { return R.ok(categoryService.listAll()); }

    @GetMapping("/{slug}")
    @Operation(summary = "按 slug 查询")
    public R<ProjectCategory> bySlug(@PathVariable String slug) { return R.ok(categoryService.bySlug(slug)); }
}
