package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.entity.Article;

/** One public access rule, including legacy password-protected records. */
public final class ArticleVisibility {
    private ArticleVisibility() { }

    public static LambdaQueryWrapper<Article> query() {
        return new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)
                .and(w -> w.isNull(Article::getPassword).or().eq(Article::getPassword, ""));
    }

    public static LambdaQueryWrapper<Article> summaries() {
        return query().select(Article::getId, Article::getTitle, Article::getSlug, Article::getSummary,
                Article::getCoverImage, Article::getCategoryId, Article::getStatus, Article::getIsTop,
                Article::getIsFeatured, Article::getAllowComment, Article::getPublishTime,
                Article::getCreateTime, Article::getUpdateTime, Article::getViewCount, Article::getShareCount);
    }

    public static boolean isPublic(Article article) {
        return article != null && Integer.valueOf(1).equals(article.getStatus())
                && !Integer.valueOf(1).equals(article.getDeleted())
                && (article.getPassword() == null || article.getPassword().isEmpty());
    }

    public static void requirePublic(Article article) {
        if (!isPublic(article)) throw new BizException(404, "文章不存在");
    }
}
