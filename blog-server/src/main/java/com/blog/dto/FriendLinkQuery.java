package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FriendLinkQuery {
    @Pattern(regexp = "^[012]$", message = "status 必须 0/1/2")
    private Integer status;

    @Size(max = 50)
    private String keyword;

    @Min(1) private long page = 1;
    @Min(1) @Max(100) private long size = 10;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
