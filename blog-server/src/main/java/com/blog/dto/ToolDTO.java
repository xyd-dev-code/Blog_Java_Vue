package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ToolDTO {
    @NotBlank
    @Size(max = 50, message = "工具名不能超过 50 字符")
    private String name;

    @NotBlank
    @Size(max = 50, message = "slug 不能超过 50 字符")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "slug 仅允许字母、数字、连字符")
    private String slug;

    @NotBlank
    @Size(max = 255)
    private String icon;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "category 仅允许字母、数字、连字符")
    private String category;

    @Size(max = 200)
    private String description;

    @Size(max = 500)
    private String url;

    /** 0=同页内嵌 1=外链 */
    @NotNull
    private Integer type;

    /** 0=下线 1=正常 2=维护中 3=预告 */
    @NotNull
    private Integer status;

    private Integer sortOrder;

    @Size(max = 200)
    private String announcement;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; }
}