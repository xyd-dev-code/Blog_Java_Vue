package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class FriendLinkDTO {
    private Long id;
    @NotBlank private String name;
    @NotBlank private String url;
    private String avatar;
    private String description;
    private String email;
    private String linkGroup;
    private Integer sortOrder;
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
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
