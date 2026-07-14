package com.blog.security;

import com.blog.config.BlogProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private final BlogProperties props;

    public JwtUtil(BlogProperties props) {
        this.props = props;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getJwt().getExpiration());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
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
