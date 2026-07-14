package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.PageQuery;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.TagMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagService {
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    public TagService(TagMapper tagMapper, ArticleTagMapper articleTagMapper) {
        this.tagMapper = tagMapper;
        this.articleTagMapper = articleTagMapper;
    }

    @Cacheable(value = "tags", key = "'all'")
    public List<Tag> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
    }

    public Page<Tag> page(PageQuery q) {
        return tagMapper.selectPage(Page.of(q.getPage(), q.getSize()),
                new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
    }

    public Tag byId(Long id) {
        return tagMapper.selectById(id);
    }

    public Tag bySlug(String slug) {
        return tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, slug));
    }

    public List<Map<String, Object>> cloud() {
        List<Tag> tags = tagMapper.selectList(null);
        if (tags.isEmpty()) return List.of();
        Map<Long, Long> counts = articleTagMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(at -> at.getTagId(), Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tag t : tags) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("slug", t.getSlug());
            m.put("count", counts.getOrDefault(t.getId(), 0L));
            result.add(m);
        }
        return result;
    }

    @CacheEvict(value = "tags", allEntries = true)
    public Tag save(Tag t) {
        if (!StringUtils.hasText(t.getSlug())) t.setSlug(slug(t.getName()));
        Long n = tagMapper.selectCount(new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, t.getSlug()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        tagMapper.insert(t);
        return t;
    }

    @CacheEvict(value = "tags", allEntries = true)
    public Tag update(Tag t) {
        Long n = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getSlug, t.getSlug()).ne(Tag::getId, t.getId()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        tagMapper.updateById(t);
        return t;
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public void delete(Long id) {
        articleTagMapper.deleteByTag(id);
        tagMapper.deleteById(id);
    }

    private String slug(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-");
    }
}
