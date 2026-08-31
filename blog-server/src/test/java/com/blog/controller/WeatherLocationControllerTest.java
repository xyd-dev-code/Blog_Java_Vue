package com.blog.controller;

import com.blog.security.ClientIpResolver;
import com.blog.service.WeatherLocationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherLocationControllerTest {
    @Test
    void usesVisitorIpAndPreventsSharedLocationCaching() {
        var locations = mock(WeatherLocationService.class);
        var expected = new WeatherLocationService.Location("长沙市", "雨花区", 28.13, 113.03);
        when(locations.locate("203.0.113.10")).thenReturn(expected);
        var controller = new WeatherLocationController(new ClientIpResolver(), locations);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.20");
        var response = controller.location(request);
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertNotNull(response.getBody());
        assertEquals(expected, response.getBody().getData());
        verify(locations).locate("203.0.113.10");
    }

    @Test
    void trustedLocalProxyUsesItsAppendedVisitorIp() {
        var locations = mock(WeatherLocationService.class);
        var controller = new WeatherLocationController(new ClientIpResolver(), locations);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.20, 203.0.113.10");
        controller.location(request);
        verify(locations).locate("203.0.113.10");
    }
}
