package com.blog.filter;

import com.blog.config.BlogProperties;
import com.blog.security.ClientIpResolver;
import com.blog.service.VisitLogService;
import com.blog.util.UserAgentUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * 访问日志过滤器 — 只对"公开前台"端点记录(避免 admin 自己刷后台时污染统计)。
 *
 * <p>路径白名单:SecurityConfig 中已 permitAll 的路径前缀。</p>
 *
 * <p>异步写日志(VisitLogService.recordAsync @Async mailTaskExecutor),
 * 不阻塞请求响应;失败仅 WARN。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)  // 在 JwtAuthFilter 之后跑
public class VisitLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(VisitLogFilter.class);

    /** 短期去重：同一 IP+UA+路径 在 5 秒内只记一次，避免前端/浏览器重复请求刷爆统计 */
    private final Cache<String, Boolean> recentVisitCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofSeconds(5))
            .build();

    /** 会话级去重：同一 IP+UA 在配置窗口内只记一次，防止刷新/恶意请求把数据库打满 */
    private final Cache<String, Boolean> sessionVisitCache;

    private final VisitLogService visitLogService;
    private final ClientIpResolver ipResolver;
    private final BlogProperties props;

    public VisitLogFilter(VisitLogService visitLogService, ClientIpResolver ipResolver, BlogProperties props) {
        this.visitLogService = visitLogService;
        this.ipResolver = ipResolver;
        this.props = props;
        this.sessionVisitCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, props.getVisitLog().getSessionIntervalSeconds())))
                .build();
    }

    /** 公开端点前缀 — 只对这些路径记录访问 */
    private static final String[] TRACKED_PREFIXES = {
            "/api/v1/site",
            "/api/v1/home",
            "/api/v1/articles",
            "/api/v1/categories",
            "/api/v1/tags",
            "/api/v1/archives",
            "/api/v1/projects",
            "/api/v1/friend-links",
            "/api/v1/comments"
    };

    /**
     * 跳过这些本地/回环 IP — admin 自己 SSH / 健康检查 / 本地 curl 都会带这些,
     * 不计入统计(否则管理员一次操作就刷爆数据)。
     */
    private static final java.util.Set<String> SKIPPED_IPS = java.util.Set.of(
            "127.0.0.1", "0:0:0:0:0:0:0:1", "::1", "0.0.0.0", "", "unknown"
    );

    /**
     * favicon / robots.txt 等浏览器自动请求 — 不算有效访问,跳过。
     */
    private static final java.util.Set<String> SKIPPED_PATHS = java.util.Set.of(
            "/api/v1/site/favicon",
            "/api/v1/site/robots.txt",
            "/favicon.ico",
            "/robots.txt"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 先放行,响应照常返回 — 写日志是旁路
        try {
            chain.doFilter(request, response);
        } finally {
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();
            String ip = ipResolver.resolve(request);
            String ua = request.getHeader("User-Agent");

            boolean includeLocalIp = props.getVisitLog().isIncludeLocalIp();
            boolean localIp = SKIPPED_IPS.contains(ip);

            // 诊断日志：帮助判断"为什么没记录"（默认不输出，需开 DEBUG）
            if (log.isDebugEnabled()) {
                if (!"GET".equalsIgnoreCase(method)) {
                    log.debug("[VisitLog] 跳过非 GET 请求: method={}, path={}", method, path);
                } else if (status < 200 || status >= 400) {
                    log.debug("[VisitLog] 跳过非 2xx/3xx 响应: status={}, path={}", status, path);
                } else if (!isTracked(path)) {
                    log.debug("[VisitLog] 跳过非白名单路径: path={}", path);
                } else if (SKIPPED_PATHS.contains(path)) {
                    log.debug("[VisitLog] 跳过自动请求路径: path={}", path);
                } else if (isAdminReferer(request)) {
                    log.debug("[VisitLog] 跳过后台 admin 页面来源: referer={}, path={}", request.getHeader("Referer"), path);
                } else if (isRecentDuplicate(ip, path, ua)) {
                    log.debug("[VisitLog] 跳过短期内重复记录: ip={}, path={}", ip, path);
                } else if (isSessionDuplicate(ip, ua)) {
                    log.debug("[VisitLog] 跳过同会话重复访问: ip={}, sessionWindow={}s", ip,
                            props.getVisitLog().getSessionIntervalSeconds());
                } else if (localIp) {
                    if (includeLocalIp) {
                        log.debug("[VisitLog] 本地/回环 IP 但已开启 include-local-ip,将记录: ip={}, path={}", ip, path);
                    } else {
                        log.debug("[VisitLog] 跳过本地/回环 IP: ip={}, path={}", ip, path);
                    }
                } else if (UserAgentUtil.isBot(ua)) {
                    log.debug("[VisitLog] 跳过爬虫 UA: ua={}, path={}", ua, path);
                } else {
                    log.debug("[VisitLog] 准备记录: ip={}, path={}, status={}, ua={}", ip, path, status, ua);
                }
            }

            // 只记录 GET + 2xx/3xx + 白名单路径 + 跳过自动请求 + 跳过爬虫
            // 本地/回环 IP 是否跳过由配置 includeLocalIp 决定(dev 调试放开,生产默认跳过)
            boolean skipIp = localIp && !includeLocalIp;
            if ("GET".equalsIgnoreCase(method)
                    && status >= 200 && status < 400
                    && isTracked(path)
                    && !SKIPPED_PATHS.contains(path)
                    && !isAdminReferer(request)
                    && !isRecentDuplicate(ip, path, ua)
                    && !isSessionDuplicate(ip, ua)
                    && !skipIp
                    && !UserAgentUtil.isBot(ua)) {
                visitLogService.recordAsync(ip, path, ua);
            }
        }
    }

    private static boolean isTracked(String path) {
        if (path == null) return false;
        for (String p : TRACKED_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    /**
     * 判断请求是否来自后台 admin 页面。后台 SPA 刷新/导航时会调用公共接口
     * (如 /api/v1/site、/api/v1/friend-links),这些调用不应被计入前台访问统计。
     *
     * <p>仅用于统计过滤,不用于安全校验(Referer 可被客户端修改)。</p>
     */
    private static boolean isAdminReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) return false;
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            return path != null && (path.equals("/admin") || path.startsWith("/admin/"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为短期内重复访问。同一 IP + 同一路径 + 同一 UA 在缓存窗口内只记一次,
     * 用于消除前端/浏览器重复请求造成的统计虚高。
     */
    private boolean isRecentDuplicate(String ip, String path, String ua) {
        String key = buildDedupKey(ip, path, ua);
        if (recentVisitCache.getIfPresent(key) != null) {
            return true;
        }
        recentVisitCache.put(key, Boolean.TRUE);
        return false;
    }

    /**
     * 判断是否为同一会话内的重复访问。同一 IP + 同一 UA 在 session 窗口内只记一次,
     * 不管访问多少接口、刷新多少次,都不会把数据库打满。
     */
    private boolean isSessionDuplicate(String ip, String ua) {
        String key = buildDedupKey(ip, null, ua);
        if (sessionVisitCache.getIfPresent(key) != null) {
            return true;
        }
        sessionVisitCache.put(key, Boolean.TRUE);
        return false;
    }

    private static String buildDedupKey(String ip, String path, String ua) {
        return (ip == null ? "" : ip) + "|"
                + (path == null ? "" : path) + "|"
                + (ua == null ? "" : ua);
    }
}