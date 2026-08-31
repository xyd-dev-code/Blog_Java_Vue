package com.blog.service;

import com.blog.common.BizException;
import com.blog.config.BlogProperties;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.security.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 评论通知业务。
 *
 * <p>两阶段通知:</p>
 * <ol>
 *   <li><b>onCreated(c)</b> — 评论提交时(无论 status),给管理员发"有新评论待审核"邮件,
 *       管理员不需要先刷后台就知道有新评论。</li>
 *   <li><b>onApproved(id)</b> — 审核通过后,给<b>评论者本人</b>发"审核通过"邮件;
 *       若有 parentId,再给<b>被回复者</b>发"被回复"通知(原文+回复内容预览)。</li>
 * </ol>
 *
 * <p>防滥用 / 越权:</p>
 * <ul>
 *   <li>收件邮箱 sha256 后做限频 key,1h 内同邮箱只发 1 次(防 spam 灌收件箱)</li>
 *   <li>收件人 == 评论者本人 → 跳过(防自发自收)</li>
 *   <li>邮箱格式无效 → 跳过</li>
 *   <li>管理员邮箱留空 → 跳过</li>
 * </ul>
 */
@Service
public class CommentNotificationService {
    private static final Logger log = LoggerFactory.getLogger(CommentNotificationService.class);

    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final SiteConfigService siteConfigService;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final String adminEmail;
    private final String siteUrl;

