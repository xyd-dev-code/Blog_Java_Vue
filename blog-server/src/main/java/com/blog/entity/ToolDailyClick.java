package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

@TableName("tool_daily_click")
public class ToolDailyClick {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long toolId;
    private LocalDate clickDate;
    private Integer clickCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getToolId() { return toolId; }
    public void setToolId(Long toolId) { this.toolId = toolId; }
    public LocalDate getClickDate() { return clickDate; }
    public void setClickDate(LocalDate clickDate) { this.clickDate = clickDate; }
    public Integer getClickCount() { return clickCount; }
    public void setClickCount(Integer clickCount) { this.clickCount = clickCount; }
}