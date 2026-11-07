package com.forkmyfolio.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filter that implements rate limiting for sensitive endpoints using Bucket4j.
 * It differentiates between anonymous (guest) and authenticated users.
 *
 * <p>
 * The bucket map is bounded to prevent unbounded memory growth from spoofed
 * client identifiers (e.g. random {@code CF-Connecting-IP} / {@code X-Forwarded-For}
 * values). Only proxy headers from trusted (loopback/private) peers are honoured.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /** Hard cap on the number of distinct rate-limit buckets kept in memory. */
    static final int MAX_BUCKETS = 10_000;

    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<String, Bucket>(1024, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_BUCKETS;
                }
            });

    // Guest Limits: 1000 requests per minute for public API (relaxed for testing)
    private final Bandwidth guestGeneralLimit = Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofMinutes(1)));
    private final Bandwidth guestDownloadLimit = Bandwidth.classic(50, Refill.greedy(50, Duration.ofHours(1)));

    // Authenticated Limits: 2000 requests per minute, 100 downloads per hour
    private final Bandwidth authGeneralLimit = Bandwidth.classic(2000, Refill.greedy(2000, Duration.ofMinutes(1)));
    private final Bandwidth authDownloadLimit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofHours(1)));

    // Analytics (DEF-1): reporting endpoints are cached server-side; a modest
    // budget per identity is plenty for dashboard polling and blocks abuse
    // from reaching the external provider.
    private final Bandwidth guestAnalyticsLimit = Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1)));
    private final Bandwidth authAnalyticsLimit = Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Determine if this is a download request or a general public API request
        boolean isDownload = path.contains("/pdf") || path.contains("/markdown") || path.contains("/vcard")
                || path.contains("/backup");
        boolean isPublicPortfolio = path.startsWith("/api/v1/portfolios");
        // DEF-1: analytics endpoints get their own tighter budget — they proxy
        // to an external provider and are cached server-side, so bursts are
        // pointless and abusive volume must not reach Umami.
        boolean isAnalytics = path.startsWith("/api/v1/me/analytics")
                || path.startsWith("/api/v1/admin/analytics")
                || path.startsWith("/api/v1/public/analytics");

        if (!isPublicPortfolio && !isAnalytics && !path.startsWith("/api/v1/me/portfolio/download")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request, isDownload, path);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket(isDownload, isAnalytics));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {} on path: {}", key, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Please try again later.\"}");
        }
    }

    private String resolveKey(HttpServletRequest request, boolean isDownload, String path) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String prefix = isDownload ? "DL:" : "GEN:";

        // Strategy:
        // 1. Authenticated users are keyed by their email/username.
        // 2. Guests are keyed by their resolved Client IP.

        String identifier;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            identifier = "AUTH:" + auth.getName();
        } else {
            identifier = "GUEST:" + resolveClientIp(request);
        }

        // SENIOR IMPROVEMENT: For downloads, include the portfolio slug in the key.
        // This ensures that a user can download 5 different portfolios' PDFs,
        // but can't nuke the download limit for everyone by spamming one.
        if (isDownload) {
            String slug = extractSlug(path);
            return prefix + identifier + ":" + (slug != null ? slug : "global");
        }

        return prefix + identifier;
    }

    /**
     * Resolves the real client IP.
     *
     * <p>
     * Proxy headers ({@code CF-Connecting-IP}, {@code X-Forwarded-For}) are only
     * honoured when the direct peer is a trusted (loopback or private) address.
     * This prevents clients from spoofing an arbitrary IP to create unlimited
     * rate-limit buckets.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            // 1. Check Cloudflare-specific header
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (cfIp != null && !cfIp.isBlank()) {
                return cfIp.trim();
            }

            // 2. Check standard proxy header
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // X-Forwarded-For can be a comma-separated list; the first one is the client.
                return xff.split(",")[0].trim();
            }
        }

        // 3. Fallback to direct remote address (local dev or direct access)
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            // Preserve behaviour for unit tests / direct invocation without a peer.
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(remoteAddr);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private String extractSlug(String path) {
        // Path pattern: /api/v1/portfolios/{slug}/...
        if (path.startsWith("/api/v1/portfolios/")) {
            String sub = path.substring("/api/v1/portfolios/".length());
            int nextSlash = sub.indexOf("/");
            return nextSlash != -1 ? sub.substring(0, nextSlash) : sub;
        }
        return null;
    }

    private Bucket createNewBucket(boolean isDownload, boolean isAnalytics) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal()));

        Bandwidth limit;
        if (isAnalytics) {
            // Analytics proxy endpoints: modest per-identity budget (server-side
            // caching makes higher rates pointless).
            limit = isAuthenticated ? authAnalyticsLimit : guestAnalyticsLimit;
        } else if (isDownload) {
            limit = isAuthenticated ? authDownloadLimit : guestDownloadLimit;
        } else {
            limit = isAuthenticated ? authGeneralLimit : guestGeneralLimit;
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
