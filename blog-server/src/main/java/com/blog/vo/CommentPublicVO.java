package com.blog.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论公开视图对象：剔除隐私字段(email / ip / ua)。
 *
 * <p>公开接口(文章评论树 / 留言板 / 提交评论回显)返回此对象，
 * 避免把评论者的真实邮箱、IP、User-Agent 暴露给任意匿名访客。
 * 后台管理接口仍使用完整 {@code Comment} 实体(管理员需邮箱用于回复通知)。</p>
 */
public class CommentPublicVO {
    private Long id;
    private Long articleId;
    private Long parentId;
    private String nickname;
    private String website;
    private String content;
    private String avatar;
    private Boolean isAdmin;
    private Integer status;
    private Integer likeCount;
    private Integer reportCount;
    private Integer contentType;
    private Integer featured;
    private LocalDateTime createTime;

    private String articleTitle;
    private String parentName;
    private List<CommentPublicVO> replies;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public Integer getContentType() { return contentType; }
    public void setContentType(Integer contentType) { this.contentType = contentType; }
    public Integer getFeatured() { return featured; }
    public void setFeatured(Integer featured) { this.featured = featured; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getArticleTitle() { return articleTitle; }
    public void setArticleTitle(String articleTitle) { this.articleTitle = articleTitle; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public List<CommentPublicVO> getReplies() { return replies; }
    public void setReplies(List<CommentPublicVO> replies) { this.replies = replies; }
}
