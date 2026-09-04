package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FriendLinkDTO {
    private Long id;
    @NotBlank @Size(max = 50) private String name;
    @NotBlank @Size(max = 255)
    @Pattern(regexp = "^https?://[^\\s]+$", message = "站点链接必须以 http:// 或 https:// 开头")
    private String url;
    @Size(max = 255)
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "头像链接格式不正确")
    private String avatar;
    @Size(max = 200) private String description;
    @Email(message = "邮箱格式不正确")
    @Size(max = 100) private String email;
    @Size(max = 50) private String linkGroup;
    @Min(value = 0, message = "sortOrder 不能为负")
    @Max(value = 10000, message = "sortOrder 过大")
    private Integer sortOrder;
    @Min(value = 0, message = "recommended 不合法")
    @Max(value = 1, message = "recommended 不合法")
    private Integer recommended;
    @Min(value = 0, message = "status 不合法")
    @Max(value = 2, message = "status 不合法")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLinkGroup() { return linkGroup; }
    public void setLinkGroup(String linkGroup) { this.linkGroup = linkGroup; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getRecommended() { return recommended; }
    public void setRecommended(Integer recommended) { this.recommended = recommended; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
