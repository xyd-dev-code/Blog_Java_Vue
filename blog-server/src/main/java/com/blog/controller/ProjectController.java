package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.Project;
import com.blog.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 项目")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "已发布项目列表")
    @GetMapping
    public R<List<Project>> list() {
        return R.ok(projectService.listPublished());
    }
}
