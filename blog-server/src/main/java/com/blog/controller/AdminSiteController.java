package com.blog.controller;

import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Tag(name = "后台 - 站点配置")
@RestController
@RequestMapping("/api/v1/admin/site")
public class AdminSiteController {
    private final SiteConfigService siteConfigService;

    /** URL 字段(logo/favicon/avatar 等)必须 http/https 开头,防 javascript: 注入 */
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[^\\s]+$");

    /** 私网/链路本地/回环 IP,SSRF 防御 */
    private static final Pattern PRIVATE_IP = Pattern.compile(
            "^(127\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|0\\.|169\\.254\\.|::1|fc|fd|fe80)"
    );

    /**
     * SiteConfig 合法 key 完整白名单。
     * 必须精确匹配,不能用 startsWith(否则 siteEvil/commentX 会通过)。
     * 前端两种 key 风格:camelCase (siteTitle) 和 dot.notation (site.title)。
     * 散列 key(authorName/userNickname/email)单独列出。
     */
    private static final Set<String> ALLOWED_KEYS = Set.of(
            // dot.notation
            "site.title", "site.subtitle", "site.description", "site.keywords",
            "site.logo", "site.favicon", "site.copyright", "site.icp", "site.police",
            "seo.title", "seo.description", "seo.keywords", "seo.canonical",
            "social.github", "social.twitter", "social.weibo", "social.qq",
            "social.email",
            "footer.text", "footer.beian", "footer.icp",
            "comment.audit", "comment.placeholder",
            "github.url",
            // camelCase
            "siteTitle", "siteSubtitle", "siteDescription", "siteKeywords",
            "siteLogo", "siteFavicon", "siteCopyright", "siteIcp", "sitePolice",
            "seoTitle", "seoDescription", "seoKeywords", "seoCanonical",
            "socialGithub", "socialTwitter", "socialWeibo", "socialQq",
            "socialEmail",
            "footerText", "footerBeian", "footerIcp",
            "commentAudit", "commentPlaceholder",
            // 评论/留言通知开关(0/1)
            "commentNotifyEnabled", "commentNotifyToAuthor", "commentNotifyToAdmin",
            "comment.notify.enabled", "comment.notify.toAuthor", "comment.notify.toAdmin",
            "githubUrl",
            // 散列 key
            "authorName", "userNickname", "email",
            // 前台与旧 seed 使用的 key(与数据库实际 key 保持一致,避免后台保存被拒)
            "siteName", "motto", "description", "keywords", "beian", "comment_audit", "github",
            // 关于页原有技术栈配置
            "aboutSkills",
            // 天气卡：博主所在城市（访客定位失败时的兜底）
            "weatherCity", "weatherLat", "weatherLon"
    );

    public AdminSiteController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @GetMapping
    @Operation(summary = "获取全部站点配置")
    public R<Map<String, String>> all() {
        return R.ok(siteConfigService.allAsMap());
    }

    @PutMapping
    @Operation(summary = "保存配置(批量 key-value,key 必须在白名单内)")
    public R<Void> save(@RequestBody Map<String, String> data) {
        if (data == null || data.isEmpty()) throw new BizException("无配置项");
        if (data.size() > 200) throw new BizException("单次最多 200 项");

        Map<String, String> sanitized = new HashMap<>(data.size());
        for (Map.Entry<String, String> e : data.entrySet()) {
            String key = e.getKey();
            if (key == null || key.isBlank()) continue;
            if (SiteConfigService.isSensitiveKey(key)) {
                throw new BizException("敏感配置必须通过环境变量注入，禁止保存到站点配置: " + key);
            }
            // key 必须完全等于白名单中的一项(防 siteEvil/commentX 等 startsWith 绕过)
            if (!ALLOWED_KEYS.contains(key)) {
                throw new BizException("不允许的配置 key: " + key);
            }
            if (key.length() > 100) throw new BizException("配置 key 过长");
            String v = e.getValue();
            if (v != null && v.length() > 5000) throw new BizException("配置 value 过长: " + key);
            // URL 类 key 强制 https?:// + 公网 IP(纵深防御,SiteController.favicon 已二次校验)
            if (isUrlKey(key)) {
                if (v != null && !v.isBlank()) {
                    if (!URL_PATTERN.matcher(v).matches()) {
                        throw new BizException("URL 字段必须以 http:// 或 https:// 开头: " + key);
                    }
                    if (!isPublicUrl(v)) {
                        throw new BizException("URL 字段不可指向私网/内网 IP: " + key);
                    }
                }
            }
            sanitized.put(key, v);
        }
        siteConfigService.save(sanitized);
        return R.ok();
    }

    private static boolean isUrlKey(String key) {
        String lower = key.toLowerCase();
        return lower.endsWith("logo") || lower.endsWith("favicon")
                || lower.endsWith("url") || lower.endsWith("image")
                || lower.endsWith("avatar");
    }

    private static boolean isPublicUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;
            if (PRIVATE_IP.matcher(host).find()) return false;
            try {
                InetAddress addr = InetAddress.getByName(host);
                if (PRIVATE_IP.matcher(addr.getHostAddress()).find()) return false;
            } catch (UnknownHostException ignored) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
