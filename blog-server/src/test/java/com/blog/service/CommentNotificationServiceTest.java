package com.blog.service;

import com.blog.config.BlogProperties;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.security.RateLimiter;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentNotificationServiceTest {
    @Test
    void guestbookMailUsesTrustedBaseUrlAndIncludesHtmlReviewButton() {
        CommentMapper comments = mock(CommentMapper.class);
        ArticleMapper articles = mock(ArticleMapper.class);
        SiteConfigService siteConfig = mock(SiteConfigService.class);
        MailService mail = mock(MailService.class);
        RateLimiter rateLimiter = mock(RateLimiter.class);
        BlogProperties properties = new BlogProperties();
        properties.setSiteBaseUrl("https://blog.example/");
        properties.getSite().setUrl("https://yourdomain.com");
        when(siteConfig.get("commentNotifyEnabled", "1")).thenReturn("1");
        when(siteConfig.get("siteName", "Blog")).thenReturn("拾光小筑");

        CommentNotificationService service = new CommentNotificationService(
                comments, articles, siteConfig, mail, rateLimiter, properties, "admin@example.com");
        Comment comment = new Comment();
        comment.setId(42L);
        comment.setArticleId(0L);
        comment.setTargetType(CommentService.GUESTBOOK);
        comment.setNickname("访客");
        comment.setEmail("visitor@example.com");
        comment.setIp("127.0.0.1");
        comment.setContent("你好 <script>alert(1)</script>");

        service.onCreated(comment);

        verify(mail).send(
                eq("admin@example.com"),
                eq("[待审核-留言板] 访客"),
                argThat(text -> text.contains("https://blog.example/admin/guestbook")
                        && !text.contains("yourdomain.com")),
                argThat(html -> html.contains("href=\"https://blog.example/admin/guestbook\"")
                        && html.contains("前往审核")
                        && html.contains("&lt;script&gt;")
                        && !html.contains("<script>")));
    }
}
