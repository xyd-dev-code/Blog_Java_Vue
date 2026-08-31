package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.SiteConfig;
import com.blog.mapper.SiteConfigMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SiteConfigService {
    public static final String DEFAULT_SITE_THEME = "sunny";
    private static final Set<String> SUPPORTED_SITE_THEMES = Set.of("sunny", "ink");

    /**
     * 匿名站点接口允许返回的配置项。站点配置表也可能被旧版本写入内部配置，
     * 因此公开接口必须使用正向白名单，不能直接返回整张表。
     */
    private static final Set<String> PUBLIC_KEYS = Set.of(
            // dot.notation
            "site.title", "site.subtitle", "site.description", "site.keywords",
            "site.logo", "site.favicon", "site.copyright", "site.icp", "site.police",
            "seo.title", "seo.description", "seo.keywords", "seo.canonical",
            "social.github", "social.twitter", "social.weibo", "social.qq", "social.email",
            "footer.text", "footer.beian", "footer.icp",
            "comment.audit", "comment.placeholder", "github.url",
            // camelCase / legacy seed keys
            "siteTitle", "siteSubtitle", "siteDescription", "siteKeywords",
            "siteLogo", "siteFavicon", "siteCopyright", "siteIcp", "sitePolice",
            "seoTitle", "seoDescription", "seoKeywords", "seoCanonical",
            "socialGithub", "socialTwitter", "socialWeibo", "socialQq", "socialEmail",
            "footerText", "footerBeian", "footerIcp",
            "commentAudit", "commentPlaceholder", "githubUrl",
            "siteName", "motto", "description", "keywords", "beian", "comment_audit",
            "github", "email", "authorName", "userNickname",
            // 站点级主题（只允许管理员修改，匿名端只读）
            "siteTheme",
            // About 页面个人资料全部由数据库维护
            "greeting", "roleTitle", "userBio", "aboutContent", "aboutSkills", "aboutTimeline",
            // 天气卡展示配置
            "weatherCity", "weatherLat", "weatherLon", "captcha_enabled"
    );

    private final SiteConfigMapper siteConfigMapper;

    public SiteConfigService(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    public Map<String, String> allAsMap() {
        Map<String, String> map = new HashMap<>();
        siteConfigMapper.selectList(null).forEach(c -> {
            if (!isSensitiveKey(c.getConfigKey())) {
                map.put(c.getConfigKey(), c.getConfigValue());
            }
        });
        return map;
    }

    /** 仅供无需登录的 /api/v1/site 使用。 */
    public Map<String, String> publicAsMap() {
        Map<String, String> map = new HashMap<>();
        siteConfigMapper.selectList(null).forEach(c -> {
            String key = c.getConfigKey();
            if (key != null && PUBLIC_KEYS.contains(key) && !isSensitiveKey(key)) {
                map.put(key, c.getConfigValue());
            }
        });
        // 旧数据库可能还没有该键，或曾被人工写入非法值；公开端始终拿到安全默认值。
        map.put("siteTheme", normalizeSiteTheme(map.get("siteTheme")));
        return map;
    }

    public static boolean isSupportedSiteTheme(String themeId) {
        return themeId != null && SUPPORTED_SITE_THEMES.contains(themeId);
    }

    public static String normalizeSiteTheme(String themeId) {
        return isSupportedSiteTheme(themeId) ? themeId : DEFAULT_SITE_THEME;
    }

    /**
     * 纵深防御：历史数据库里即使残留 github.token / smtpPassword 等字段，
     * 也禁止进入管理端或公开端的 JSON，更禁止被本站配置接口继续写入。
     */
    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) return false;
        String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return normalized.contains("password")
                || normalized.contains("passwd")
                || normalized.contains("secret")
                || normalized.endsWith("token")
                || normalized.contains("apikey")
                || normalized.contains("accesskey")
                || normalized.contains("privatekey")
                || normalized.contains("credential");
    }

    public String get(String key, String def) {
        SiteConfig c = siteConfigMapper.selectOne(
            new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, key)
        );
        return c == null ? def : c.getConfigValue();
    }

    public void save(Map<String, String> data) {
        if (data == null) return;
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (isSensitiveKey(e.getKey())) {
                throw new IllegalArgumentException("敏感配置必须通过环境变量注入，禁止写入站点配置: " + e.getKey());
            }
            SiteConfig c = siteConfigMapper.selectOne(
                new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, e.getKey())
            );
            if (c == null) {
                c = new SiteConfig();
                c.setConfigKey(e.getKey());
                c.setConfigValue(e.getValue());
                siteConfigMapper.insert(c);
            } else {
                c.setConfigValue(e.getValue());
                siteConfigMapper.updateById(c);
            }
        }
    }
}
