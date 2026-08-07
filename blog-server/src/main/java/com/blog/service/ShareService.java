package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.entity.Article;
import com.blog.entity.ShareLog;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ShareLogMapper;
import com.blog.security.ClientIpResolver;
import com.blog.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
public class ShareService {

    public static final Set<String> ALLOWED_CHANNELS =
            Set.of("wechat", "weibo", "qq", "douban", "copy", "link", "native");

    private final ShareLogMapper shareLogMapper;
    private final ArticleMapper articleMapper;
    private final ClientIpResolver ipResolver;
    private final RateLimiter rateLimiter;

    @Autowired
    public ShareService(ShareLogMapper shareLogMapper, ArticleMapper articleMapper,
                        ClientIpResolver ipResolver, RateLimiter rateLimiter) {
        this.shareLogMapper = shareLogMapper;
        this.articleMapper = articleMapper;
        this.ipResolver = ipResolver;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 记录一次分享点击。
     * 同一 (articleId, channel, ip, shareDate) 当日去重 — 重复点击不计。
     * 限频:同 IP 每分钟 ≤30 次,每天 ≤100 次。
     */
    @Transactional
    public ShareResult recordShare(Long articleId, String channel, HttpServletRequest req) {
        if (articleId == null) throw new BizException("articleId 不能为空");
        if (channel == null) channel = "link";
        channel = channel.trim().toLowerCase();
        if (!ALLOWED_CHANNELS.contains(channel)) {
            throw new BizException("不支持的分享渠道: " + channel);
        }
        Article a = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getId, articleId)
                .eq(Article::getDeleted, 0));
        if (a == null) throw new BizException("文章不存在");

        String ip = ipResolver.resolve(req);

        // IP 维度限流:防刷
        rateLimiter.acquireOrThrow("share:ip:" + ip, 30, 60);
        rateLimiter.acquireOrThrow("share:daily:" + ip, 100, 86400);

        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 200) ua = ua.substring(0, 200);
        LocalDate today = LocalDate.now();

        // 去重
        Long dup = shareLogMapper.findDupe(articleId, channel, ip, today);
        if (dup != null) {
            return new ShareResult(currentCount(articleId), false);
        }

        ShareLog log = new ShareLog();
        log.setArticleId(articleId);
        log.setChannel(channel);
        log.setIp(ip);
        log.setUserAgent(ua);
        log.setShareDate(today);
        shareLogMapper.insert(log);

        shareLogMapper.incrShareCount(articleId);
        return new ShareResult(currentCount(articleId), true);
    }

    private Integer currentCount(Long articleId) {
        Integer c = shareLogMapper.getShareCount(articleId);
        return c == null ? 0 : c;
    }

    public static class ShareResult {
        public final Integer shareCount;
        public final boolean counted;
        public ShareResult(Integer shareCount, boolean counted) {
            this.shareCount = shareCount;
            this.counted = counted;
        }
    }
}