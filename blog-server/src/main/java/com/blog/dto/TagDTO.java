package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TagDTO {
    private Long id;

    @NotBlank(message = "标签名不能为空")
    @Size(max = 30, message = "标签名不能超过 30 字符")
    private String name;

    @Size(max = 50)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,48}[a-z0-9]$", message = "slug 只能含小写字母/数字/连字符")
    private String slug;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
