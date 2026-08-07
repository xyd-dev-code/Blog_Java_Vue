package com.blog.service;

import com.blog.common.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 轻量算术验证码。
 *
 * <p>用于留言/评论提交前的风控。生成一道 1~9 的加减法题，答案存入进程内
 * Caffeine 缓存（5 分钟过期），下发 token。前端提交时携带 token + 答案，
 * 后端校验通过后<b>一次性</b>消费（防重放）。</p>
 *
 * <p>开关走站点配置 {@code captcha_enabled}（1=开 0=关），与 {@link SiteConfigService} 联动，
 * 可在后台动态调整无需重启。多实例部署下各实例独立校验，单机够用。</p>
 */
@Service
public class CaptchaService {

    private final Cache<String, Integer> store = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(20000)
            .build();

    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private final SiteConfigService siteConfigService;

    public CaptchaService(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    private boolean enabled() {
        return "1".equals(siteConfigService.get("captcha_enabled", "1"));
    }

    /** 生成验证码挑战；enabled=false 时返回空题，前端据此隐藏验证码 UI。 */
    public Map<String, Object> generate() {
        Map<String, Object> m = new HashMap<>();
        boolean on = enabled();
        m.put("enabled", on);
        if (!on) {
            m.put("token", "");
            m.put("question", "");
            return m;
        }
        int a = rnd.nextInt(1, 10);
        int b = rnd.nextInt(1, 10);
        int answer;
        String question;
        if (rnd.nextBoolean()) {
            answer = a + b;
            question = a + " + " + b + " = ?";
        } else {
            if (a < b) { int t = a; a = b; b = t; } // 保证减法非负
            answer = a - b;
            question = a + " - " + b + " = ?";
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, answer);
        m.put("token", token);
        m.put("question", question);
        return m;
    }

    /** 校验并一次性消费；关闭时直接放行。 */
    public boolean verify(String token, String answer) {
        if (!enabled()) return true;
        if (token == null || token.isEmpty()) return false;
        Integer expected = store.getIfPresent(token);
        if (expected == null) return false; // 过期或已用过
        store.invalidate(token);
        if (answer == null) return false;
        try {
            return expected == Integer.parseInt(answer.trim());
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
