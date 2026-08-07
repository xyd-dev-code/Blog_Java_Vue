package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ProjectQuery {
    // status 为 Integer，只能用 @Min/@Max；@Pattern 仅支持 CharSequence，
    // 一旦此类被 @Valid 校验就会抛 UnexpectedTypeException。
    @Min(value = 0, message = "status 必须 0/1")
    @Max(value = 1, message = "status 必须 0/1")
    private Integer status;

    @Size(max = 50)
    private String keyword;

    private Long categoryId;

    @Min(1) private long page = 1;
    @Min(1) @Max(100) private long size = 10;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
