package com.blog.security;
import com.blog.common.BizException;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterWindowTest {
    @Test void cleanupRespectsHourlyAndDailyWindows() {
        AtomicLong millis = new AtomicLong(1_000_000);
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
        };
        RateLimiter limiter = new RateLimiter(clock);
        limiter.acquireOrThrow("hour",1,3600);
        limiter.acquireOrThrow("day",1,86400);
        millis.addAndGet(120_000); limiter.evictStale();
        assertThrows(BizException.class, () -> limiter.acquireOrThrow("hour",1,3600));
        assertThrows(BizException.class, () -> limiter.acquireOrThrow("day",1,86400));
        millis.addAndGet(3600_000); limiter.evictStale();
        assertDoesNotThrow(() -> limiter.acquireOrThrow("hour",1,3600));
        assertThrows(BizException.class, () -> limiter.acquireOrThrow("day",1,86400));
    }
}
