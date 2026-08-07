package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("tool_category")
public class ToolCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    @TableField("sort_order")
    private Integer sortOrder;
    /** 1=正常 0=下线(前台工具页隐藏) */
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createTime;
    @TableField("updated_at")
    private LocalDateTime updateTime;
    /** 关联工具数(非表字段,仅列表展示用) */
    @TableField(exist = false)
    private Long toolCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getToolCount() { return toolCount; }
    public void setToolCount(Long toolCount) { this.toolCount = toolCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
