package com.blog.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Project;
import java.util.List;

/** Public project fields, excluding notification, deletion and persistence metadata. */
public record ProjectPublicVO(
        Long id,
        String name,
        String description,
        String techStack,
        String icon,
        String color,
        String githubUrl,
        String demoUrl,
        String coverUrl,
        Long categoryId,
        Integer sortOrder,
        List<String> stack) {

    public static ProjectPublicVO from(Project project) {
        if (project == null) return null;
        return new ProjectPublicVO(
                project.getId(), project.getName(), project.getDescription(), project.getTechStack(),
                project.getIcon(), project.getColor(), project.getGithubUrl(), project.getDemoUrl(),
                project.getCoverUrl(), project.getCategoryId(), project.getSortOrder(), project.getStack());
    }

    public static Page<ProjectPublicVO> page(Page<Project> source) {
        Page<ProjectPublicVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(ProjectPublicVO::from).toList());
        return result;
    }
}
