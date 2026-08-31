package com.blog;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMigrationTest {
    @Test void upgradesExistingSchemaWithoutGuessingGuestbookHistoryOrPublishingPrivateContent() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:legacy-review;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1", "sa", "");
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__Baseline_schema.sql"));
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO article(id,title,slug,content,status,password) VALUES(1,'Private','private','preserved body',1,'legacy-plaintext')");
        jdbc.update("INSERT INTO comment(id,article_id,parent_id,nickname,content,status) VALUES(1,1,0,'visitor','preserved comment',1)");
        jdbc.update("INSERT INTO email_subscription(id,email,status,source) VALUES(1,'web@example.com',1,'web'),(2,'admin@example.com',1,'admin')");
        Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("1").load().migrate();
        jdbc.update("INSERT INTO visit_log(ip,province) VALUES('203.0.113.1','test')");
        assertEquals("preserved body", jdbc.queryForObject("SELECT content FROM article WHERE id=1", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT status FROM article WHERE id=1", Integer.class));
        assertNull(jdbc.queryForObject("SELECT password FROM article WHERE id=1", String.class));
        assertEquals("ARTICLE", jdbc.queryForObject("SELECT target_type FROM comment WHERE id=1", String.class));
        assertEquals("preserved comment", jdbc.queryForObject("SELECT content FROM comment WHERE id=1", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT status FROM email_subscription WHERE id=1", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM email_subscription WHERE id=2", Integer.class));
        assertEquals(0, Flyway.configure().dataSource(dataSource).load().migrate().migrationsExecuted);
    }
}
