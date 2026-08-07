package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.CategoryDTO;
import com.blog.dto.PageQuery;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    public CategoryService(CategoryMapper categoryMapper, ArticleMapper articleMapper) {
        this.categoryMapper = categoryMapper;
        this.articleMapper = articleMapper;
    }

    @Cacheable(value = "categories", key = "'all'")
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
    }

    public Page<Category> page(PageQuery q) {
        return page(q.getPage(), q.getSize(), null);
    }

    /**
     * 分页 + 模糊搜索(name/slug 命中任一)+ 批量统计 articleCount。
     */
    public Page<Category> page(long p, long size, String keyword) {
        LambdaQueryWrapper<Category> w = new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(qq -> qq.like(Category::getName, kw).or().like(Category::getSlug, kw));
        }
        Page<Category> pg = categoryMapper.selectPage(Page.of(p, size), w);
        // 一次性 COUNT GROUP BY categoryId 取本页所有分类的文章数
        if (pg.getRecords() != null && !pg.getRecords().isEmpty()) {
            List<Long> ids = pg.getRecords().stream().map(Category::getId).collect(Collectors.toList());
            List<Map<String, Object>> rows = articleMapper.selectMaps(
                    new QueryWrapper<Article>()
                            .select("category_id AS cid, COUNT(*) AS cnt")
                            .in("category_id", ids)
                            .eq("deleted", 0)
                            .groupBy("category_id")
            );
            Map<Long, Long> counts = rows.stream().collect(Collectors.toMap(
                    r -> ((Number) r.get("cid")).longValue(),
                    r -> ((Number) r.get("cnt")).longValue()
            ));
            pg.getRecords().forEach(c -> c.setArticleCount(counts.getOrDefault(c.getId(), 0L)));
        }
        return pg;
    }

    public Category byId(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BizException(404, "分类不存在");
        return c;
    }

    public Category bySlug(String slug) {
        Category c = categoryMapper.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getSlug, slug));
        if (c == null) throw new BizException(404, "分类不存在");
        return c;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category save(Category c) {
        if (!StringUtils.hasText(c.getSlug())) c.setSlug(slug(c.getName()));
        Long n = categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getSlug, c.getSlug()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        if (c.getSortOrder() == null || c.getSortOrder() == 0) c.setSortOrder(nextSortOrder());
        pushDownSortOrder(null, c.getSortOrder());
        renumberSortOrder();
        categoryMapper.insert(c);
        return c;
    }

    private int nextSortOrder() {
        Integer max = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .select(Category::getSortOrder))
                .stream().map(Category::getSortOrder).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(-1);
        return max + 1;
    }

    /** 位次冲突下推：sortOrder>=target 的其他记录降序 +1 */
    private void pushDownSortOrder(Long excludeId, int target) {
        LambdaQueryWrapper<Category> w = new LambdaQueryWrapper<Category>()
                .ne(excludeId != null, Category::getId, excludeId)
                .ge(Category::getSortOrder, target)
                .orderByDesc(Category::getSortOrder);
        List<Category> conflicts = categoryMapper.selectList(w);
        for (Category c : conflicts) {
            c.setSortOrder(c.getSortOrder() + 1);
            categoryMapper.updateById(c);
        }
    }

    /** 归一化位次：按当前排序重排为 1,2,3...（消除 pushDown 留下的空位） */
    private void renumberSortOrder() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        for (int i = 0; i < all.size(); i++) {
            int target = i + 1;
            Category c = all.get(i);
            if (c.getSortOrder() == null || c.getSortOrder() != target) {
                c.setSortOrder(target);
                categoryMapper.updateById(c);
            }
        }
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category update(Category c) {
        Long n = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getSlug, c.getSlug()).ne(Category::getId, c.getId()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        if (c.getSortOrder() != null) {
            Category old = categoryMapper.selectById(c.getId());
            if (old != null && !c.getSortOrder().equals(old.getSortOrder())) {
                pushDownSortOrder(c.getId(), c.getSortOrder());
                renumberSortOrder();
            }
        }
        categoryMapper.updateById(c);
        return c;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) { categoryMapper.deleteById(id); }

    @CacheEvict(value = "categories", allEntries = true)
    public Category saveFromDTO(CategoryDTO dto) {
        Category c = new Category();
        c.setName(dto.getName());
        c.setSlug(dto.getSlug());
        c.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) c.setSortOrder(dto.getSortOrder());
        return save(c);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category updateFromDTO(CategoryDTO dto) {
        Category c = new Category();
        c.setId(dto.getId());
        c.setName(dto.getName());
        c.setSlug(dto.getSlug());
        c.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) c.setSortOrder(dto.getSortOrder());
        return update(c);
    }

    private String slug(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }
}
