package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ProjectDTO;
import com.blog.dto.ProjectQuery;
import com.blog.entity.Project;
import com.blog.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台 - 项目")
@RestController
@RequestMapping("/api/v1/admin/projects")
public class AdminProjectController {
    private final ProjectService projectService;

    public AdminProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "分页查询")
    public R<Page<Project>> page(ProjectQuery q) {
        return R.ok(projectService.page(q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "详情")
    public R<Project> byId(@PathVariable Long id) {
        return R.ok(projectService.byId(id));
    }

    @PostMapping
    @Operation(summary = "创建")
    public R<Project> create(@Valid @RequestBody ProjectDTO dto) {
        return R.ok(projectService.save(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新")
    public R<Project> update(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto) {
        dto.setId(id);
        return R.ok(projectService.update(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "批量删除")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return R.ok();
        if (ids.size() > 100) {
            throw new com.blog.common.BizException("批量删除最多 100 条");
        }
        ids.stream().filter(java.util.Objects::nonNull).forEach(projectService::delete);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改状态 0下架 1发布")
    public R<Void> updateStatus(@PathVariable Long id,
                                @RequestParam @Min(value = 0, message = "status 必须 0/1")
                                @Max(value = 1, message = "status 必须 0/1") Integer status) {
        projectService.updateStatus(id, status);
        return R.ok();
    }
}
