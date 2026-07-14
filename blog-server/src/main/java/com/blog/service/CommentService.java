package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.CommentDTO;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    /** 留言板评论使用的特殊 articleId，与历史数据保持一致 */
    public static final long GUESTBOOK_ARTICLE_ID = 1L;

    public CommentService(CommentMapper commentMapper, ArticleMapper articleMapper, SiteConfigService siteConfigService) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
        this.siteConfigService = siteConfigService;
    }


    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final SiteConfigService siteConfigService;

    public Comment create(CommentDTO dto, String ip) {
        if (articleMapper.selectCount(new LambdaQueryWrapper<com.blog.entity.Article>()
                .eq(com.blog.entity.Article::getId, dto.getArticleId())) == 0) {
            throw new BizException("文章不存在");
        }
        String audit = siteConfigService.get("comment_audit", "0");
        Comment c = new Comment();
        c.setArticleId(dto.getArticleId());
        c.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        c.setNickname(sanitize(dto.getNickname()));
        c.setEmail(dto.getEmail());
        c.setWebsite(dto.getWebsite());
        c.setContent(clean(dto.getContent()));
        // 优先使用用户上传的头像，否则用 gravatar identicon
        if (dto.getAvatar() != null && !dto.getAvatar().trim().isEmpty()) {
            c.setAvatar(dto.getAvatar().trim());
        } else {
            c.setAvatar("https://www.gravatar.com/avatar/" + md5Like(dto.getEmail()) + "?d=identicon");
        }
        c.setIp(ip);
        c.setStatus("1".equals(audit) ? 0 : 1);
        c.setCreateTime(LocalDateTime.now());
        commentMapper.insert(c);
        return c;
    }

    public List<Comment> treeByArticle(Long articleId, boolean includePending) {
        LambdaQueryWrapper<Comment> w = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .orderByAsc(Comment::getCreateTime);
        if (!includePending) w.eq(Comment::getStatus, 1);
        List<Comment> all = commentMapper.selectList(w);
        return buildTree(all);
    }

    public List<Comment> guestbook() {
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, GUESTBOOK_ARTICLE_ID)
                .eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreateTime));
        return buildTree(all);
    }

    public List<Comment> treeAll() {
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .orderByDesc(Comment::getCreateTime).last("LIMIT 200"));
        Set<Long> ids = all.stream().map(Comment::getArticleId).collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            articleMapper.selectBatchIds(ids).forEach(a -> {
                for (Comment c : all) if (c.getArticleId().equals(a.getId())) c.setArticleTitle(a.getTitle());
            });
        }
        return all;
    }

    public long countPending() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 0));
    }

    public void approve(Long id) {
        Comment c = new Comment(); c.setId(id); c.setStatus(1); commentMapper.updateById(c);
    }

    public void markSpam(Long id) {
        Comment c = new Comment(); c.setId(id); c.setStatus(2); commentMapper.updateById(c);
    }

    public void delete(Long id) { commentMapper.deleteById(id); }

    public long totalApproved() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 1));
    }

    public long countApproved() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 1));
    }

    public long countSpam() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 2));
    }

    // ── 留言板专用统计 ──
    public long countPendingGuestbook() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, GUESTBOOK_ARTICLE_ID).eq(Comment::getStatus, 0));
    }
    public long countApprovedGuestbook() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, GUESTBOOK_ARTICLE_ID).eq(Comment::getStatus, 1));
    }
    public long countSpamGuestbook() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, GUESTBOOK_ARTICLE_ID).eq(Comment::getStatus, 2));
    }

    public Page<Comment> page(Page<Comment> p,
                          LambdaQueryWrapper<Comment> w) {
        return commentMapper.selectPage(p, w);
    }

    /** 给一批 Comment 填充 parentName（回复对象的昵称） */
    public void fillParentNames(List<Comment> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> parentIds = records.stream()
                .map(Comment::getParentId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (parentIds.isEmpty()) return;
        List<Comment> parents = commentMapper.selectBatchIds(parentIds);
        Map<Long, String> nameMap = new HashMap<>();
        for (Comment p : parents) nameMap.put(p.getId(), p.getNickname());
        for (Comment c : records) {
            if (c.getParentId() != null && c.getParentId() > 0) {
                c.setParentName(nameMap.get(c.getParentId()));
            }
        }
    }

    private List<Comment> buildTree(List<Comment> all) {
        Map<Long, Comment> map = all.stream().collect(Collectors.toMap(Comment::getId, c -> c));
        List<Comment> roots = new ArrayList<>();
        for (Comment c : all) {
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(c);
            } else {
                Comment parent = map.get(c.getParentId());
                if (parent != null) {
                    if (parent.getReplies() == null) parent.setReplies(new ArrayList<>());
                    c.setParentName(parent.getNickname());
                    parent.getReplies().add(c);
                }
            }
        }
        return roots;
    }

    private String clean(String html) {
        if (html == null) return null;
        return Jsoup.clean(html, Safelist.relaxed()
                .addTags("code", "pre")
                .addAttributes("a", "href", "title")
                .addProtocols("a", "href", "http", "https", "mailto"));
    }

    private String sanitize(String s) {
        if (s == null) return null;
        return Jsoup.clean(s, Safelist.none()).trim();
    }

    private String md5Like(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest((s == null ? "" : s.trim().toLowerCase()).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                int v = b & 0xff;
                sb.append(Character.forDigit(v >>> 4, 16));
                sb.append(Character.forDigit(v & 0x0f, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}