package com.blog.vo;

import com.blog.entity.Article;
import com.blog.entity.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import java.time.LocalDateTime;
import java.util.List;

/** Explicit public response fields; persistence-only fields never cross this boundary. */
@Data
public class ArticlePublicVO {
    private Long id;
    private String title, slug, content, summary, coverImage;
    private Long categoryId;
    private Integer status, isTop, isFeatured, allowComment;
    private LocalDateTime publishTime, createTime, updateTime;
    private Long viewCount, commentCount;
    private Integer shareCount;
    private String categoryName, categorySlug, author, authorAvatar;
    private List<Tag> tags;

    public static ArticlePublicVO from(Article article) {
        if (article == null) return null;
        ArticlePublicVO view = new ArticlePublicVO();
        BeanUtils.copyProperties(article, view);
        return view;
    }
    public static List<ArticlePublicVO> list(List<Article> articles) {
        return articles.stream().map(ArticlePublicVO::from).toList();
    }
    public static Page<ArticlePublicVO> page(Page<Article> source) {
        Page<ArticlePublicVO> page = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        page.setRecords(list(source.getRecords()));
        return page;
    }
}
