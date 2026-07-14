package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQuery;
import com.blog.entity.Article;
import com.blog.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台 - 文章")
@RestController
@RequestMapping("/api/v1/admin/articles")
public class AdminArticleController {
    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(summary = "分页查询")
    public R<Page<Article>> page(ArticleQuery q) {
        return R.ok(articleService.pageAdmin(q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "文章详情（含标签）")
    public R<Article> byId(@PathVariable Long id) {
        return R.ok(articleService.detailById(id));
    }

    @PostMapping
    @Operation(summary = "创建文章")
    public R<Article> create(@Valid @RequestBody ArticleDTO dto) {
        return R.ok(articleService.save(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文章")
    public R<Article> update(@PathVariable Long id, @Valid @RequestBody ArticleDTO dto) {
        dto.setId(id);
        return R.ok(articleService.update(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    public R<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "批量删除")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        articleService.batchDelete(ids);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改状态 0草稿 1发布 2归档")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        articleService.updateStatus(id, status);
        return R.ok();
    }

    @PutMapping("/{id}/top")
    @Operation(summary = "置顶 0否 1是")
    public R<Void> updateTop(@PathVariable Long id, @RequestParam Integer top) {
        articleService.updateTop(id, top);
        return R.ok();
    }

    @PutMapping("/{id}/featured")
    @Operation(summary = "推荐 0否 1是")
    public R<Void> updateFeatured(@PathVariable Long id, @RequestParam Integer featured) {
        articleService.updateFeatured(id, featured);
        return R.ok();
    }

    @PutMapping("/{id}/view-count")
    @Operation(summary = "设置阅读量为指定值(article)")
    public R<Void> setViewCount(@PathVariable Long id, @RequestParam Long value) {
        articleService.setViewCount(id, value);
        return R.ok();
    }

    @PutMapping("/{id}/view-count/delta")
    @Operation(summary = "按增量调整阅读量(article),delta 可负,但结果不能 < 0")
    public R<Void> updateViewDelta(@PathVariable Long id, @RequestParam Long delta) {
        articleService.incrViewBy(id, delta);
        return R.ok();
    }
}
