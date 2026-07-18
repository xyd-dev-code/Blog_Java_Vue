package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.ProjectDTO;
import com.blog.dto.ProjectQuery;
import com.blog.entity.Project;
import com.blog.mapper.ProjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectService {
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    public Page<Project> page(ProjectQuery q) {
        Page<Project> p = Page.of(q.getPage(), q.getSize());
        LambdaQueryWrapper<Project> w = new LambdaQueryWrapper<>();
        if (q.getStatus() != null) w.eq(Project::getStatus, q.getStatus());
        if (StringUtils.hasText(q.getKeyword())) {
            w.and(z -> z.like(Project::getName, q.getKeyword())
                    .or().like(Project::getDescription, q.getKeyword()));
        }
        w.orderByAsc(Project::getSortOrder).orderByDesc(Project::getId);
        Page<Project> res = projectMapper.selectPage(p, w);
        res.getRecords().forEach(this::decorate);
        return res;
    }

    public List<Project> listAll() {
        List<Project> list = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .orderByAsc(Project::getSortOrder).orderByDesc(Project::getId));
        list.forEach(this::decorate);
        return list;
    }

    public List<Project> listPublished() {
        List<Project> list = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getStatus, 1)
                .orderByAsc(Project::getSortOrder).orderByDesc(Project::getId));
        list.forEach(this::decorate);
        return list;
    }

    public Project byId(Long id) {
        Project p = projectMapper.selectById(id);
        if (p == null) throw new BizException(404, "项目不存在");
        decorate(p);
        return p;
    }

    public Project save(ProjectDTO dto) {
        Project p = new Project();
        BeanUtils.copyProperties(dto, p);
        if (p.getStatus() == null) p.setStatus(1);
        if (p.getSortOrder() == null || p.getSortOrder() == 0) p.setSortOrder(nextSortOrder());
        p.setTechStack(join(dto.getStack()));
        projectMapper.insert(p);
        return byId(p.getId());
    }

    private int nextSortOrder() {
        Integer max = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                        .select(Project::getSortOrder))
                .stream().map(Project::getSortOrder).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(-1);
        return max + 1;
    }

    public Project update(ProjectDTO dto) {
        if (dto.getId() == null) throw new BizException("id 必填");
        Project p = new Project();
        BeanUtils.copyProperties(dto, p);
        p.setTechStack(join(dto.getStack()));
        projectMapper.updateById(p);
        return byId(dto.getId());
    }

    public void delete(Long id) { projectMapper.deleteById(id); }

    public void batchDelete(List<Long> ids) { ids.forEach(projectMapper::deleteById); }

    public void updateStatus(Long id, Integer status) {
        Project p = new Project();
        p.setId(id);
        p.setStatus(status);
        projectMapper.updateById(p);
    }

    private void decorate(Project p) {
        if (StringUtils.hasText(p.getTechStack())) {
            p.setStack(Arrays.asList(p.getTechStack().split(",")));
        } else {
            p.setStack(List.of());
        }
    }

    private String join(List<String> stack) {
        if (stack == null || stack.isEmpty()) return null;
        return String.join(",", stack);
    }
}
