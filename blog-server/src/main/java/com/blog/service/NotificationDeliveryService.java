package com.blog.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Per-recipient delivery state; SMTP itself cannot provide exactly-once delivery after a process crash. */
@Service
public class NotificationDeliveryService {
    private final JdbcTemplate jdbc;
    public NotificationDeliveryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean deliver(String type, Long contentId, Long subscriberId, BooleanSupplier send) {
        try {
            jdbc.update("INSERT INTO notification_delivery(content_type, content_id, subscription_id) VALUES (?, ?, ?)",
                    type, contentId, subscriberId);
        } catch (DuplicateKeyException ignored) { /* Existing delivery keeps its state. */ }
        Instant now = Instant.now();
        String claim = UUID.randomUUID().toString();
        int claimed = jdbc.update("""
                UPDATE notification_delivery SET status = 'PROCESSING', attempts = attempts + 1,
                       lease_until = ?, claim_token = ?
                 WHERE content_type = ? AND content_id = ? AND subscription_id = ?
                   AND status NOT IN ('SENT', 'FAILED') AND attempts < 5
                   AND (next_attempt_at IS NULL OR next_attempt_at <= ?)
                   AND (lease_until IS NULL OR lease_until <= ?)
                """, Timestamp.from(now.plusSeconds(120)), claim, type, contentId, subscriberId,
                Timestamp.from(now), Timestamp.from(now));
        if (claimed == 0) {
            // An interrupted final attempt must eventually stop retrying too.
            jdbc.update("""
                    UPDATE notification_delivery SET status = 'FAILED'
                     WHERE content_type = ? AND content_id = ? AND subscription_id = ?
                       AND status = 'PROCESSING' AND attempts >= 5 AND lease_until <= ?
                    """, type, contentId, subscriberId, Timestamp.from(now));
            String state = jdbc.queryForObject("SELECT status FROM notification_delivery WHERE content_type = ? AND content_id = ? AND subscription_id = ?",
                    String.class, type, contentId, subscriberId);
            return "SENT".equals(state) || "FAILED".equals(state);
        }
        int attempts = jdbc.queryForObject("SELECT attempts FROM notification_delivery WHERE content_type = ? AND content_id = ? AND subscription_id = ?",
                Integer.class, type, contentId, subscriberId);
        boolean sent;
        try { sent = send.getAsBoolean(); }
        catch (RuntimeException ex) { sent = false; }
        String state = sent ? "SENT" : attempts >= 5 ? "FAILED" : "RETRY";
        jdbc.update("""
                UPDATE notification_delivery SET status = ?, next_attempt_at = ?, lease_until = NULL, claim_token = NULL
                 WHERE content_type = ? AND content_id = ? AND subscription_id = ? AND claim_token = ?
                """, state, sent ? null : Timestamp.from(Instant.now().plusSeconds(60L << Math.min(attempts, 10))),
                type, contentId, subscriberId, claim);
        if ("FAILED".equals(state)) org.slf4j.LoggerFactory.getLogger(getClass())
                .warn("通知重试次数耗尽: type={}, contentId={}, subscriberId={}", type, contentId, subscriberId);
        return sent || "FAILED".equals(state);
    }
}
