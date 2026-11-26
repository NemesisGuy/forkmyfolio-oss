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
    void testCfHeaderFromPublicPeerIsIgnored() throws ServletException, IOException {
        // v1.2.11 SECURITY: CF-Connecting-IP is CLIENT-supplied — a public
        // peer must never be able to pick a fresh bucket with it.
        when(request.getRequestURI()).thenReturn("/api/v1/portfolios/jane-doe");
        when(request.getRemoteAddr()).thenReturn("172.68.0.1"); // public peer
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.1");
        when(securityContext.getAuthentication()).thenReturn(null);

        rateLimitingFilter.doFilter(request, response, filterChain);
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.2");
        rateLimitingFilter.doFilter(request, response, filterChain);

        // Both requests share the peer-IP bucket (not two attacker-chosen ones).
        verify(filterChain, times(2)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
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

    // ------------------------------------------------------------------
    // v1.2.11 SECURITY: auth routes were completely unthrottled, and
    // client-supplied IP headers could be used to choose a fresh bucket.
    // ------------------------------------------------------------------

    private void stubWriter() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    void authLogin_exhaustsAtCapacity_andIgnoresPrincipal() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(authentication.getPrincipal()).thenReturn("alice@example.com");
        stubWriter();

        for (int i = 0; i < 120; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(120)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        rateLimitingFilter.doFilter(request, response, filterChain); // 121st
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, times(120)).doFilter(request, response);
    }

    @Test
    void authRoutesShareOnePerIpBucket() throws ServletException, IOException {
        stubWriter();
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("203.0.113.11");
        when(securityContext.getAuthentication()).thenReturn(null);

        String[] routes = {"/api/v1/auth/login", "/api/v1/auth/register",
                "/api/v1/auth/guest-login"};
        for (int i = 0; i < 40; i++) {
            for (String route : routes) {
                when(request.getRequestURI()).thenReturn(route);
                rateLimitingFilter.doFilter(request, response, filterChain);
            }
        }
        verify(filterChain, times(120)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        rateLimitingFilter.doFilter(request, response, filterChain);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void authBucketIsPerClientIp() throws ServletException, IOException {
        stubWriter();
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(securityContext.getAuthentication()).thenReturn(null);

        when(request.getRemoteAddr()).thenReturn("203.0.113.20");
        for (int i = 0; i < 120; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }
        clearInvocations(response);
        stubWriter();

        // A different client IP gets its own bucket.
        when(request.getRemoteAddr()).thenReturn("203.0.113.21");
        rateLimitingFilter.doFilter(request, response, filterChain);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void spoofedProxyHeadersCannotResetAuthBucket() throws ServletException, IOException {
        stubWriter();
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.9"); // trusted proxy
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 192.168.1.9");
        when(request.getHeader("CF-Connecting-IP")).thenReturn("9.9.9.9");
        when(securityContext.getAuthentication()).thenReturn(null);

        for (int i = 0; i < 120; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }
        // Attacker flips BOTH client-controlled headers, but nginx-appended
        // rightmost XFF entry is unchanged: still the same exhausted bucket.
        when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8, 192.168.1.9");
        when(request.getHeader("CF-Connecting-IP")).thenReturn("7.7.7.7");
        clearInvocations(response);
        stubWriter();
        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void nonAuthAuthPathsAreNotLimited() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");
        when(request.getMethod()).thenReturn("POST");
        when(securityContext.getAuthentication()).thenReturn(null);

        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
