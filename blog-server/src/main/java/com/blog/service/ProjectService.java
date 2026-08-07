package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.ProjectDTO;
import com.blog.dto.ProjectQuery;
import com.blog.entity.Project;
import com.blog.mapper.ProjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
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
        if (q.getCategoryId() != null) w.eq(Project::getCategoryId, q.getCategoryId());
        w.orderByAsc(Project::getSortOrder).orderByDesc(Project::getId);
        Page<Project> res = projectMapper.selectPage(p, w);
        res.getRecords().forEach(this::decorate);
        return res;
    }

    /** 前台已发布项目分页（支持分类筛选） */
    public Page<Project> publishedPage(ProjectQuery q) {
        Page<Project> p = Page.of(q.getPage(), q.getSize());
        LambdaQueryWrapper<Project> w = new LambdaQueryWrapper<>();
        w.eq(Project::getStatus, 1);
        if (q.getCategoryId() != null) w.eq(Project::getCategoryId, q.getCategoryId());
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

    // 项目归属变化会影响前台分类的项目计数，这里统一让分类缓存失效
    @CacheEvict(value = "projectCategories", allEntries = true)
    public Project save(ProjectDTO dto) {
        Project p = new Project();
        BeanUtils.copyProperties(dto, p);
        if (p.getStatus() == null) p.setStatus(1);
        if (p.getSortOrder() == null || p.getSortOrder() == 0) p.setSortOrder(nextSortOrder());
        pushDownSortOrder(null, p.getSortOrder());
        renumberSortOrder();
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

    /** 位次冲突下推：sortOrder>=target 的其他记录降序 +1 */
    private void pushDownSortOrder(Long excludeId, int target) {
        List<Project> conflicts = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .ne(excludeId != null, Project::getId, excludeId)
                .ge(Project::getSortOrder, target)
                .orderByDesc(Project::getSortOrder));
        for (Project c : conflicts) {
            projectMapper.update(null, new LambdaUpdateWrapper<Project>()
                    .eq(Project::getId, c.getId())
                    .set(Project::getSortOrder, c.getSortOrder() + 1));
        }
    }

    /** 归一化位次：按当前排序重排为 1,2,3...（消除 pushDown 留下的空位） */
    private void renumberSortOrder() {
        List<Project> all = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .orderByAsc(Project::getSortOrder)
                .orderByDesc(Project::getId));
        for (int i = 0; i < all.size(); i++) {
            int target = i + 1;
            Project c = all.get(i);
            if (c.getSortOrder() == null || c.getSortOrder() != target) {
                projectMapper.update(null, new LambdaUpdateWrapper<Project>()
                        .eq(Project::getId, c.getId())
                        .set(Project::getSortOrder, target));
            }
        }
    }

    @CacheEvict(value = "projectCategories", allEntries = true)
    public Project update(ProjectDTO dto) {
        if (dto.getId() == null) throw new BizException("id 必填");
        Project p = new Project();
        BeanUtils.copyProperties(dto, p);
        // 位次变化 → 下推冲突
        if (dto.getSortOrder() != null) {
            Project old = projectMapper.selectById(dto.getId());
            if (old != null && !dto.getSortOrder().equals(old.getSortOrder())) {
                pushDownSortOrder(dto.getId(), dto.getSortOrder());
                renumberSortOrder();
            }
        }
        p.setTechStack(join(dto.getStack()));
        projectMapper.updateById(p);
        if (dto.getCategoryId() == null) {
            projectMapper.update(null, new LambdaUpdateWrapper<Project>()
                    .eq(Project::getId, dto.getId())
                    .set(Project::getCategoryId, null));
        }
        return byId(dto.getId());
    }

    @CacheEvict(value = "projectCategories", allEntries = true)
    public void delete(Long id) { projectMapper.deleteById(id); }

    @CacheEvict(value = "projectCategories", allEntries = true)
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

    /** tech_stack 列为 VARCHAR(500)，这里提前拦截，避免落库时抛 "Data too long" 变成 500 */
    private static final int TECH_STACK_MAX = 500;

    private String join(List<String> stack) {
        // 返回空串而非 null，保证"清空技术栈"能真正落库
        if (stack == null || stack.isEmpty()) return "";
        String joined = String.join(",", stack);
        if (joined.length() > TECH_STACK_MAX) {
            throw new BizException("技术栈总长度不能超过 " + TECH_STACK_MAX + " 字符");
        }
        return joined;
    }
}
