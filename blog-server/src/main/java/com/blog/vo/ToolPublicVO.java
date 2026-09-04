package com.blog.vo;

import com.blog.entity.Tool;
import java.util.List;

/** Public tool fields, excluding notification, deletion and persistence metadata. */
public record ToolPublicVO(
        Long id,
        String name,
        String slug,
        String icon,
        String category,
        String description,
        String url,
        Integer type,
        Integer status,
        Integer sortOrder,
        Long viewCount,
        Long clickCount,
        String announcement) {

    public static ToolPublicVO from(Tool tool) {
        if (tool == null) return null;
        return new ToolPublicVO(
                tool.getId(), tool.getName(), tool.getSlug(), tool.getIcon(), tool.getCategory(),
                tool.getDescription(), tool.getUrl(), tool.getType(), tool.getStatus(), tool.getSortOrder(),
                tool.getViewCount(), tool.getClickCount(), tool.getAnnouncement());
    }

    public static List<ToolPublicVO> list(List<Tool> tools) {
        return tools == null ? List.of() : tools.stream().map(ToolPublicVO::from).toList();
    }
}
