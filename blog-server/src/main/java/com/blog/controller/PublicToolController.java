package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.Tool;
import com.blog.service.ToolService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 在线工具箱")
@RestController
@RequestMapping("/api/v1/tools")
public class PublicToolController {

    private final ToolService toolService;

    public PublicToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    /** 前台工具列表(可选 category 过滤);返回 status=1 的工具 */
    @GetMapping
    public R<List<Tool>> list(@RequestParam(required = false) String category) {
        return R.ok(toolService.safeList(toolService.listPublished(category)));
    }

    @GetMapping("/{id}")
    public R<Tool> detail(@PathVariable Long id) {
        return R.ok(toolService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    public R<Tool> detailBySlug(@PathVariable String slug) {
        return R.ok(toolService.getBySlug(slug));
    }

    /** 累计点击 +1 + 今日点击 upsert */
    @PostMapping("/{id}/click")
    public R<Void> click(@PathVariable Long id) {
        toolService.recordClick(id);
        return R.ok();
    }

    /** 今日热门 Top N(给前端 banner 用) */
    @GetMapping("/hot")
    public R<List<Tool>> hot(@RequestParam(defaultValue = "4") int limit) {
        return R.ok(toolService.safeList(toolService.hotToday(Math.min(limit, 20))));
    }
}