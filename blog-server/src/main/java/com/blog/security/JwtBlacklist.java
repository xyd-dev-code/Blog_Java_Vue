package com.blog.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 黑名单 — 内存级,够单实例。
 *
 * <p>logout 时把当前 token 的 jti(或 exp)放进来,JwtAuthFilter 解析时查一次。
 * 进程重启会丢黑名单,这是已知 trade-off(单实例部署 OK;
 * 多实例要换 Redis SET,带 EXPIRE)。</p>
 */
@Component
public class JwtBlacklist {

    /** key = jti 或 token hash,value = 到期时间(毫秒),过期自动清理 */
    private final ConcurrentHashMap<String, Long> revoked = new ConcurrentHashMap<>();

    /**
     * 把 token 加入黑名单直到它自然过期。
     * 由 AuthService.logout 调用。
     */
    public void revoke(String jtiOrToken, long expireAtMillis) {
        if (jtiOrToken == null || jtiOrToken.isBlank()) return;
        revoked.put(jtiOrToken, expireAtMillis);
    }

    public boolean isRevoked(String jtiOrToken) {
        if (jtiOrToken == null) return false;
        Long exp = revoked.get(jtiOrToken);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            revoked.remove(jtiOrToken);
            return false;
        }
        return true;
    }

    /** 维护用,定期调用清掉过期项。当前依赖惰性删除,足够。 */
    public int size() {
        return revoked.size();
    }
}
