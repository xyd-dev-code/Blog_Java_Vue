package com.blog.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.blog.config.BlogProperties;
import com.blog.security.ClientIpResolver;
import com.blog.service.VisitLogService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.mockito.Mockito.*;

class VisitLogFilterTest {
    @Test void debugLoggingDoesNotConsumeFirstVisit() throws Exception {
        VisitLogService visits = mock(VisitLogService.class);
        ClientIpResolver resolver = mock(ClientIpResolver.class);
        when(resolver.resolve(any())).thenReturn("203.0.113.1");
        VisitLogFilter filter = new VisitLogFilter(visits, resolver, new BlogProperties());
        Logger logger = (Logger) LoggerFactory.getLogger(VisitLogFilter.class);
        Level previous = logger.getLevel();
        String ua = "Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36";
        try {
            logger.setLevel(Level.DEBUG);
            for (int i = 0; i < 2; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/articles");
                request.addHeader("User-Agent", ua);
                filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
            }
            verify(visits, times(1)).recordAsync("203.0.113.1", "/api/v1/articles", ua);
        } finally {
            logger.setLevel(previous);
        }
    }
}
