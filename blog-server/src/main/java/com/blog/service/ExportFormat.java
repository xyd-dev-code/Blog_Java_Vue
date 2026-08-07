package com.blog.service;

import com.blog.entity.Article;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

public enum ExportFormat {
    MD("md", MediaType.TEXT_MARKDOWN_VALUE),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    PDF("pdf", MediaType.APPLICATION_PDF_VALUE);

    private final String ext;
    private final String mediaType;

    ExportFormat(String ext, String mediaType) {
        this.ext = ext;
        this.mediaType = mediaType;
    }

    public String extension() {
        return ext;
    }

    public String mediaType() {
        return mediaType;
    }

    public String contentDispositionFilename(Article a) {
        // 优先用标题(用户看得懂),回退到 slug,再回退到 article-{id}
        String base = filesafe(a.getTitle());
        if (base.isEmpty()) base = (a.getSlug() == null || a.getSlug().isBlank())
                ? ("article-" + a.getId())
                : a.getSlug();
        return base + "." + ext;
    }

    /** 把任意字符串洗成可作文件名的部分:去 Win/Mac 非法字符和控符、去首尾空白/点,过长截断 */
    static String filesafe(String name) {
        if (name == null) return "";
        String s = name
                .replaceAll("[<>:\"/\\\\|?*\\x00-\\x1f]", "")
                .replaceAll("[\\s.]+$", "")
                .trim();
        if (s.isEmpty()) return "";
        if (s.length() > 100) s = s.substring(0, 100).trim();
        return s;
    }

    /** 解析外部传入的 format 字符串,空/null/未知 → 默认值 */
    public static ExportFormat parse(String s, ExportFormat def) {
        if (!StringUtils.hasText(s)) return def;
        for (ExportFormat f : values()) {
            if (f.ext.equalsIgnoreCase(s.trim())) return f;
        }
        return def;
    }
}
