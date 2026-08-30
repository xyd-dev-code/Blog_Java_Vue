package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.CommentReportMapper;
import com.blog.mapper.UserMapper;
import com.blog.vo.CommentPublicVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceAdminAvatarTest {

    @Mock private CommentMapper commentMapper;
    @Mock private ArticleMapper articleMapper;
    @Mock private UserMapper userMapper;
    @Mock private SiteConfigService siteConfigService;
    @Mock private CommentNotificationService notificationService;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private CommentReportMapper commentReportMapper;

    private CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(commentMapper, articleMapper, userMapper, siteConfigService,
                notificationService, commentLikeMapper, commentReportMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicAdminReplyUsesCurrentAvatarInsteadOfDeletedSnapshot() {
        User admin = admin("DemoAuthor", "admin@example.com", "https://img.example.com/current.jpg");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

        Comment reply = reply("DemoAuthor", "admin@example.com",
                "https://img.example.com/deleted.jpg", "", "");

        CommentPublicVO result = service.toPublic(reply);

        assertTrue(result.getIsAdmin());
        assertEquals("https://img.example.com/current.jpg", result.getAvatar());
    }

    @Test
    @SuppressWarnings("unchecked")
    void visitorWithSameNicknameIsNotMarkedAsAdmin() {
        User admin = admin("DemoAuthor", "admin@example.com", "https://img.example.com/current.jpg");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

        Comment visitor = reply("DemoAuthor", "admin@example.com",
                "https://img.example.com/visitor.jpg", "203.0.113.8", "Mozilla/5.0");

        CommentPublicVO result = service.toPublic(visitor);

        assertFalse(result.getIsAdmin());
        assertEquals("https://img.example.com/visitor.jpg", result.getAvatar());
    }

    @Test
    void profileUpdateSynchronizesHistoricalAdminReplies() {
        User previous = admin("DemoAuthor", "admin@example.com", "https://img.example.com/old.jpg");
        previous.setUsername("admin");
        User current = admin("DemoAuthor", "admin@example.com", "https://img.example.com/current.jpg");
        current.setUsername("admin");
        when(commentMapper.syncAdminReplyProfile(
                "admin@example.com", "DemoAuthor", "admin",
                "DemoAuthor", "admin@example.com", "https://img.example.com/current.jpg"))
                .thenReturn(16);

        int affected = service.syncAdminReplyProfile(previous, current);

        assertEquals(16, affected);
    }

    private User admin(String nickname, String email, String avatar) {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setNickname(nickname);
        admin.setEmail(email);
        admin.setAvatar(avatar);
        admin.setRole("ADMIN");
        admin.setStatus(1);
        return admin;
    }

    private Comment reply(String nickname, String email, String avatar, String ip, String ua) {
        Comment comment = new Comment();
        comment.setId(2L);
        comment.setArticleId(1L);
        comment.setParentId(1L);
        comment.setNickname(nickname);
        comment.setEmail(email);
        comment.setAvatar(avatar);
        comment.setIp(ip);
        comment.setUa(ua);
        comment.setContent("回复内容");
        comment.setStatus(1);
        return comment;
    }
}
