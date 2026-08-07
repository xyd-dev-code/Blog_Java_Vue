package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ToolCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ToolCategoryMapper extends BaseMapper<ToolCategory> {

    /** 统计每个分类下的未删除工具数,返回 [{code, cnt}] 列表 */
    @Select("SELECT category AS code, COUNT(*) AS cnt FROM tool " +
            "WHERE deleted = 0 GROUP BY category")
    List<Map<String, Object>> countToolsByCode();
}
