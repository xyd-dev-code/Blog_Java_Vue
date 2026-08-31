package com.blog.controller;

import com.blog.common.BizException;
import com.blog.service.SiteConfigService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminSiteControllerTest {

    @Test
    void rejectsUnsupportedSiteTheme() {
        SiteConfigService service = mock(SiteConfigService.class);
        AdminSiteController controller = new AdminSiteController(service);

        assertThrows(BizException.class,
                () -> controller.save(Map.of("siteTheme", "visitor-choice")));
        verifyNoInteractions(service);
    }

    @Test
    void savesSupportedSiteTheme() {
        SiteConfigService service = mock(SiteConfigService.class);
        AdminSiteController controller = new AdminSiteController(service);

        controller.save(Map.of("siteTheme", "ink"));

        verify(service).save(Map.of("siteTheme", "ink"));
    }
}
