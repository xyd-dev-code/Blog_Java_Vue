package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.entity.Article;
import com.blog.service.ArticleService;
import com.blog.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "前台 - 文章")
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final TagService tagService;

    public ArticleController(ArticleService articleService, TagService tagService) {
        this.articleService = articleService;
        this.tagService = tagService;
    }

    @Operation(summary = "首页列表")
    @GetMapping
    public R<Page<Article>> home(@RequestParam(defaultValue = "1") @Min(1) long page,
                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) long size) {
        return R.ok(articleService.homePage(page, size));
    }

    @Operation(summary = "推荐文章")
    @GetMapping("/featured")
    public R<List<Article>> featured(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return R.ok(articleService.listFeatured(limit));
    }

    @Operation(summary = "搜索")
    @GetMapping("/search")
    public R<List<Article>> search(@RequestParam @Size(min = 2, max = 50, message = "搜索关键词 2~50 字") String q,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return R.ok(articleService.search(q, limit));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/{slug}")
    public R<Map<String, Object>> detail(@PathVariable String slug, HttpServletRequest req) {
        Article a = articleService.detailBySlug(slug);
        articleService.incrView(a.getId());
        a.setViewCount(a.getViewCount() + 1);
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("article", a);
        map.put("prev", articleService.prevNext(a.getId(), true));
        map.put("next", articleService.prevNext(a.getId(), false));
        map.put("related", articleService.related(a.getId()));
        return R.ok(map);
    }

    @Operation(summary = "归档按月")
    @GetMapping("/archive/{ym}")
    public R<List<Article>> byMonth(@PathVariable @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "ym 格式须为 YYYY-MM") String ym) {
        return R.ok(articleService.listByMonth(ym));
    }

    @Operation(summary = "归档总览")
    @GetMapping("/archive")
    public R<List<Map<String, Object>>> archive() {
        return R.ok(articleService.archive());
    }

    @Operation(summary = "按标签")
    @GetMapping("/by-tag/{tagId}")
    public R<List<Article>> byTag(@PathVariable Long tagId) {
        return R.ok(articleService.listByTag(tagId));
    }

    @Operation(summary = "按分类")
    @GetMapping("/by-category/{categoryId}")
    public R<List<Article>> byCategory(@PathVariable Long categoryId) {
        return R.ok(articleService.listByCategory(categoryId));
    }

    @Operation(summary = "热门标签云")
    @GetMapping("/tag-cloud")
    public R<List<Map<String, Object>>> tagCloud() {
        return R.ok(tagService.cloud());
    }
}
