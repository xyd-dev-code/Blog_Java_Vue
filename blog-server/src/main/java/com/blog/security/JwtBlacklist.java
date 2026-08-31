package com.blog.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.dao.DuplicateKeyException;

/** Persistent revocations survive restarts and apply across application instances. */
@Component
public class JwtBlacklist {
    private final JdbcTemplate jdbc;
    public JwtBlacklist(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void revoke(String jti, long expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt <= System.currentTimeMillis()) return;
        try { jdbc.update("INSERT INTO jwt_revocation(jti, expires_at) VALUES (?, ?)", jti, expiresAt); }
        catch (DuplicateKeyException ignored) { /* Already revoked. */ }
    }
    public boolean isRevoked(String jti) {
        if (jti == null) return true;
        return jdbc.queryForObject("SELECT COUNT(*) FROM jwt_revocation WHERE jti = ? AND expires_at > ?",
                Long.class, jti, System.currentTimeMillis()) > 0;
    }
    @Scheduled(fixedDelay = 300000)
    public void evictExpired() {
        jdbc.update("DELETE FROM jwt_revocation WHERE expires_at <= ?", System.currentTimeMillis());
    }
    public int size() { return jdbc.queryForObject("SELECT COUNT(*) FROM jwt_revocation", Integer.class); }
}
