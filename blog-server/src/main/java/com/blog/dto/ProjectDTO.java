package com.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ProjectDTO {
    private Long id;

    @NotBlank
    @Size(max = 80, message = "项目名不能超过 80 字符")
    private String name;

    // 以下上限与 project 表列宽严格对齐(description/tech_stack VARCHAR(500)、
    // icon VARCHAR(50)、github_url/demo_url VARCHAR(255))。
    // 否则超长内容会绕过校验直到 INSERT 才抛 "Data too long"，前端只能拿到 500。
    @Size(max = 500, message = "描述不能超过 500 字符")
    private String description;

    // 注意：@Size 作用于 List 时校验的是元素个数，不是字符长度；
    // 拼接后的总长度由 ProjectService.join() 再兜一道。
    @Size(max = 20, message = "技术栈标签不能超过 20 个")
    private List<String> stack;

    @Size(max = 50, message = "icon 不能超过 50 字符")
    @Pattern(regexp = "^([a-zA-Z][a-zA-Z0-9-]*|https?://[^\\s]+)?$", message = "icon 须为 Element Plus 图标名或 http/https 链接")
    private String icon;

    @Size(max = 20)
    @Pattern(regexp = "^#[0-9A-Fa-f]{0,8}$", message = "color 须为 # 开头的 hex")
    private String color;

    @Size(max = 255, message = "githubUrl 不能超过 255 字符")
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "githubUrl 必须以 http/https 开头")
    private String githubUrl;

    @Size(max = 255, message = "demoUrl 不能超过 255 字符")
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "demoUrl 必须以 http/https 开头")
    private String demoUrl;

    @Size(max = 512)
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "coverUrl 必须以 http/https 开头")
    private String coverUrl;

    private Long categoryId;

    @Min(0)
    @Max(99999)
    private Integer sortOrder;

    @Min(0) @Max(1)
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getStack() { return stack; }
    public void setStack(List<String> stack) { this.stack = stack; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getDemoUrl() { return demoUrl; }
    public void setDemoUrl(String demoUrl) { this.demoUrl = demoUrl; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
