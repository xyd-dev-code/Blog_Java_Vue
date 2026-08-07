package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class PageQuery {
    @Min(1) private long page = 1;
    @Min(1) @Max(100) private long size = 10;

    @Pattern(regexp = "^[a-zA-Z_]{1,30}$", message = "sort 字段名不合法")
    private String sort;

    @Pattern(regexp = "^(asc|desc)$", message = "order 必须 asc/desc")
    private String order = "desc";

    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }
}
