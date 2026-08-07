package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CommentDTO {
    @NotNull private Long articleId;
    private Long parentId;
    @NotBlank
    @Size(max = 30, message = "昵称不能超过 30 字符")
    private String nickname;
    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;
    @Size(max = 500, message = "网址过长")
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "网址必须以 http:// 或 https:// 开头")
    private String website;
    @Size(max = 500)
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "头像链接格式不正确")
    private String avatar;
    @NotBlank
    @Size(max = 1000, message = "评论内容不能超过 1000 字符")
    private String content;

    private Integer contentType;
    private String captchaToken;
    private String captchaAnswer;

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getContentType() { return contentType; }
    public void setContentType(Integer contentType) { this.contentType = contentType; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
    public String getCaptchaAnswer() { return captchaAnswer; }
    public void setCaptchaAnswer(String captchaAnswer) { this.captchaAnswer = captchaAnswer; }
}
