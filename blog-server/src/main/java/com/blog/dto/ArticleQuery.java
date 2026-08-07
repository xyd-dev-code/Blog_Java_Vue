package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ArticleQuery {
    @Pattern(regexp = "^[012]$", message = "status 必须 0/1/2")
    private Integer status;

    @Min(1) private Long categoryId;
    @Min(1) private Long tagId;

    @Size(max = 50, message = "keyword 过长")
    private String keyword;

    @Min(1) private long page = 1;
    @Min(1) @Max(100) private long size = 10;

    private boolean featuredOnly;
    private boolean topOnly;
    private String sortField = "publish_time";

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public boolean isFeaturedOnly() { return featuredOnly; }
    public void setFeaturedOnly(boolean featuredOnly) { this.featuredOnly = featuredOnly; }
    public boolean isTopOnly() { return topOnly; }
    public void setTopOnly(boolean topOnly) { this.topOnly = topOnly; }
    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }
}
