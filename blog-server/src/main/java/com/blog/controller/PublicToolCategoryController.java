package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.ToolCategory;
import com.blog.service.ToolCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台 - 工具分类")
@RestController
@RequestMapping("/api/v1/tool-categories")
public class PublicToolCategoryController {

    private final ToolCategoryService categoryService;

    public PublicToolCategoryController(ToolCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public R<List<ToolCategory>> list() {
        // 前台只返回正常(status=1)的分类,下线分类隐藏
        List<ToolCategory> all = categoryService.listAll();
        List<ToolCategory> visible = all.stream()
                .filter(c -> c.getStatus() == null || c.getStatus() == 1)
                .collect(java.util.stream.Collectors.toList());
        return R.ok(visible);
    }
}
