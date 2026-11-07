package com.forkmyfolio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitingFilter = new RateLimitingFilter();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGuestGeneralRateLimitExceeded() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/jane-doe");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(securityContext.getAuthentication()).thenReturn(null);

        // Mock response writer
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Guest general limit is 1000/min with a greedy refill, so a fast burst
        // will eventually hit the ceiling. Verify that requests DO get blocked.
        for (int i = 0; i < 3000; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }

        // Some requests must have been rejected at this volume.
        verify(response, atLeastOnce()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, atMost(3000)).doFilter(request, response);
    }

    @Test
    void testCloudflareIpResolution() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/jane-doe");
        // Cloudflare header present
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.1");
        // Remote addr is the CF edge node
        when(request.getRemoteAddr()).thenReturn("172.68.0.1");
        when(securityContext.getAuthentication()).thenReturn(null);

        // Should use CF-Connecting-IP as the key
        rateLimitingFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);

        // If we change CF head, it should be a new bucket
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.2");
        rateLimitingFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    void testXForwardedForFallback() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/jane-doe");
        when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 2.2.2.2");
        when(securityContext.getAuthentication()).thenReturn(null);

        rateLimitingFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDownloadSlugIsolation() throws ServletException, IOException {
        // User A downloads Jane's PDF 5 times -> limited
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/jane-doe/pdf");
        when(request.getRemoteAddr()).thenReturn("192.168.1.5");
        when(securityContext.getAuthentication()).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        for (int i = 0; i < 50; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }

        // 51st download for Jane is blocked
        rateLimitingFilter.doFilter(request, response, filterChain);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        // BUT, 1st download for John should STILL work (isolation)
        reset(response);
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/john-smith/pdf");
        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, times(51)).doFilter(request, response);
    }

    @Test
    void testGuestDownloadRateLimitExceeded() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/jane-doe/pdf");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(securityContext.getAuthentication()).thenReturn(null);

        // Mock response writer
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Exhaust the bucket (guest download limit is 50 per hour)
        for (int i = 0; i < 50; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }

        // The 51st request should be blocked
        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, times(50)).doFilter(request, response);
    }

    @Test
    void testAuthenticatedUserHigherLimit() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/jane-doe");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Mock authenticated user
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user@example.com");
        when(authentication.getPrincipal()).thenReturn("user@example.com");

        // Authenticated users have 100 requests limit
        for (int i = 0; i < 50; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }

        // Should still be allowed after 50 requests
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, times(50)).doFilter(request, response);
    }
}
