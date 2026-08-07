package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FriendLinkApplyDTO {
    @NotBlank(message = "站点名称不能为空")
    @Size(max = 50, message = "站点名称过长")
    private String name;

    @NotBlank(message = "站点链接不能为空")
    @Size(max = 500, message = "站点链接过长")
    // 仅允许 http/https,防止 javascript: / data: 等伪协议被管理员通过后注入
    @Pattern(regexp = "^https?://[^\\s]+$", message = "站点链接必须以 http:// 或 https:// 开头")
    private String url;

    @Size(max = 500, message = "头像链接过长")
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "头像链接格式不正确")
    private String avatar;

    @NotBlank(message = "站点简介不能为空")
    @Size(max = 200, message = "站点简介过长")
    private String description;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;

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
}
