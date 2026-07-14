package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.PageQuery;
import com.blog.entity.Category;
import com.blog.mapper.CategoryMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Cacheable(value = "categories", key = "'all'")
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
    }

    public Page<Category> page(PageQuery q) {
        return categoryMapper.selectPage(Page.of(q.getPage(), q.getSize()),
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId));
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
        categoryMapper.insert(c);
        return c;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category update(Category c) {
        Long n = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getSlug, c.getSlug()).ne(Category::getId, c.getId()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        categoryMapper.updateById(c);
        return c;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) { categoryMapper.deleteById(id); }

    private String slug(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-");
    }
}
