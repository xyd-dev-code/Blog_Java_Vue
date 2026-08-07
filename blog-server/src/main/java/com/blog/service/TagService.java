package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.PageQuery;
import com.blog.dto.TagDTO;
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
        return page(q, null);
    }

    /**
     * 分页 + 模糊搜索(name/slug 命中任一)+ 批量统计 articleCount。
     */
    public Page<Tag> page(PageQuery q, String keyword) {
        LambdaQueryWrapper<Tag> w = new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(qq -> qq.like(Tag::getName, kw).or().like(Tag::getSlug, kw));
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Tag> p =
                tagMapper.selectPage(Page.of(q.getPage(), q.getSize()), w);
        if (p.getRecords() != null && !p.getRecords().isEmpty()) {
            // 批量统计本页标签的文章数
            List<Long> tagIds = p.getRecords().stream().map(Tag::getId).collect(Collectors.toList());
            Map<Long, Long> counts = articleTagMapper.selectList(
                    new LambdaQueryWrapper<com.blog.entity.ArticleTag>().in(com.blog.entity.ArticleTag::getTagId, tagIds)
            ).stream().collect(Collectors.groupingBy(at -> at.getTagId(), Collectors.counting()));
            for (Tag t : p.getRecords()) {
                t.setArticleCount(counts.getOrDefault(t.getId(), 0L));
            }
        }
        return p;
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

    @CacheEvict(value = "tags", allEntries = true)
    public Tag saveFromDTO(TagDTO dto) {
        Tag t = new Tag();
        t.setName(dto.getName());
        t.setSlug(dto.getSlug());
        return save(t);
    }

    @CacheEvict(value = "tags", allEntries = true)
    public Tag updateFromDTO(TagDTO dto) {
        Tag t = new Tag();
        t.setId(dto.getId());
        t.setName(dto.getName());
        t.setSlug(dto.getSlug());
        return update(t);
    }

    private String slug(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }
}
