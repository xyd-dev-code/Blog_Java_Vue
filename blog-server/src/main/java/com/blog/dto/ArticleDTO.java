package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ArticleDTO {
    private Long id;
    @NotBlank private String title;
    private String slug;
    @NotBlank private String content;
    private String summary;
    private String coverImage;
    private Long categoryId;
    private Integer status;
    private Integer isTop;
    private Integer isFeatured;
    private Integer allowComment;
    private String password;
    private java.time.LocalDateTime publishTime;
    private List<Long> tagIds;

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
