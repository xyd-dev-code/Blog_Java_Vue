package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrView(@Param("id") Long id);

    @Update("UPDATE article SET view_count = #{value} WHERE id = #{id}")
    int setViewCount(@Param("id") Long id, @Param("value") long value);

    @Update("UPDATE article SET view_count = GREATEST(0, view_count + #{delta}) WHERE id = #{id}")
    int incrByView(@Param("id") Long id, @Param("delta") long delta);

    @Select("SELECT DATE_FORMAT(publish_time, '%Y-%m') AS ym, COUNT(*) AS cnt " +
            "FROM article WHERE status = 1 AND deleted = 0 AND (password IS NULL OR password = '') AND publish_time IS NOT NULL " +
            "GROUP BY ym ORDER BY ym DESC")
    List<Map<String, Object>> archiveStats();

    @Select("SELECT COALESCE(SUM(view_count), 0) FROM article WHERE status = 1 AND deleted = 0 AND (password IS NULL OR password = '')")
    Long sumViews();
}
