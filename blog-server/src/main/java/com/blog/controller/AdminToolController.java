package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ToolDTO;
import com.blog.dto.ToolReorderDTO;
import com.blog.entity.Tool;
import com.blog.service.ToolService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台 - 工具箱管理")
@RestController
@RequestMapping("/api/v1/admin/tools")
public class AdminToolController {

    private final ToolService toolService;

    public AdminToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public R<Page<Tool>> page(@RequestParam(defaultValue = "1") @Min(1) long page,
                               @RequestParam(defaultValue = "15") @Min(1) @Max(200) long size,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) Integer status,
                               @RequestParam(required = false) String keyword) {
        return R.ok(toolService.page(page, size, category, status, keyword));
    }

    @GetMapping("/next-sort")
    public R<Integer> nextSort() {
        return R.ok(toolService.nextSortOrder());
    }

    @GetMapping("/{id}")
    public R<Tool> detail(@PathVariable Long id) {
        return R.ok(toolService.getById(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ToolDTO dto) {
        return R.ok(toolService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ToolDTO dto) {
        toolService.update(id, dto);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody StatusBody body) {
        toolService.updateStatus(id, body == null ? null : body.getStatus());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        toolService.delete(id);
        return R.ok();
    }

    /** 拖拽排序:整组 ids 回传 */
    @PutMapping("/reorder")
    public R<Void> reorder(@Valid @RequestBody ToolReorderDTO body) {
        toolService.reorder(body.getIds());
        return R.ok();
    }

    public static class StatusBody {
        private Integer status;
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}