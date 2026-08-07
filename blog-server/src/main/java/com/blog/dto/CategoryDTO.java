package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryDTO {
    private Long id;

    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名不能超过 50 字符")
    private String name;

    @Size(max = 50)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,48}[a-z0-9]$", message = "slug 只能含小写字母/数字/连字符")
    private String slug;

    @Size(max = 500, message = "描述不能超过 500 字符")
    private String description;

    @Min(0)
    @Max(99999)
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
