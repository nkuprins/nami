package com.app.backend.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void doFilter_allowsUnclassifiedRequests_withoutRateLimiting() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/saved");
        request.setRequestURI("/api/saved");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 100; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(100)).doFilter(request, response);
    }

    @Test
    void doFilter_limitsPropertyReads_at60PerMinute() throws Exception {
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/properties");
            req.setRequestURI("/api/properties");
            req.setRemoteAddr("5.5.5.5");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/properties");
        request.setRequestURI("/api/properties");
        request.setRemoteAddr("5.5.5.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void doFilter_limitsPropertyWrites_at10PerMinute() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/properties");
            req.setRequestURI("/api/properties");
            req.setRemoteAddr("6.6.6.6");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/properties");
        request.setRequestURI("/api/properties");
        request.setRemoteAddr("6.6.6.6");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void doFilter_limitsPresignUploads_at10PerMinute() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/uploads/presign");
            req.setRequestURI("/api/uploads/presign");
            req.setRemoteAddr("7.7.7.7");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/uploads/presign");
        request.setRequestURI("/api/uploads/presign");
        request.setRemoteAddr("7.7.7.7");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void doFilter_separatesReadAndWriteBuckets_forSameIp() throws Exception {
        // Exhaust the write budget (10) for an IP.
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/properties");
            req.setRequestURI("/api/properties");
            req.setRemoteAddr("8.8.8.8");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        // Reads for the same IP still have their own (untouched) budget.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/properties");
        request.setRequestURI("/api/properties");
        request.setRemoteAddr("8.8.8.8");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_allowsFirst5AuthRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
        }

        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    void doFilter_returns429_afterExceedingLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRequestURI("/api/auth/login");
            req.setRemoteAddr("10.0.0.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    void doFilter_usesXForwardedFor_forIpExtraction() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRequestURI("/api/auth/login");
            req.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void doFilter_separatesBucketsPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRequestURI("/api/auth/login");
            req.setRemoteAddr("1.1.1.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void evictStaleBuckets_resetsRateLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRequestURI("/api/auth/login");
            req.setRemoteAddr("3.3.3.3");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        filter.evictStaleBuckets();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("3.3.3.3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
