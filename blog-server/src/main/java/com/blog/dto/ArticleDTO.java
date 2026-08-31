package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ArticleDTO {
    private Long id;

    @NotBlank
    @Size(max = 200, message = "标题不能超过 200 字符")
    private String title;

    @Size(max = 100)
    @Pattern(regexp = "^([\\u4e00-\\u9fa5a-z0-9_-]{1,100})?$",
             message = "slug 只能含中文/小写字母/数字/下划线/连字符")
    private String slug;

    @NotBlank
    @Size(max = 200_000, message = "正文不能超过 200k 字符")
    private String content;

    @Size(max = 500)
    private String summary;

    @Size(max = 500)
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "封面必须以 http/https 开头")
    private String coverImage;

    private Long categoryId;

    @Min(0) @Max(2)
    private Integer status;

    @Min(0) @Max(1)
    private Integer isTop;

    @Min(0) @Max(1)
    private Integer isFeatured;

    @Min(0) @Max(1)
    private Integer allowComment;

    @Size(max = 128)
    @Pattern(regexp = "^$", message = "暂不支持文章口令，请使用草稿状态保护非公开内容")
    private String password;

    private java.time.LocalDateTime publishTime;
    @Size(max = 30)
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> tagIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsTop() { return isTop; }
    public void setIsTop(Integer isTop) { this.isTop = isTop; }
    public Integer getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Integer isFeatured) { this.isFeatured = isFeatured; }
    public Integer getAllowComment() { return allowComment; }
    public void setAllowComment(Integer allowComment) { this.allowComment = allowComment; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public java.time.LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(java.time.LocalDateTime publishTime) { this.publishTime = publishTime; }
    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }
}
