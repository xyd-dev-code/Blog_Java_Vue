package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ToolDailyClick;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ToolDailyClickMapper extends BaseMapper<ToolDailyClick> {
    @org.apache.ibatis.annotations.Insert("INSERT INTO tool_daily_click(tool_id, click_date, click_count) VALUES(#{toolId}, #{date}, 1) ON DUPLICATE KEY UPDATE click_count = click_count + 1")
    int increment(@org.apache.ibatis.annotations.Param("toolId") Long toolId,
                  @org.apache.ibatis.annotations.Param("date") java.time.LocalDate date);
}
