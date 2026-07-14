package com.blog.mapper;

import com.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagRelationMapper {

    @Select("SELECT a.* FROM article a INNER JOIN article_tag at ON a.id = at.article_id " +
            "WHERE at.tag_id = #{tagId} AND a.status = 1 AND a.deleted = 0 " +
            "ORDER BY a.is_top DESC, a.publish_time DESC")
    List<Article> findByTag(@Param("tagId") Long tagId);

    @Select("SELECT a.* FROM article a INNER JOIN article_tag at ON a.id = at.article_id " +
            "WHERE at.tag_id = #{tagId} AND a.status = 1 AND a.deleted = 0")
    List<Article> findAllByTagId(@Param("tagId") Long tagId);

    @Select("SELECT article_id FROM article_tag WHERE tag_id = #{tagId}")
    List<Long> findArticleIdsByTag(@Param("tagId") Long tagId);
}