package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 站点配置(单条 key-value)DTO — 收口 AdminSiteController.save 的 raw Map。
 * 关键 URL 类字段(logo/favicon 等)必须 ^https?:// 开头,防 javascript: 注入。
 */
public class SiteConfigDTO {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,50}$", message = "key 只能含字母/数字/下划线/连字符")
    private String key;

    /**
     * value 按 key 前缀做不同校验:URL 类需 http/https;普通文本类限制长度。
     * 这里 base 上限 5000,Service 层在持久化前按 key 名再做一次白名单/格式校验。
     */
    @Size(max = 5000, message = "value 过长")
    private String value;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
