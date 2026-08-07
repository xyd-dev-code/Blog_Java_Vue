package com.blog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 后台拖拽排序请求体:整组 ids 按新顺序回传 */
public class ToolReorderDTO {
    @NotNull
    private List<@NotNull Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}