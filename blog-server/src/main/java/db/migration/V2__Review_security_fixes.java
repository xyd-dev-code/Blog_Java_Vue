package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Connection;
import java.sql.SQLException;

/** Additive migration: preserves existing articles/comments and never guesses ambiguous guestbook history. */
public class V2__Review_security_fixes extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                new org.springframework.core.io.ClassPathResource("db/migration/V1__Baseline_schema.sql"));
        addColumn(connection, "visit_log", "province", "VARCHAR(64) NOT NULL DEFAULT ''");
        addIndex(connection, "visit_log", "idx_visit_province", "province, visit_time");
        addColumn(connection, "comment", "target_type", "VARCHAR(16) NOT NULL DEFAULT 'ARTICLE'");
        addColumn(connection, "email_subscription", "confirmation_token", "VARCHAR(64) DEFAULT NULL");
        addColumn(connection, "email_subscription", "confirmation_expires_at", "DATETIME DEFAULT NULL");
        try (var statement = connection.createStatement()) {
            addIndex(connection, "comment", "idx_comment_target_parent", "target_type, article_id, parent_id, status, create_time");
            addIndex(connection, "email_subscription", "idx_subscription_confirmation", "confirmation_token");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS jwt_revocation (
                        jti VARCHAR(64) PRIMARY KEY,
                        expires_at BIGINT NOT NULL
                    )
                    """);
            addIndex(connection, "jwt_revocation", "idx_jwt_expiration", "expires_at");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS notification_delivery (
                        content_type VARCHAR(16) NOT NULL,
                        content_id BIGINT NOT NULL,
                        subscription_id BIGINT NOT NULL,
                        status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                        attempts INT NOT NULL DEFAULT 0,
                        next_attempt_at TIMESTAMP NULL,
                        lease_until TIMESTAMP NULL,
                        claim_token VARCHAR(36) NULL,
                        PRIMARY KEY (content_type, content_id, subscription_id)
                    )
                    """);
            statement.execute("UPDATE article SET publish_time = COALESCE(update_time, create_time, CURRENT_TIMESTAMP) WHERE status = 1 AND publish_time IS NULL");
            // Password-protected articles were never implemented safely. Keep their content as drafts,
            // remove the exposed plaintext credential, and require explicit administrator republication.
            statement.execute("UPDATE article SET status = 0, password = NULL WHERE password IS NOT NULL AND password <> ''");
            // Old public subscriptions may have been auto-confirmed while SMTP was disabled.
            // Preserve addresses, but require fresh consent; administrator-added records remain active.
            statement.execute("UPDATE email_subscription SET status = 0, confirm_time = NULL WHERE status = 1 AND (source IS NULL OR source <> 'admin')");
        }
    }

    private void addColumn(Connection connection, String table, String column, String definition) throws SQLException {
        try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) {
            if (columns.next()) return;
        }
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void addIndex(Connection connection, String table, String name, String columns) throws SQLException {
        try (var indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (indexes.next()) if (name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return;
        }
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE INDEX " + name + " ON " + table + "(" + columns + ")");
        }
    }
}
