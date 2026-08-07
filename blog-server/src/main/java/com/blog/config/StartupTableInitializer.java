package com.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 应用启动后自动建表 — 主要服务那些生产环境没有 mysql 客户端、
 * 又无法手敲 SQL 的人。
 *
 * <p>每张表独立判断:已存在跳过,不存在建。幂等可重入。</p>
 */
@Component
@Order(0)  // 最早跑,确保 Filter/Service 起来前表已就绪
public class StartupTableInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupTableInitializer.class);

    private final DataSource dataSource;

    public StartupTableInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable("visit_log", """
                CREATE TABLE IF NOT EXISTS visit_log (
                    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                    ip          VARCHAR(45)  NOT NULL DEFAULT '',
                    device_type VARCHAR(16)  NOT NULL DEFAULT '',
                    os          VARCHAR(64)  NOT NULL DEFAULT '',
                    browser     VARCHAR(64)  NOT NULL DEFAULT '',
                    path        VARCHAR(255) NOT NULL DEFAULT '',
                    user_agent  VARCHAR(512) NOT NULL DEFAULT '',
                    visit_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_visit_time (visit_time),
                    INDEX idx_visit_ip_time (ip, visit_time)
                ) ENGINE=InnoDB COMMENT='公开端点访问日志'
                """);
    }

    private void ensureTable(String tableName, String ddl) {
        try (Connection c = dataSource.getConnection()) {
            try (Statement s = c.createStatement()) {
                try (ResultSet rs = s.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "'")) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        log.debug("[StartupTable] 表已存在,跳过: {}", tableName);
                        return;
                    }
                }
                s.execute(ddl);
                log.info("[StartupTable] 自动建表完成: {}", tableName);
            }
        } catch (Exception e) {
            // 建表失败不应阻止应用启动 — 让运维手动建
            log.warn("[StartupTable] 自动建表失败(可忽略,运维手动建即可): {} - {}",
                    tableName, e.getMessage());
        }
    }
}