    public CommentNotificationService(CommentMapper commentMapper,
                                      ArticleMapper articleMapper,
                                      SiteConfigService siteConfigService,
                                      MailService mailService,
                                      RateLimiter rateLimiter,
                                      BlogProperties blogProperties,
                                      @Value("${blog.mail.admin-email:}") String adminEmail) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
        this.siteConfigService = siteConfigService;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
        this.adminEmail = adminEmail;
        this.siteUrl = trimSlash(blogProperties.getSite().getUrl());
    }

    /**
     * 评论提交时调用 — 给管理员发"待审核"通知。
     * 整个方法 @Async,不阻塞评论提交响应。
     */
    @Async("mailTaskExecutor")
    public void onCreated(Comment c) {
        if (c == null || c.getId() == null) return;
        try {
            if (!"1".equals(siteConfigService.get("commentNotifyEnabled", "1"))) return;
            if (!isValidEmail(adminEmail)) return;
            // 不发给评论者本人(只在 adminEmail == 评论者邮箱 时跳过 — 大多数情况不触发)
            if (equalsIgnoreCase(adminEmail, c.getEmail())) return;

            String subject;
            String text;
            String articleTitle = articleTitleOf(c.getArticleId());
            String snippet = clip(c.getContent());
            String link;
            boolean isGuestbook = CommentService.GUESTBOOK.equals(c.getTargetType());

            if (isGuestbook) {
                subject = "[待审核-留言板] " + safe(c.getNickname());
                link = siteUrl + "/admin/guestbook";
                text = String.format(
                        "留言板有新留言待审核:\n\n作者:%s\n邮箱:%s\nIP:%s\n内容:%s\n\n去审核:%s\n\n%s",
                        safe(c.getNickname()), safe(c.getEmail()), safe(c.getIp()), snippet, link, signature());
            } else {
                subject = "[待审核] " + safe(articleTitle);
                link = siteUrl + "/admin/comments";
                text = String.format(
                        "文章《%s》有新评论待审核:\n\n作者:%s\n邮箱:%s\nIP:%s\n内容:%s\n\n去审核:%s\n\n%s",
                        articleTitle, safe(c.getNickname()), safe(c.getEmail()), safe(c.getIp()), snippet, link, signature());
            }
            mailService.send(adminEmail, subject, text, null);
        } catch (Exception e) {
            log.warn("[CommentNotify] onCreated 失败 id={} errType={}",
                    c.getId(), e.getClass().getSimpleName());
        }
    }

    /**
     * 审核通过时调用 — 给评论者本人发"审核通过"邮件;
     * 若有 parentId,再给被回复者发"被回复"通知。
     */
    @Async("mailTaskExecutor")
    public void onApproved(Long commentId) {
        if (commentId == null) return;
        try {
            Comment c = commentMapper.selectById(commentId);
            if (c == null) {
                log.warn("[CommentNotify] 评论已不存在 id={}", commentId);
                return;
            }
            if (c.getStatus() == null || c.getStatus() != 1) return;
            if (!"1".equals(siteConfigService.get("commentNotifyEnabled", "1"))) return;

            // 1) 给评论者本人发"审核通过"邮件
            if (isValidEmail(c.getEmail()) && !equalsIgnoreCase(c.getEmail(), adminEmail)) {
                sendApproveNotifyToCommenter(c);
            }

            // 2) 若有被回复者,且被回复者 != 评论者本人 → 发"被回复"通知
            if (c.getParentId() != null && c.getParentId() > 0) {
                Comment parent = commentMapper.selectById(c.getParentId());
                if (parent != null && isValidEmail(parent.getEmail())
                        && !equalsIgnoreCase(parent.getEmail(), c.getEmail())) {
                    sendReplyNotify(parent, c);
                }
            }
        } catch (Exception e) {
            log.warn("[CommentNotify] onApproved 失败 id={} errType={}",
                    commentId, e.getClass().getSimpleName());
        }
    }

    /**
     * 管理员后台回复时调用 — 仅给被回复者发"被回复"通知,
     * 不给自己发"审核通过"邮件(管理员回复直接通过,无需审核通知)。
     */
    @Async("mailTaskExecutor")
    public void onAdminReply(Comment reply) {
        if (reply == null || reply.getId() == null) return;
        try {
            if (!"1".equals(siteConfigService.get("commentNotifyEnabled", "1"))) return;
            if (reply.getParentId() == null || reply.getParentId() <= 0) return;
            Comment parent = commentMapper.selectById(reply.getParentId());
            if (parent == null) return;
            // 避免自发自收:被回复者邮箱无效或与回复者相同则跳过
            if (!isValidEmail(parent.getEmail()) || equalsIgnoreCase(parent.getEmail(), reply.getEmail())) return;
            sendReplyNotify(parent, reply);
        } catch (Exception e) {
            log.warn("[CommentNotify] onAdminReply 失败 id={} errType={}",
                    reply.getId(), e.getClass().getSimpleName());
        }
    }

    // ────────────────────────────────────── 模板与发送 ──────────────────────────────────────

    private void sendApproveNotifyToCommenter(Comment c) {
        if (!rateLimit(c.getEmail())) return;
        String articleTitle = articleTitleOf(c.getArticleId());
        String subject = "[评论已通过] " + safe(articleTitle);
        String snippet = clip(c.getContent());
        String link;
        if (CommentService.GUESTBOOK.equals(c.getTargetType())) {
            link = siteUrl + "/guestbook#comment-" + c.getId();
        } else {
            link = siteUrl + "/articles/" + c.getArticleId() + "#comment-" + c.getId();
        }
        String text = String.format(
                "%s,你%s的评论已通过审核,谢谢!\n\n文章:%s\n内容:%s\n\n查看:%s\n\n%s",
                safe(c.getNickname()),
                CommentService.GUESTBOOK.equals(c.getTargetType())
                        ? "在留言板留下" : "在文章下留下",
                articleTitle, snippet, link, signature());
        mailService.send(c.getEmail(), subject, text, null);
    }

    private void sendReplyNotify(Comment parent, Comment reply) {
        if (!rateLimit(parent.getEmail())) return;
        String articleTitle = articleTitleOf(reply.getArticleId());
        String subject = "[回复通知] " + safe(parent.getNickname()) + ",有人在你的评论下回复";
        String snippet = clip(reply.getContent());
        String link;
        if (CommentService.GUESTBOOK.equals(reply.getTargetType())) {
            link = siteUrl + "/guestbook#comment-" + reply.getId();
        } else {
            link = siteUrl + "/articles/" + reply.getArticleId() + "#comment-" + reply.getId();
        }
        String text = String.format(
                "%s 在文章《%s》中回复了你的评论:\n\n原评论:%s\n\n回复内容:%s\n\n查看:%s\n\n%s",
                safe(reply.getNickname()), articleTitle, clip(parent.getContent()), snippet, link, signature());
        mailService.send(parent.getEmail(), subject, text, null);
    }

    // ────────────────────────────────────── 工具 ──────────────────────────────────────

    private String articleTitleOf(Long articleId) {
        if (articleId == null) return "(未知文章)";
        try {
            com.blog.entity.Article a = articleMapper.selectById(articleId);
            return a == null ? "(已删除文章#" + articleId + ")" : a.getTitle();
        } catch (Exception e) {
            return "(文章#" + articleId + ")";
        }
    }

    private String signature() {
        return "— " + safe(siteConfigService.get("siteName", "Blog"));
    }

    private String clip(String s) {
        if (s == null) return "";
        String trimmed = s.strip();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed;
    }

    private boolean isValidEmail(String s) {
        return s != null && !s.isBlank() && EMAIL_RE.matcher(s).matches();
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    /**
     * 收件人限频:同邮箱 1h 内只发 1 次。
     * key 用 sha256(email),避免内存里直接存邮箱原文(隐私 + 内存上限控制)。
     */
    private boolean rateLimit(String email) {
        try {
            String hash = sha256Hex(email.toLowerCase());
            rateLimiter.acquireOrThrow("mailnotify:to:" + hash, 1, 3600);
            return true;
        } catch (BizException e) {
            log.info("[CommentNotify] 1h 内已发送过，跳过重复通知");
            return false;
        } catch (Exception e) {
            log.warn("[CommentNotify] rateLimiter 异常，放行通知: {}", e.getClass().getSimpleName());
            return true;
        }
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n]", " ");
    }

    private static String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
