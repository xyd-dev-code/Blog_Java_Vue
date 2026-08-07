package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.ToolCategoryDTO;
import com.blog.entity.OperationLog;
import com.blog.entity.ToolCategory;
import com.blog.service.OperationLogService;
import com.blog.service.ToolCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "后台 - 工具分类管理")
@RestController
@RequestMapping("/api/v1/admin/tool-categories")
public class AdminToolCategoryController {

    private static final String MODULE = "工具分类";

    private final ToolCategoryService categoryService;
    private final OperationLogService logService;

    public AdminToolCategoryController(ToolCategoryService categoryService, OperationLogService logService) {
        this.categoryService = categoryService;
        this.logService = logService;
    }

    @GetMapping
    public R<List<ToolCategory>> list() {
        return R.ok(categoryService.listAll());
    }

    @GetMapping("/{id}")
    public R<ToolCategory> detail(@PathVariable Long id) {
        return R.ok(categoryService.getById(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ToolCategoryDTO dto) {
        return R.ok(categoryService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ToolCategoryDTO dto) {
        categoryService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }

    /** 切换状态:1=正常 0=下线 */
    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        categoryService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    /** 本模块最近操作日志(抽屉展示) */
    @GetMapping("/logs")
    public R<List<OperationLog>> logs(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(logService.recent(MODULE, limit));
    }

    /** 拖拽排序:整组 ids 回传 */
    @PutMapping("/reorder")
    public R<Void> reorder(@RequestBody List<Long> ids) {
        categoryService.reorder(ids);
        return R.ok();
    }
}
