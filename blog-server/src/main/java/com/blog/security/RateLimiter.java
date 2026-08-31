package com.blog.security;
import com.blog.common.BizException;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/** Single-instance sliding windows; use shared storage before deploying multiple instances. */
@Component
public class RateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    public RateLimiter() { this(Clock.systemUTC()); }
    RateLimiter(Clock clock) { this.clock = clock; }
    public void acquireOrThrow(String key, int limit, int windowSec) {
        if (key == null || limit < 1 || windowSec < 1) throw new IllegalArgumentException("Invalid rate window");
        long now = clock.millis();
        windows.compute(key, (ignored, previous) -> {
            Window window = previous == null ? new Window() : previous;
            window.duration = Math.max(window.duration, windowSec * 1000L);
            window.prune(now);
            if (window.timestamps.size() >= limit) throw new BizException(429, "操作过于频繁，请稍后再试");
            window.timestamps.addLast(now);
            return window;
        });
    }
    public void evictStale() {
        long now = clock.millis();
        windows.forEach((key, ignored) -> windows.computeIfPresent(key, (k, window) -> {
            window.prune(now);
            return window.timestamps.isEmpty() ? null : window;
        }));
    }
    void resetAll() { windows.clear(); }
    private static final class Window {
        final ArrayDeque<Long> timestamps = new ArrayDeque<>();
        long duration;
        void prune(long now) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - duration) timestamps.removeFirst();
        }
    }
}
