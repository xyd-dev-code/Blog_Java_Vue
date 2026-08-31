package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.entity.Article;
import com.blog.vo.ArticlePublicVO;
import com.blog.entity.Category;
import com.blog.service.ArticleService;
import com.blog.service.CategoryService;
import com.blog.service.CommentService;
import com.blog.service.SiteConfigService;
import com.blog.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "前台 - 首页聚合")
@RestController
@RequestMapping("/api/v1")
public class HomeController {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CommentService commentService;
    private final SiteConfigService siteConfigService;

    public HomeController(ArticleService articleService,
                          CategoryService categoryService,
                          TagService tagService,
                          CommentService commentService,
                          SiteConfigService siteConfigService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.siteConfigService = siteConfigService;
    }

    @Operation(summary = "首页聚合数据")
    @GetMapping("/home")
    public R<Map<String, Object>> home() {
        Map<String, Object> map = new HashMap<>();
        List<Article> featured = articleService.listFeatured(3);
        List<Article> latest = articleService.listLatest(3);
        List<Category> categories = categoryService.listAll();
        List<com.blog.entity.Tag> tags = tagService.listAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("articleCount", articleService.countPublished());
        stats.put("categoryCount", categories.size());
        stats.put("tagCount", tags.size());
        stats.put("viewCount", articleService.sumViews());
        stats.put("commentCount", commentService.totalApproved());
        map.put("featured", ArticlePublicVO.list(featured));
        map.put("latest", ArticlePublicVO.list(latest));
        map.put("categories", categories);
        map.put("tags", tags);
        map.put("stats", stats);
        return R.ok(map);
    }

    @Operation(summary = "归档总览")
    @GetMapping("/archives")
    public R<Map<String, Object>> archives(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "50") long size) {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> months = articleService.archive();
        Page<Article> all = articleService.archivePage(page, size);
        map.put("months", months);
        map.put("articles", ArticlePublicVO.list(all.getRecords()));
        map.put("total", all.getTotal());
        map.put("page", all.getCurrent());
        map.put("size", all.getSize());
        return R.ok(map);
    }

    @Operation(summary = "归档按月")
    @GetMapping("/archives/{ym}")
    public R<List<ArticlePublicVO>> archiveMonth(@PathVariable @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "ym 格式须为 YYYY-MM") String ym) {
        return R.ok(ArticlePublicVO.list(articleService.listByMonth(ym)));
    }

    @Operation(summary = "按分类分页")
    @GetMapping("/categories/{slug}/articles")
    public R<Page<ArticlePublicVO>> articlesByCategory(@PathVariable String slug,
                                              @RequestParam(defaultValue = "1") @Min(1) long page,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) {
        return R.ok(ArticlePublicVO.page(articleService.pageByCategorySlug(slug, page, size)));
    }

    @Operation(summary = "按标签分页")
    @GetMapping("/tags/{slug}/articles")
    public R<Page<ArticlePublicVO>> articlesByTag(@PathVariable String slug,
                                          @RequestParam(defaultValue = "1") @Min(1) long page,
                                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) {
        return R.ok(ArticlePublicVO.page(articleService.pageByTagSlug(slug, page, size)));
    }
}