package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ToolCategoryDTO {

    private Long id;

    @NotBlank(message = "分类键不能为空")
    @Size(max = 30, message = "分类键最长 30 字符")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "分类键仅允许字母、数字、连字符")
    private String code;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最长 50 字符")
    private String name;

    private Integer sortOrder;
    /** 1=正常 0=下线 */
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
