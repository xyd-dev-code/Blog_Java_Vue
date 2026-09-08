package com.blog;

import com.blog.common.BizException;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtUtil;
import com.blog.security.JwtBlacklist;
import com.blog.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:reviewfix;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "blog.jwt.secret=dummy-review-test-key-0000000000000000000000000000000000",
    "blog.mail.enabled=false", "blog.notify.enabled=false", "BLOG_ADMIN_INITIAL_PASSWORD=",
    "blog.local-storage.base-url=https://images.example.invalid", "logging.level.com.blog=WARN"
})
@AutoConfigureMockMvc
public class ReviewFixIntegrationTest {
    static final Path TEMP;
    static { try { TEMP = Files.createTempDirectory("blog-review-test-"); } catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void paths(DynamicPropertyRegistry registry) {
        registry.add("blog.local-storage.dir", () -> TEMP.resolve("images").toString());
        registry.add("blog.export.template-dir", () -> TEMP.resolve("templates").toString());
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired UserMapper users;
    @Autowired JwtUtil jwt;
    @Autowired JwtBlacklist blacklist;
    @Autowired ArticleService articles;
    @Autowired CommentService comments;
    @Autowired SubscriptionService subscriptions;
    @Autowired NotificationDeliveryService deliveries;

    @BeforeEach void fixtures() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
        jdbc.execute("CREATE ALIAS IF NOT EXISTS date_format FOR \"com.blog.ReviewFixIntegrationTest.mysqlDateFormat\"");
        for (String table : List.of("comment_like", "comment_report", "comment", "article_tag", "article",
                "user", "tool_daily_click", "tool", "project", "friend_link", "email_subscription",
                "notification_delivery", "jwt_revocation"))
            jdbc.update("DELETE FROM " + table);
        jdbc.update("INSERT INTO user(id, username, password, role, status) VALUES (1, 'reviewadmin', ?, 'ADMIN', 1)",
                new BCryptPasswordEncoder(4).encode("OldPassword!123"));
        jdbc.update("INSERT INTO article(id, title, slug, content, summary, status, allow_comment, publish_time) VALUES (1,'Public','public','full body','summary',1,1,CURRENT_TIMESTAMP)");
    }
    String token() { return jwt.generate(users.selectById(1L)); }

    @Test void passwordChangeRevokesEveryOldSessionAndDisabledUsersCannotAuthenticate() throws Exception {
        String first = token(), second = token();
        mvc.perform(post("/api/v1/admin/profile/password").header("Authorization", "Bearer " + first)
                .contentType("application/json").content("{\"oldPassword\":\"OldPassword!123\",\"newPassword\":\"NewPassword!456\"}"))
                .andExpect(status().isOk());
        for (String old : List.of(first, second)) mvc.perform(get("/api/v1/admin/profile").header("Authorization", "Bearer " + old))
                .andExpect(status().isUnauthorized());
        String fresh = token();
        mvc.perform(get("/api/v1/admin/profile").header("Authorization", "Bearer " + fresh)).andExpect(status().isOk());
        jdbc.update("UPDATE user SET status=0 WHERE id=1");
        mvc.perform(get("/api/v1/admin/profile").header("Authorization", "Bearer " + fresh)).andExpect(status().isUnauthorized());
    }

    @Test void guestbookSearchCombinesKeywordStatusAndPagination() throws Exception {
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES "
                + "(901,0,'GUESTBOOK',0,'needle author','first',1),"
                + "(902,0,'GUESTBOOK',0,'reader','needle content',1),"
                + "(903,0,'GUESTBOOK',0,'reader','needle spam',2),"
                + "(904,1,'ARTICLE',0,'reader','needle article',1),"
                + "(905,0,'GUESTBOOK',0,'reader','unrelated',1)");
        String auth = "Bearer " + token();
        mvc.perform(get("/api/v1/admin/guestbook").header("Authorization", auth)
                .param("keyword", "  needle  ").param("status", "1").param("size", "1").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(1));
        mvc.perform(get("/api/v1/admin/guestbook").header("Authorization", auth)
                .param("keyword", "needle").param("status", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].id").value(903));
        mvc.perform(get("/api/v1/admin/guestbook").header("Authorization", auth)
                .param("keyword", "   ").param("status", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(3));
        mvc.perform(get("/api/v1/admin/guestbook").header("Authorization", auth).param("keyword", "missing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
    }

    @Test void commentAndReportSearchPreserveStatusAndPagination() throws Exception {
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES "
                + "(911,1,'ARTICLE',0,'needle author','first',1),"
                + "(912,1,'ARTICLE',0,'reader','needle content',1),"
                + "(913,0,'GUESTBOOK',0,'guest','needle guest',1),"
                + "(914,1,'ARTICLE',0,'other','unrelated',2)");
        jdbc.update("INSERT INTO comment_report(id,comment_id,reason,detail,email,status) VALUES "
                + "(921,911,'other','detail','',0),(922,912,'other','detail','',0),"
                + "(923,914,'other','needle detail','',0),(924,914,'other','detail','needle@example.com',0),"
                + "(925,911,'other','detail','',1)");
        String auth = "Bearer " + token();
        mvc.perform(get("/api/v1/admin/comments").header("Authorization", auth)
                .param("keyword", " needle ").param("status", "1").param("size", "1").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value(911));
        mvc.perform(get("/api/v1/admin/reports").header("Authorization", auth)
                .param("keyword", " needle ").param("status", "0").param("size", "2").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.records.length()").value(2));
        mvc.perform(get("/api/v1/admin/reports").header("Authorization", auth)
                .param("keyword", "needle").param("status", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        for (String endpoint : List.of("comments", "reports")) {
            mvc.perform(get("/api/v1/admin/" + endpoint).header("Authorization", auth).param("keyword", "missing"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        }
    }

    @Test void logoutPersistsRevocationAcrossBlacklistInstances() throws Exception {
        String token = token();
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertTrue(new JwtBlacklist(jdbc).isRevoked(jwt.parse(token).getId()));
        mvc.perform(get("/api/v1/admin/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test void adminEndpointsRejectAnonymousAndNonAdminUsers() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());

        jdbc.update("INSERT INTO user(id, username, password, role, status) VALUES (2, 'reviewuser', ?, 'USER', 1)",
                new BCryptPasswordEncoder(4).encode("UserPassword!123"));
        String userToken = jwt.generate(users.selectById(2L));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("USER"));
        mvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test void loginDoesNotRevealWhetherAnAccountExistsOrIsDisabled() throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"WrongPassword!123\"}";
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(body.formatted("missing-user")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        jdbc.update("UPDATE user SET status=0 WHERE id=1");
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(body.formatted("reviewadmin")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test void wrongOldPasswordIsAValidationErrorAndKeepsSessionValid() throws Exception {
        String token = token();
        mvc.perform(post("/api/v1/admin/profile/password").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"oldPassword\":\"WrongPassword!123\",\"newPassword\":\"NewPassword!456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/v1/admin/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test void publicArticlesNeverExposePasswordOrListBodies() throws Exception {
        mvc.perform(get("/api/v1/articles")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].content").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].password").doesNotExist());
        jdbc.update("UPDATE article SET password='dummy-private' WHERE id=1");
        mvc.perform(get("/api/v1/articles/public")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/articles")).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/v1/archives")).andExpect(jsonPath("$.data.total").value(0));
    }

    @Test void anonymousInteractionsWorkButHiddenToolsDoNot() throws Exception {
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(100,1,'ARTICLE',0,'visitor','hello',1)");
        mvc.perform(post("/api/v1/comments/100/like")).andExpect(status().isOk()).andExpect(jsonPath("$.data.likeCount").value(1));
        mvc.perform(post("/api/v1/comments/100/report").contentType("application/json")
                .content("{\"reason\":\"spam\",\"detail\":\"test report\"}")).andExpect(status().isOk());
        jdbc.update("INSERT INTO tool(id,name,slug,url,status,category) VALUES(1,'Tool','tool','https://example.invalid',1,'test')");
        mvc.perform(post("/api/v1/tools/1/click")).andExpect(status().isOk());
        jdbc.update("UPDATE tool SET status=0 WHERE id=1");
        mvc.perform(get("/api/v1/tools/1")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/tools/1/click")).andExpect(status().isNotFound());
    }

    @Test void publicContentResponsesExcludePrivatePersistenceFields() throws Exception {
        jdbc.update("INSERT INTO friend_link(id,name,url,email,status) VALUES(1,'Friend','https://example.invalid','private@example.com',1)");
        mvc.perform(get("/api/v1/friend-links")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Friend"))
                .andExpect(jsonPath("$.data[0].email").doesNotExist())
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist());

        jdbc.update("INSERT INTO tool(id,name,slug,icon,category,url,type,status,notified,sort_order) "
                + "VALUES(1,'Tool','tool','Tools','test','https://example.invalid',1,1,1,1)");
        mvc.perform(get("/api/v1/tools/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.notified").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.createTime").doesNotExist());
        jdbc.update("INSERT INTO tool(id,name,slug,icon,category,url,type,status,sort_order) "
                + "VALUES(2,'Maintenance','maintenance','Tools','test','/tools/maintenance',0,2,2)");
        mvc.perform(get("/api/v1/tools")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].status").value(2));

        jdbc.update("INSERT INTO project(id,name,status,notified) VALUES(1,'Project',1,1)");
        mvc.perform(get("/api/v1/projects")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].name").value("Project"))
                .andExpect(jsonPath("$.data.records[0].status").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].notified").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].deleted").doesNotExist());
    }

    @Test void toolUrlsMustMatchTheirNavigationType() throws Exception {
        String admin = token();
        String externalWithInternalUrl = "{\"name\":\"Tool\",\"slug\":\"tool\",\"icon\":\"Tools\","
                + "\"category\":\"test\",\"url\":\"/tools/tool\",\"type\":1,\"status\":1}";
        mvc.perform(post("/api/v1/admin/tools").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(externalWithInternalUrl))
                .andExpect(status().isBadRequest());

        String unsafeUrl = "{\"name\":\"Tool\",\"slug\":\"tool\",\"icon\":\"Tools\","
                + "\"category\":\"test\",\"url\":\"javascript:alert(1)\",\"type\":1,\"status\":1}";
        mvc.perform(post("/api/v1/admin/tools").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(unsafeUrl))
                .andExpect(status().isBadRequest());
    }

    @Test void commentPolicyAndGuestbookHaveIndependentTargets() {
        CommentDTO dto = comment(1L, "ARTICLE");
        jdbc.update("UPDATE article SET allow_comment=0 WHERE id=1");
        assertThrows(BizException.class, () -> comments.create(dto, "203.0.113.1", "test"));
        jdbc.update("UPDATE article SET status=0 WHERE id=1");
        assertThrows(BizException.class, () -> comments.treeByArticle(1L, false));
        CommentDTO guest = comment(null, "GUESTBOOK");
        var created = comments.create(guest, "203.0.113.1", "test");
        assertEquals("GUESTBOOK", created.getTargetType());
        assertEquals(0L, created.getArticleId());
        assertEquals(1, comments.guestbook().size());
    }

    @Test void repliesStayUnderParentsAndGuestbookModerationPreservesType() {
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(100,1,'ARTICLE',0,'parent','root',1)");
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(101,1,'ARTICLE',100,'child','reply',1)");
        var tree = comments.treeByArticle(1L, false);
        assertEquals(1, tree.size());
        assertEquals(101L, tree.get(0).getReplies().get(0).getId());
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(102,0,'GUESTBOOK',0,'guest','entry',0)");
        comments.approve(102L);
        assertEquals("GUESTBOOK", jdbc.queryForObject("SELECT target_type FROM comment WHERE id=102", String.class));
    }

    @Test void concurrentLikesKeepRecordsAndCounterConsistent() throws Exception {
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(100,1,'ARTICLE',0,'visitor','hello',1)");
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Object>> tasks = new ArrayList<>();
            for (int i=0; i<20; i++) { final int actor=i; tasks.add(() -> comments.like(100L, "203.0.113." + actor, null)); }
            for (Future<Object> future : pool.invokeAll(tasks)) future.get(10, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        assertEquals(20, jdbc.queryForObject("SELECT like_count FROM comment WHERE id=100", Integer.class));
        assertEquals(20, jdbc.queryForObject("SELECT COUNT(*) FROM comment_like WHERE comment_id=100", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM comment WHERE id=100", Integer.class));
    }

    @Test void cappedTreesExposeRemainingRepliesThroughCursorPagination() throws Exception {
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(100,1,'ARTICLE',0,'parent','root',1)");
        List<Object[]> rows = new ArrayList<>();
        for (long id = 101; id <= 1102; id++) rows.add(new Object[] {id});
        jdbc.batchUpdate("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(?,1,'ARTICLE',100,'child','reply',1)", rows);
        var root = comments.treeByArticle(1L, false, 1, 1).get(0);
        assertEquals(999, root.getReplies().size());
        assertTrue(root.isHasMoreReplies());
        mvc.perform(get("/api/v1/comments/100/replies").param("afterId", "1099"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records.length()").value(3))
                .andExpect(jsonPath("$.data.records[0].id").value(1100))
                .andExpect(jsonPath("$.data.records[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.hasMore").value(false));
        jdbc.update("UPDATE article SET status=0 WHERE id=1");
        mvc.perform(get("/api/v1/comments/100/replies")).andExpect(status().isNotFound());
    }

    @Test void approvedRepliesRemainReachableWhenParentIsHidden() {
        jdbc.update("INSERT INTO comment(id,article_id,target_type,parent_id,nickname,content,status) VALUES(100,1,'ARTICLE',0,'parent','hidden',2),(101,1,'ARTICLE',100,'child','visible',1)");
        var tree = comments.treeByArticle(1L, false);
        assertEquals(1, tree.size());
        assertEquals(101L, tree.get(0).getId());
    }

    @Test void publicationSetsDateAndServiceValidatesImportedDtos() {
        jdbc.update("UPDATE article SET status=0, publish_time=NULL WHERE id=1");
        ArticleDTO dto = new ArticleDTO();
        dto.setId(1L); dto.setTitle("Published"); dto.setContent("body"); dto.setStatus(1);
        assertNotNull(articles.update(dto).getPublishTime());
        dto.setSlug("../unsafe");
        assertThrows(BizException.class, () -> articles.update(dto));
        dto.setSlug("safe"); dto.setPassword("secret");
        assertThrows(BizException.class, () -> articles.update(dto));
    }

    @Test void confirmationIsExpiringSingleUseAndCannotUndoUnsubscribe() {
        jdbc.update("INSERT INTO email_subscription(id,email,status,token,confirmation_token,confirmation_expires_at) VALUES(1,'person@example.com',0,'unsubscribe','confirm',DATEADD('HOUR',1,CURRENT_TIMESTAMP))");
        subscriptions.confirm("confirm");
        assertEquals(1, jdbc.queryForObject("SELECT status FROM email_subscription WHERE id=1", Integer.class));
        assertNull(jdbc.queryForObject("SELECT confirmation_token FROM email_subscription WHERE id=1", String.class));
        subscriptions.unsubscribe("unsubscribe");
        subscriptions.confirm("confirm");
        assertEquals(2, jdbc.queryForObject("SELECT status FROM email_subscription WHERE id=1", Integer.class));
        assertThrows(BizException.class, () -> subscriptions.subscribe("other@example.com","web",new org.springframework.mock.web.MockHttpServletRequest()));
    }

    @Test void successfulRecipientsAreNotResentWhenAnotherFails() {
        var successful = new java.util.concurrent.atomic.AtomicInteger();
        assertTrue(deliveries.deliver("ARTICLE",1L,1L, () -> { successful.incrementAndGet(); return true; }));
        assertFalse(deliveries.deliver("ARTICLE",1L,2L, () -> false));
        assertTrue(new NotificationDeliveryService(jdbc).deliver("ARTICLE",1L,1L, () -> { successful.incrementAndGet(); return true; }));
        assertEquals(1, successful.get());
        assertEquals("RETRY", jdbc.queryForObject("SELECT status FROM notification_delivery WHERE subscription_id=2",String.class));
    }

    @Test void failedDeliveriesHaveBackoffAndAnAttemptLimit() {
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.BooleanSupplier failedSend = () -> { attempts.incrementAndGet(); return false; };
        assertFalse(deliveries.deliver("ARTICLE", 1L, 1L, failedSend));
        assertFalse(deliveries.deliver("ARTICLE", 1L, 1L, failedSend));
        assertEquals(1, attempts.get());
        for (int i = 2; i <= 5; i++) {
            jdbc.update("UPDATE notification_delivery SET next_attempt_at = DATEADD('HOUR',-1,CURRENT_TIMESTAMP)");
            assertEquals(i == 5, deliveries.deliver("ARTICLE", 1L, 1L, failedSend));
        }
        assertTrue(deliveries.deliver("ARTICLE", 1L, 1L, failedSend));
        assertEquals(5, attempts.get());
        assertEquals("FAILED", jdbc.queryForObject("SELECT status FROM notification_delivery WHERE subscription_id=1", String.class));
    }

    @Test void expiredConfirmationCannotActivateSubscription() {
        jdbc.update("INSERT INTO email_subscription(id,email,status,token,confirmation_token,confirmation_expires_at) VALUES(1,'expired@example.com',0,'unsubscribe','expired',DATEADD('HOUR',-1,CURRENT_TIMESTAMP))");
        subscriptions.confirm("expired");
        assertEquals(0, jdbc.queryForObject("SELECT status FROM email_subscription WHERE id=1", Integer.class));
    }

    @Test void schemaAndOpenApiAreUsable() throws Exception {
        jdbc.update("INSERT INTO visit_log(ip,province) VALUES('203.0.113.1','test')");
        mvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.openapi").exists());
    }

    public static String mysqlDateFormat(java.sql.Timestamp value, String format) {
        if (value == null) return null;
        return value.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern(format.replace("%Y", "yyyy").replace("%m", "MM").replace("%d", "dd")));
    }

    private CommentDTO comment(Long article, String target) {
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(article); dto.setTargetType(target);
        dto.setNickname("visitor"); dto.setEmail("visitor@example.com"); dto.setContent("hello");
        return dto;
    }
}
