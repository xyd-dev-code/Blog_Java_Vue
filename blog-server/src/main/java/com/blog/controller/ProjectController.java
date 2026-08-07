package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ProjectQuery;
import com.blog.entity.Project;
import com.blog.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台 - 项目")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "已发布项目列表(支持分页与分类筛选)")
    @GetMapping
    public R<Page<Project>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) long size) {
        ProjectQuery q = new ProjectQuery();
        q.setCategoryId(categoryId);
        q.setPage(page);
        q.setSize(size);
        return R.ok(projectService.publishedPage(q));
    }
}
