package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class FriendLinkApplyDTO {
    @NotBlank(message = "站点名称不能为空")
    private String name;

    @NotBlank(message = "站点链接不能为空")
    private String url;

    private String avatar;

    @NotBlank(message = "站点简介不能为空")
    private String description;

    @Email(message = "邮箱格式不正确")
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
