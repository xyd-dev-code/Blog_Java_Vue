package com.blog.config;

import com.blog.security.RateLimiter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期清理内存中的过期限流窗口,防泄漏。
 * 5 分钟跑一次,只删 1 分钟没新请求的 key。
 */
@Component
public class RateLimiterCleanup {

    private final RateLimiter rateLimiter;

    public RateLimiterCleanup(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    public void cleanup() {
        rateLimiter.evictStale();
    }
}
