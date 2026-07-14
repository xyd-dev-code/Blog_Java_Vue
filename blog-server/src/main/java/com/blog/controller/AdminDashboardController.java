package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.TagMapper;
import com.blog.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "后台 - 仪表盘")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    public AdminDashboardController(ArticleMapper articleMapper, CategoryMapper categoryMapper,
                                    TagMapper tagMapper, CommentMapper commentMapper, UserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "仪表盘聚合")
    public R<Map<String, Object>> dashboard() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stats", overviewStats());
        Page<Article> p = Page.of(1, 10);
        articleMapper.selectPage(p, new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getCreateTime));
        map.put("recentArticles", p.getRecords());
        List<Comment> pendings = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 0)
                .orderByDesc(Comment::getCreateTime)
                .last("LIMIT 10"));
        map.put("pendingComments", pendings);
        return R.ok(map);
    }

    @GetMapping("/overview")
    @io.swagger.v3.oas.annotations.Operation(summary = "汇总统计")
    public R<Map<String, Object>> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("articleCount", articleMapper.selectCount(null));
        map.put("publishedCount", articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)));
        map.put("draftCount", articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 0)));
        map.put("categoryCount", categoryMapper.selectCount(null));
        map.put("tagCount", tagMapper.selectCount(null));
        map.put("commentCount", commentMapper.selectCount(null));
        map.put("pendingCommentCount", commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 0)));

        Page<Article> p = Page.of(1, 5);
        articleMapper.selectPage(p, new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount));
        map.put("topArticles", p.getRecords());

        List<Comment> recent = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .orderByDesc(Comment::getCreateTime).last("LIMIT 5"));
        map.put("recentComments", recent);

        List<Article> all = articleMapper.selectList(null);
        long totalView = all.stream().mapToLong(a -> a.getViewCount() == null ? 0L : a.getViewCount()).sum();
        map.put("totalView", totalView);
        return R.ok(map);
    }

    private Map<String, Object> overviewStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("articleCount", articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)));
        s.put("categoryCount", categoryMapper.selectCount(null));
        s.put("tagCount", tagMapper.selectCount(null));
        s.put("commentCount", commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 1)));
        List<Article> all = articleMapper.selectList(null);
        long totalView = all.stream().mapToLong(a -> a.getViewCount() == null ? 0L : a.getViewCount()).sum();
        s.put("viewCount", totalView);
        return s;
    }
}