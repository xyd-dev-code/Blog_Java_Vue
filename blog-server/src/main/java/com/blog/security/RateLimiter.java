package com.blog.security;

import com.blog.common.BizException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进程内(单实例)滑动窗口限流器,够 cover 单机部署,够挡撞库和刷评论/分享。
 *
 * <p>多实例场景需要换成 Redis token-bucket;目前先满足单实例需求,
 * 不引入外部依赖。</p>
 *
 * <p>key 一般是 "ip" 或 "ip:userId";window 是秒。</p>
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 试图消耗一个令牌;超限就抛 429。
     *
     * @param key      限流 key(通常 "login:203.0.113.1")
     * @param limit    窗口内允许的最大次数
     * @param windowSec 窗口宽度(秒)
     */
    public void acquireOrThrow(String key, int limit, int windowSec) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSec * 1000L;
        Window w = windows.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            // 滑动窗口:把过期计数从头部弹掉
            while (!w.timestamps.isEmpty() && w.timestamps.peekFirst() < windowStart) {
                w.timestamps.pollFirst();
                w.count.decrementAndGet();
            }
            if (w.count.get() >= limit) {
                throw new BizException(429, "操作过于频繁,请稍后再试");
            }
            w.timestamps.addLast(now);
            w.count.incrementAndGet();
        }
    }

    /** 命中即 -1,失败不增计数;用于"密码错 N 次后锁 10 分钟"这种累加器。 */
    public void acquireOrThrowCounter(String key, int limit, int windowSec) {
        acquireOrThrow(key, limit, windowSec);
    }

    /**
     * 定期清理空窗口的 key,防内存泄漏。
     * 由 ScheduledTask 或 @Scheduled 调用(默认 5 分钟一次)。
     */
    public void evictStale() {
        long cutoff = System.currentTimeMillis() - 60_000L; // 1 分钟没新请求的窗口
        windows.entrySet().removeIf(e -> {
            Window w = e.getValue();
            synchronized (w) {
                return w.timestamps.isEmpty()
                        || (w.timestamps.peekLast() != null && w.timestamps.peekLast() < cutoff);
            }
        });
    }

    /** 调试用,清空所有计数(不暴露在 controller)。 */
    void resetAll() {
        windows.clear();
    }

    private static class Window {
        final java.util.ArrayDeque<Long> timestamps = new java.util.ArrayDeque<>();
        final AtomicInteger count = new AtomicInteger(0);
    }
}
