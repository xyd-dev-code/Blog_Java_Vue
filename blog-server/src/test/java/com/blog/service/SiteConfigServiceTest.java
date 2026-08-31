package com.blog.service;

import com.blog.entity.SiteConfig;
import com.blog.mapper.SiteConfigMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteConfigServiceTest {

    @Test
    void publicMapUsesAllowlistAndDropsSensitiveKeys() {
        SiteConfigMapper mapper = mock(SiteConfigMapper.class);
        when(mapper.selectList(null)).thenReturn(List.of(
                config("siteName", "Example Blog"),
                config("aboutContent", "Public profile"),
                config("siteTheme", "ink"),
                config("sensitive_words", "internal moderation list"),
                config("github.token", "must-not-leave-server"),
                config("smtpPassword", "must-not-leave-server")
        ));

        SiteConfigService service = new SiteConfigService(mapper);
        Map<String, String> publicData = service.publicAsMap();

        assertEquals("Example Blog", publicData.get("siteName"));
        assertEquals("Public profile", publicData.get("aboutContent"));
        assertEquals("ink", publicData.get("siteTheme"));
        assertFalse(publicData.containsKey("sensitive_words"));
        assertFalse(publicData.containsKey("github.token"));
        assertFalse(publicData.containsKey("smtpPassword"));

        Map<String, String> adminData = service.allAsMap();
        assertEquals("internal moderation list", adminData.get("sensitive_words"));
        assertFalse(adminData.containsKey("github.token"));
        assertFalse(adminData.containsKey("smtpPassword"));
    }

    @Test
    void saveRejectsSecretLikeKeys() {
        SiteConfigService service = new SiteConfigService(mock(SiteConfigMapper.class));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(Map.of("githubToken", "not-allowed")));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(Map.of("smtp_password", "not-allowed")));
    }

    @Test
    void publicMapFallsBackToSunnyForMissingOrInvalidTheme() {
        SiteConfigMapper mapper = mock(SiteConfigMapper.class);
        when(mapper.selectList(null)).thenReturn(List.of(config("siteTheme", "unsupported")));

        SiteConfigService service = new SiteConfigService(mapper);
        assertEquals("sunny", service.publicAsMap().get("siteTheme"));
        assertEquals("sunny", SiteConfigService.normalizeSiteTheme(null));
        assertEquals("sunny", SiteConfigService.normalizeSiteTheme("unsupported"));
    }

    private static SiteConfig config(String key, String value) {
        SiteConfig config = new SiteConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
