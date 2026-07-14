package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.entity.Page;
import com.blog.mapper.PageMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PageService {
    private final PageMapper pageMapper;

    public PageService(PageMapper pageMapper) {
        this.pageMapper = pageMapper;
    }

    @Cacheable(value = "pages", key = "'all'")
    public List<Page> listAll() {
        return pageMapper.selectList(new LambdaQueryWrapper<Page>()
                .eq(Page::getStatus, 1)
                .orderByAsc(Page::getSortOrder)
                .orderByAsc(Page::getId));
    }

    @Cacheable(value = "pages", key = "'slug:' + #slug")
    public Page bySlug(String slug) {
        Page p = pageMapper.selectOne(new LambdaQueryWrapper<Page>()
                .eq(Page::getSlug, slug).eq(Page::getStatus, 1));
        if (p == null) throw new BizException(404, "页面不存在");
        return p;
    }

    public Page byId(Long id) {
        return pageMapper.selectById(id);
    }

    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<Page> page(long p, long size) {
        return pageMapper.selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(p, size),
                new LambdaQueryWrapper<Page>().orderByAsc(Page::getSortOrder));
    }

    @CacheEvict(value = "pages", allEntries = true)
    public Page save(Page p) {
        if (!StringUtils.hasText(p.getSlug())) p.setSlug(slug(p.getTitle()));
        Long n = pageMapper.selectCount(new LambdaQueryWrapper<Page>().eq(Page::getSlug, p.getSlug()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        pageMapper.insert(p);
        return p;
    }

    @CacheEvict(value = "pages", allEntries = true)
    public Page update(Page p) {
        Long n = pageMapper.selectCount(new LambdaQueryWrapper<Page>()
                .eq(Page::getSlug, p.getSlug()).ne(Page::getId, p.getId()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        pageMapper.updateById(p);
        return p;
    }

    @CacheEvict(value = "pages", allEntries = true)
    public void delete(Long id) { pageMapper.deleteById(id); }

    private String slug(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-");
    }
}
