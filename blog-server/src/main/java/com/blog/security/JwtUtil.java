package com.blog.security;

import com.blog.config.BlogProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import com.blog.entity.User;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private final BlogProperties props;

    public JwtUtil(BlogProperties props) {
        String secret = props.getJwt().getSecret();
        // HS256 推荐至少 32 字节(256 位)随机 key;短于此会直接启动失败,
        // 避免误用弱口令/空串在生产里签 token。
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "blog.jwt.secret 未配置。请设置 BLOG_JWT_SECRET 环境变量(>= 32 字节随机串)。");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "blog.jwt.secret 过短(" + keyBytes.length + " 字节);HS256 至少需要 32 字节。");
        }
        this.props = props;
        long exp = props.getJwt().getExpiration();
        if (exp <= 0) {
            throw new IllegalStateException("blog.jwt.expiration 不能为 0 或负数");
        }
        if (exp > 24 * 3600 * 1000L) {
            // 超过 24 小时视为异常(正常应该是 2 小时)——可能是 yml 覆盖丢失,
            // 回退到了 Java 默认值 7 天。打印 ERROR 让运维注意到。
            log.warn("blog.jwt.expiration = {} ms ({} 小时),超过 24 小时安全建议上限。"
                    + "请确认 application.yml 的 blog.jwt.expiration 覆盖是否生效。",
                    exp, exp / 3600_000);
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getJwt().getExpiration());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .claim("credentials", credentialsVersion(user))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    // HMAC binds sessions to the current password without exposing its BCrypt hash in JWTs.
    // Legacy tokens without this claim require a fresh login after deployment.
    private String credentialsVersion(User user) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((user.getId() + ":" + user.getPassword())
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Cannot verify credentials version", e);
        }
    }

    public boolean matchesCredentials(Claims claims, User user) {
        String version = claims.get("credentials", String.class);
        return user != null && user.getPassword() != null && version != null
                && MessageDigest.isEqual(version.getBytes(StandardCharsets.UTF_8),
                        credentialsVersion(user).getBytes(StandardCharsets.UTF_8));
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public String resolve(String header) {
        if (header == null) return null;
        String prefix = props.getJwt().getPrefix();
        if (header.startsWith(prefix)) return header.substring(prefix.length());
        return null;
    }

    public long expirationSeconds() {
        return props.getJwt().getExpiration() / 1000;
    }

    public Map<String, Object> summary() {
        return Map.of("header", props.getJwt().getHeader(),
                      "prefix", props.getJwt().getPrefix(),
                      "expireSeconds", expirationSeconds());
    }
}
