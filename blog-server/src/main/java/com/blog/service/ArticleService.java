package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQuery;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.TagMapper;
import com.blog.mapper.TagRelationMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.CommentMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    public ArticleService(ArticleMapper articleMapper, ArticleTagMapper articleTagMapper, CategoryMapper categoryMapper, TagMapper tagMapper, TagRelationMapper tagRelationMapper, UserMapper userMapper, CommentMapper commentMapper, jakarta.validation.Validator validator) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.tagRelationMapper = tagRelationMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.validator = validator;
    }


    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final TagRelationMapper tagRelationMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final jakarta.validation.Validator validator;

    public Page<Article> homePage(long page, long size) {
        Page<Article> p = Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        p = articleMapper.selectPage(p, ArticleVisibility.summaries()
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreateTime));
        // 注入作者信息
        User admin = userMapper.selectById(1L);
        for (Article a : p.getRecords()) setAuthor(a, admin);
        injectCommentCount(p.getRecords());
        return p;
    }

    public Page<Article> pageAdmin(ArticleQuery q) {
        // cap size 防单请求拉全表(size=1_000_000 等)
        long size = Math.max(1, Math.min(q.getSize(), 100));
        long page = Math.max(1, q.getPage());
        Page<Article> p = Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<>();
        if (q.getStatus() != null) w.eq(Article::getStatus, q.getStatus());
        if (q.getCategoryId() != null) w.eq(Article::getCategoryId, q.getCategoryId());
        if (StringUtils.hasText(q.getKeyword())) {
            // keyword 长度上限,防止 LIKE 性能炸弹;且需要 final 变量给 lambda 用
            final String kw;
            String raw = q.getKeyword().trim();
            if (raw.length() > 50) raw = raw.substring(0, 50);
            kw = raw;
            w.and(z -> z.like(Article::getTitle, kw)
                        .or().like(Article::getSummary, kw));
        }
        w.orderByDesc(Article::getIsTop).orderByDesc(Article::getCreateTime);
        return articleMapper.selectPage(p, w);
    }

    public List<Article> listFeatured(int limit) {
        // 防御性 cap,public front-controller 直接传 limit 给前端
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Article> list = articleMapper.selectList(ArticleVisibility.summaries().eq(Article::getIsFeatured, 1)
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + safeLimit));
        User admin = userMapper.selectById(1L);
        for (Article a : list) setAuthor(a, admin);
        injectCommentCount(list);
        return list;
    }

    public List<Article> listByCategory(Long categoryId) {
        List<Article> list = articleMapper.selectList(ArticleVisibility.summaries().eq(Article::getCategoryId, categoryId)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getPublishTime).last("LIMIT 100"));
        User admin = userMapper.selectById(1L);
        for (Article a : list) setAuthor(a, admin);
        injectCommentCount(list);
        return list;
    }

    public List<Article> listByTag(Long tagId) {
        List<Long> ids = tagRelationMapper.findArticleIdsByTag(tagId);
        if (ids.isEmpty()) return List.of();
        return articleMapper.selectList(ArticleVisibility.summaries().in(Article::getId, ids)
                .orderByDesc(Article::getPublishTime).last("LIMIT 100"));
    }

    public Article detailBySlug(String slug) {
        Article a = articleMapper.selectOne(ArticleVisibility.query().eq(Article::getSlug, slug));
        if (a == null) throw new BizException(404, "文章不存在");
        decorate(a);
        return a;
    }

    public Article detailById(Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null) throw new BizException(404, "文章不存在");
        decorate(a);
        return a;
    }

    public void incrView(Long id) { articleMapper.incrView(id); }

    public Article prevNext(Long id, boolean prev) {
        Article cur = articleMapper.selectById(id);
        if (!ArticleVisibility.isPublic(cur) || cur.getPublishTime() == null) return null;
        LambdaQueryWrapper<Article> w = ArticleVisibility.summaries();
        if (prev) {
            w.lt(Article::getPublishTime, cur.getPublishTime())
             .orderByDesc(Article::getPublishTime).last("LIMIT 1");
        } else {
            w.gt(Article::getPublishTime, cur.getPublishTime())
             .orderByAsc(Article::getPublishTime).last("LIMIT 1");
        }
        return articleMapper.selectOne(w);
    }

    public List<Article> related(Long articleId) {
        Article a = articleMapper.selectById(articleId);
        if (!ArticleVisibility.isPublic(a) || a.getCategoryId() == null) return List.of();
        return articleMapper.selectList(ArticleVisibility.summaries().eq(Article::getCategoryId, a.getCategoryId())
                .ne(Article::getId, articleId)
                .orderByDesc(Article::getPublishTime).last("LIMIT 6"));
    }

    public List<Article> search(String kw, int limit) {
        if (!StringUtils.hasText(kw)) return List.of();
        String safeKw = kw.trim();
        if (safeKw.length() < 2 || safeKw.length() > 50) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        final String likeKw = safeKw;
        List<Article> list = articleMapper.selectList(ArticleVisibility.summaries()
                .and(z -> z.like(Article::getTitle, likeKw).or().like(Article::getSummary, likeKw))
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + safeLimit));
        User admin = userMapper.selectById(1L);
        for (Article a : list) setAuthor(a, admin);
        injectCommentCount(list);
        return list;
    }

    public List<Map<String, Object>> archive() {
        return articleMapper.archiveStats();
    }

    public List<Article> listByMonth(String ym) {
        List<Article> list = articleMapper.selectList(ArticleVisibility.summaries()
                .ge(Article::getPublishTime, monthStart(ym))
                .lt(Article::getPublishTime, monthStart(ym).plusMonths(1))
                .orderByDesc(Article::getPublishTime).last("LIMIT 100"));
        User admin = userMapper.selectById(1L);
        for (Article a : list) setAuthor(a, admin);
        injectCommentCount(list);
        return list;
    }

    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public Article save(ArticleDTO dto) {
        validate(dto);
        if (!StringUtils.hasText(dto.getSlug())) dto.setSlug(toSlug(dto.getTitle()));
        Long n = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, dto.getSlug()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        Article a = new Article();
        BeanUtils.copyProperties(dto, a, "id");
        if (a.getStatus() == null) a.setStatus(1);
        if (a.getStatus() == 1 && a.getPublishTime() == null) a.setPublishTime(LocalDateTime.now());
        if (!StringUtils.hasText(a.getSummary())) a.setSummary(extractSummary(a.getContent()));
        articleMapper.insert(a);
        syncTags(a.getId(), dto.getTagIds());
        return detailById(a.getId());
    }

    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public Article update(ArticleDTO dto) {
        if (dto.getId() == null) throw new BizException("id 必填");
        Article previous = articleMapper.selectById(dto.getId());
        if (previous == null) throw new BizException(404, "文章不存在");
        if (!StringUtils.hasText(dto.getSlug())) dto.setSlug(previous.getSlug());
        validate(dto);
        Long n = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, dto.getSlug()).ne(Article::getId, dto.getId()));
        if (n != null && n > 0) throw new BizException("slug 已存在");
        Article a = new Article();
        BeanUtils.copyProperties(dto, a);
        if (a.getStatus() == null) a.setStatus(previous.getStatus());
        if (a.getPublishTime() == null) a.setPublishTime(previous.getPublishTime());
        if (Integer.valueOf(1).equals(a.getStatus()) && a.getPublishTime() == null) a.setPublishTime(LocalDateTime.now());
        articleMapper.updateById(a);
        syncTags(dto.getId(), dto.getTagIds());
        return detailById(dto.getId());
    }

    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public void delete(Long id) {
        articleMapper.deleteById(id);
        articleTagMapper.deleteByArticle(id);
    }

    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public void batchDelete(List<Long> ids) {
        ids.forEach(id -> { articleMapper.deleteById(id); articleTagMapper.deleteByArticle(id); });
    }

    @CacheEvict(value = "articles", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        Article a = new Article();
        a.setId(id);
        a.setStatus(status);
        if (status == null || status < 0 || status > 2) throw new BizException("文章状态无效");
        Article previous = articleMapper.selectById(id);
        if (previous == null) throw new BizException(404, "文章不存在");
        if (status == 1 && previous.getPublishTime() == null) a.setPublishTime(LocalDateTime.now());
        articleMapper.updateById(a);
    }

    @CacheEvict(value = "articles", allEntries = true)
    public void updateTop(Long id, Integer top) {
        Article a = new Article();
        a.setId(id);
        a.setIsTop(top);
        articleMapper.updateById(a);
    }

    @CacheEvict(value = "articles", allEntries = true)
    public void updateFeatured(Long id, Integer featured) {
        Article a = new Article();
        a.setId(id);
        a.setIsFeatured(featured);
        articleMapper.updateById(a);
    }

    @CacheEvict(value = "articles", allEntries = true)
    public void setViewCount(Long id, long value) {
        if (value < 0 || value > 10_000_000L) throw new BizException("阅读量必须在 0 ~ 10,000,000 之间");
        Article cur = articleMapper.selectById(id);
        if (cur == null) throw new BizException(404, "文章不存在");
        int n = articleMapper.setViewCount(id, value);
        if (n == 0) throw new BizException(404, "文章不存在");
    }

    @CacheEvict(value = "articles", allEntries = true)
    public void incrViewBy(Long id, long delta) {
        if (delta < -10_000L || delta > 10_000L) {
            throw new BizException("单次调整幅度必须在 -10000 ~ 10000 之间");
        }
        Article cur = articleMapper.selectById(id);
        if (cur == null) throw new BizException(404, "文章不存在");
        long curVal = cur.getViewCount() == null ? 0L : cur.getViewCount();
        long next = curVal + delta;
        if (next < 0 || next > 10_000_000L) throw new BizException("调整后阅读量超出允许范围");
        int n = articleMapper.incrByView(id, delta);
        if (n == 0) throw new BizException(404, "文章不存在");
    }

    private void syncTags(Long articleId, List<Long> tagIds) {
        articleTagMapper.deleteByArticle(articleId);
        if (tagIds == null || tagIds.isEmpty()) return;
        for (Long tid : new java.util.LinkedHashSet<>(tagIds)) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tid);
            articleTagMapper.insert(at);
        }
    }

    private String extractSummary(String content) {
        if (content == null) return null;
        String plain = content.replaceAll("[\\s#>*`\\-_\\[\\]\\(\\)]+", " ").trim();
        return plain.length() > 180 ? plain.substring(0, 180) + "…" : plain;
    }

    private String toSlug(String s) {
        if (s == null) return "";
        String slug = s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        long ts = System.currentTimeMillis();
        return (slug.length() > 50 ? slug.substring(0, 50) : slug) + "-" + ts;
    }

    public List<Article> listLatest(int limit) {
        List<Article> list = articleMapper.selectList(ArticleVisibility.summaries()
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
        User admin = userMapper.selectById(1L);
        for (Article a : list) setAuthor(a, admin);
        injectCommentCount(list);
        return list;
    }

    public long countPublished() {
        return articleMapper.selectCount(ArticleVisibility.query());
    }

    public long sumViews() {
        Long sum = articleMapper.sumViews();
        return sum == null ? 0 : sum;
    }

    public Page<Article> pageByCategorySlug(String slug, long page, long size) {
        Category c = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getSlug, slug));
        if (c == null) return Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        return articleMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100))),
                ArticleVisibility.summaries()
                        .eq(Article::getCategoryId, c.getId())
                        .orderByDesc(Article::getPublishTime));
    }

    public Page<Article> pageByTagSlug(String slug, long page, long size) {
        Tag t = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, slug));
        if (t == null) return Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        List<Long> articleIds = tagRelationMapper.findArticleIdsByTag(t.getId());
        if (articleIds.isEmpty()) return Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        return articleMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100))),
                ArticleVisibility.summaries()
                        .in(Article::getId, articleIds)
                        .orderByDesc(Article::getPublishTime));
    }

    public void validate(ArticleDTO dto) {
        if (dto == null) throw new BizException("文章参数不能为空");
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) throw new BizException(violations.iterator().next().getMessage());
        if (dto.getCategoryId() != null && categoryMapper.selectById(dto.getCategoryId()) == null)
            throw new BizException("分类不存在");
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            var ids = new java.util.HashSet<>(dto.getTagIds());
            if (tagMapper.selectCount(new LambdaQueryWrapper<Tag>().in(Tag::getId, ids)) != ids.size())
                throw new BizException("标签不存在");
        }
    }

    private LocalDateTime monthStart(String month) {
        try { return java.time.YearMonth.parse(month).atDay(1).atStartOfDay(); }
        catch (java.time.DateTimeException e) { throw new BizException("月份格式无效"); }
    }

    public Page<Article> archivePage(long page, long size) {
        return articleMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100))),
                ArticleVisibility.summaries().orderByDesc(Article::getPublishTime).orderByDesc(Article::getId));
    }

    private void decorate(Article a) {
        if (a.getCategoryId() != null) {
            Category c = categoryMapper.selectById(a.getCategoryId());
            if (c != null) { a.setCategoryName(c.getName()); a.setCategorySlug(c.getSlug()); }
        }
        List<ArticleTag> ats = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getArticleId, a.getId()));
        if (!ats.isEmpty()) {
            List<Long> ids = ats.stream().map(ArticleTag::getTagId).toList();
            a.setTags(tagMapper.selectBatchIds(ids));
        } else {
            a.setTags(List.of());
        }
        User admin = userMapper.selectById(1L);
        setAuthor(a, admin);
    }

    private void setAuthor(Article a, User admin) {
        if (admin != null) {
            if (admin.getNickname() != null && !admin.getNickname().isEmpty()) {
                a.setAuthor(admin.getNickname());
            }
            if (admin.getAvatar() != null && !admin.getAvatar().isEmpty()) {
                a.setAuthorAvatar(admin.getAvatar());
            }
        }
    }

    /** 批量注入每篇文章的已审核评论数（status=1），避免 N+1 查询 */
    private void injectCommentCount(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;
        List<Long> ids = articles.stream().map(Article::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = commentMapper.countApprovedByArticles(ids).stream()
                .collect(Collectors.toMap(com.blog.vo.ArticleCommentCount::getArticleId,
                        com.blog.vo.ArticleCommentCount::getCount));
        for (Article a : articles) {
            a.setCommentCount(countMap.getOrDefault(a.getId(), 0L));
        }
    }
}