package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    int deleteByArticle(@Param("articleId") Long articleId);

    @Delete("DELETE FROM article_tag WHERE tag_id = #{tagId}")
    int deleteByTag(@Param("tagId") Long tagId);
}