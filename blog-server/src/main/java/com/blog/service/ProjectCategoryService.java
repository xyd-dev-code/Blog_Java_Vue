package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.ProjectCategoryDTO;
import com.blog.entity.Project;
import com.blog.entity.ProjectCategory;
import com.blog.mapper.ProjectCategoryMapper;
import com.blog.mapper.ProjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectCategoryService {
    private final ProjectCategoryMapper categoryMapper;
    private final ProjectMapper projectMapper;

    public ProjectCategoryService(ProjectCategoryMapper categoryMapper, ProjectMapper projectMapper) {
        this.categoryMapper = categoryMapper;
        this.projectMapper = projectMapper;
    }

    @Cacheable(value = "projectCategories", key = "'all'")
    public List<ProjectCategory> listAll() {
        List<ProjectCategory> list = categoryMapper.selectList(new LambdaQueryWrapper<ProjectCategory>()
                .eq(ProjectCategory::getStatus, 1)
                .orderByAsc(ProjectCategory::getSortOrder)
                .orderByAsc(ProjectCategory::getId));
        fillProjectCount(list, true);
        return list;
    }

    /** 后台下拉用：含下架分类 */
    public List<ProjectCategory> listAllIncludingUnpublished() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ProjectCategory>()
                .orderByAsc(ProjectCategory::getSortOrder)
                .orderByAsc(ProjectCategory::getId));
    }

    public Page<ProjectCategory> page(long p, long size, String keyword) {
        LambdaQueryWrapper<ProjectCategory> w = new LambdaQueryWrapper<ProjectCategory>()
                .orderByAsc(ProjectCategory::getSortOrder)
                .orderByAsc(ProjectCategory::getId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(qq -> qq.like(ProjectCategory::getName, kw).or().like(ProjectCategory::getSlug, kw));
        }
        Page<ProjectCategory> pg = categoryMapper.selectPage(Page.of(p, size), w);
        fillProjectCount(pg.getRecords(), false);
        return pg;
    }

    /**
     * 批量统计各分类下的项目数。
     * @param publishedOnly true 只统计已发布项目(前台用)，false 统计全部(后台用)
     */
    private void fillProjectCount(List<ProjectCategory> list, boolean publishedOnly) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(ProjectCategory::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return;
        // deleted 由 @TableLogic 自动追加，无需手动拼条件
        QueryWrapper<Project> w = new QueryWrapper<Project>()
                .select("category_id AS cid, COUNT(*) AS cnt")
                .in("category_id", ids);
        if (publishedOnly) w.eq("status", 1);
        List<Map<String, Object>> rows = projectMapper.selectMaps(w.groupBy("category_id"));
        Map<Long, Long> counts = rows.stream()
                .filter(r -> r.get("cid") != null)
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("cid")).longValue(),
                        r -> ((Number) r.get("cnt")).longValue()
                ));
        list.forEach(c -> c.setProjectCount(counts.getOrDefault(c.getId(), 0L)));
    }

    public ProjectCategory byId(Long id) {
        ProjectCategory c = categoryMapper.selectById(id);
        if (c == null) throw new BizException(404, "项目分类不存在");
        return c;
    }

    public ProjectCategory bySlug(String slug) {
        ProjectCategory c = categoryMapper.selectOne(new LambdaQueryWrapper<ProjectCategory>().eq(ProjectCategory::getSlug, slug));
        if (c == null) throw new BizException(404, "项目分类不存在");
        return c;
    }

    @CacheEvict(value = "projectCategories", allEntries = true)
    public ProjectCategory save(ProjectCategory c) {
        c.setSlug(normalizeSlug(c.getSlug(), c.getName()));
        if (existsSlug(c.getSlug(), null)) throw new BizException("slug 已存在");
        if (c.getSortOrder() == null || c.getSortOrder() == 0) c.setSortOrder(nextSortOrder());
        if (c.getStatus() == null) c.setStatus(1);
        pushDownSortOrder(null, c.getSortOrder());
        renumberSortOrder();
        categoryMapper.insert(c);
        return c;
    }

    /** slug 为空则由名称派生；派生结果仍为空(如名称全是符号)时兜底一个唯一值，避免空串撞 UNIQUE 约束 */
    private String normalizeSlug(String slug, String name) {
        String s = StringUtils.hasText(slug) ? slug.trim() : slug(name);
        if (!StringUtils.hasText(s)) s = "cat-" + System.currentTimeMillis();
        return s;
    }

    private boolean existsSlug(String slug, Long id) {
        LambdaQueryWrapper<ProjectCategory> w = new LambdaQueryWrapper<ProjectCategory>().eq(ProjectCategory::getSlug, slug);
        if (id != null) w.ne(ProjectCategory::getId, id);
        return categoryMapper.selectCount(w) > 0;
    }

    private int nextSortOrder() {
        Integer max = categoryMapper.selectList(new LambdaQueryWrapper<ProjectCategory>()
                        .select(ProjectCategory::getSortOrder))
                .stream().map(ProjectCategory::getSortOrder).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(-1);
        return max + 1;
    }

    /** 位次冲突下推：sortOrder>=target 的其他记录降序 +1 */
    private void pushDownSortOrder(Long excludeId, int target) {
        LambdaQueryWrapper<ProjectCategory> w = new LambdaQueryWrapper<ProjectCategory>()
                .ne(excludeId != null, ProjectCategory::getId, excludeId)
                .ge(ProjectCategory::getSortOrder, target)
                .orderByDesc(ProjectCategory::getSortOrder);
        List<ProjectCategory> conflicts = categoryMapper.selectList(w);
        for (ProjectCategory c : conflicts) {
            c.setSortOrder(c.getSortOrder() + 1);
            categoryMapper.updateById(c);
        }
    }

    /** 归一化位次：按当前排序重排为 1,2,3...（消除 pushDown 留下的空位） */
    private void renumberSortOrder() {
        List<ProjectCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<ProjectCategory>()
                .orderByAsc(ProjectCategory::getSortOrder)
                .orderByAsc(ProjectCategory::getId));
        for (int i = 0; i < all.size(); i++) {
            int target = i + 1;
            ProjectCategory c = all.get(i);
            if (c.getSortOrder() == null || c.getSortOrder() != target) {
                c.setSortOrder(target);
                categoryMapper.updateById(c);
            }
        }
    }

    @CacheEvict(value = "projectCategories", allEntries = true)
    public ProjectCategory update(ProjectCategory c) {
        if (c.getId() == null) throw new BizException("id 必填");
        ProjectCategory exist = byId(c.getId());
        // slug 留空时沿用旧值
        String slug = StringUtils.hasText(c.getSlug())
                ? c.getSlug().trim()
                : (StringUtils.hasText(c.getName()) ? normalizeSlug(null, c.getName()) : exist.getSlug());
        c.setSlug(slug);
        if (existsSlug(slug, c.getId())) throw new BizException("slug 已存在");
        // 位次变化 → 下推冲突
        if (c.getSortOrder() != null && !c.getSortOrder().equals(exist.getSortOrder())) {
            pushDownSortOrder(c.getId(), c.getSortOrder());
            renumberSortOrder();
        }
        categoryMapper.updateById(c);
        return byId(c.getId());
    }

    /** 单独切换发布状态，不触碰其它字段 */
    @CacheEvict(value = "projectCategories", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        byId(id);
        ProjectCategory c = new ProjectCategory();
        c.setId(id);
        c.setStatus(status);
        categoryMapper.updateById(c);
    }

    @CacheEvict(value = "projectCategories", allEntries = true)
    public void delete(Long id) {
        ProjectCategory exist = byId(id);
        // 先解绑该分类下的项目，避免留下指向已删分类的悬空 category_id
        projectMapper.update(null, new LambdaUpdateWrapper<Project>()
                .eq(Project::getCategoryId, id)
                .set(Project::getCategoryId, null));
        // slug 列有 UNIQUE 约束，逻辑删除后仍会占位；这里改名释放，便于日后用同名重建
        String old = exist.getSlug() == null ? "" : exist.getSlug();
        String freed = (old.length() > 50 ? old.substring(0, 50) : old) + "-del-" + id;
        categoryMapper.update(null, new LambdaUpdateWrapper<ProjectCategory>()
                .eq(ProjectCategory::getId, id)
                .set(ProjectCategory::getSlug, freed));
        categoryMapper.deleteById(id);
    }

    @CacheEvict(value = "projectCategories", allEntries = true)
    public ProjectCategory saveFromDTO(ProjectCategoryDTO dto) {
        ProjectCategory c = new ProjectCategory();
        c.setName(dto.getName());
        c.setSlug(dto.getSlug());
        c.setColor(dto.getColor());
        c.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) c.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) c.setStatus(dto.getStatus());
        return save(c);
    }

    @CacheEvict(value = "projectCategories", allEntries = true)
    public ProjectCategory updateFromDTO(ProjectCategoryDTO dto) {
        ProjectCategory c = new ProjectCategory();
        c.setId(dto.getId());
        c.setName(dto.getName());
        c.setSlug(dto.getSlug());
        c.setColor(dto.getColor());
        c.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) c.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) c.setStatus(dto.getStatus());
        return update(c);
    }

    private String slug(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }
}
