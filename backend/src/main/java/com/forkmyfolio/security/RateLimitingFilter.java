package com.forkmyfolio.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    // v1.2.11 SECURITY: /auth/** was previously unthrottled (brute-force and
    // registration-abuse surface). Defaults allow ~24 auth calls/minute per
    // client IP with 4x headroom over a full e2e suite run; tunable by env,
    // no secrets involved. Field initializers keep `new RateLimitingFilter()`
    // working in unit tests.
    @Value("${app.rate-limit.auth-capacity:120}")
    private int authCapacity = 120;

    @Value("${app.rate-limit.auth-refill:120}")
    private int authRefill = 120;

    @Value("${app.rate-limit.auth-refill-minutes:5}")
    private int authRefillMinutes = 5;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Determine if this is a download request or a general public API request
        // Phase 0.19: /cover-letter is a PDF download endpoint and shares the
        // download budget with /pdf, /markdown and /vcard.
        boolean isDownload = path.contains("/pdf") || path.contains("/markdown") || path.contains("/vcard")
                || path.contains("/cover-letter") || path.contains("/backup");
        boolean isPublicPortfolio = path.startsWith("/api/v1/portfolios");
        // DEF-1: analytics endpoints get their own tighter budget — they proxy
        // to an external provider and are cached server-side, so bursts are
        // pointless and abusive volume must not reach Umami.
        boolean isAnalytics = path.startsWith("/api/v1/me/analytics")
                || path.startsWith("/api/v1/admin/analytics")
                || path.startsWith("/api/v1/public/analytics");
        // v1.2.11 SECURITY: these are the unauthenticated credential/account
        // endpoints that used to bypass the filter entirely.
        // /refresh-token is deliberately NOT here: every SPA page load probes
        // it, and it only consumes a server-issued random UUID (no brute-force
        // value). Including it would exhaust one shared bucket mid-e2e.
        boolean isAuthRoute = "POST".equalsIgnoreCase(request.getMethod()) && (
                path.equals("/api/v1/auth/login")
                        || path.equals("/api/v1/auth/register")
                        || path.equals("/api/v1/auth/guest-login"));

        if (!isAuthRoute && !isPublicPortfolio && !isAnalytics
                && !path.startsWith("/api/v1/me/portfolio/download")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request, isDownload, path, isAuthRoute);
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> createNewBucket(isDownload, isAnalytics, isAuthRoute));

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

    private String resolveKey(HttpServletRequest request, boolean isDownload, String path, boolean isAuthRoute) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String prefix = isAuthRoute ? "AUTH-IP:" : (isDownload ? "DL:" : "GEN:");

        // Auth endpoints are ALWAYS keyed by client IP, never by a principal:
        // a stale SecurityContext from another request must not let a caller
        // switch buckets mid-burst.
        if (isAuthRoute) {
            return prefix + resolveClientIp(request);
        }

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
     * Proxy headers are honoured ONLY when the direct peer is trusted
     * (loopback/private — i.e. the bundled nginx/docker network). From a
     * multi-value {@code X-Forwarded-For} we take the <b>last</b> entry: nginx
     * appends the real peer itself, while anything to the left is
     * client-supplied and therefore forgeable. {@code CF-Connecting-IP} is
     * ignored entirely (a client can set that header directly). From a public
     * peer we use {@code getRemoteAddr()} only — no headers at all.
     *
     * <p>Trade-off: behind Docker NAT every visitor shares the nginx peer IP,
     * so the bucket is per-host rather than per-visitor. That is conservative
     * (no bypass) and the auth budget is sized accordingly.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            // nginx appends the real peer as the LAST XFF entry. Anything to
            // the left came from the client and must be ignored (v1.2.11).
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] parts = xff.split(",");
                String last = parts[parts.length - 1].trim();
                if (!last.isEmpty()) {
                    return last;
                }
            }
        }

        // Direct/public peer (or no usable proxy header): never trust headers.
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

    private Bucket createNewBucket(boolean isDownload, boolean isAnalytics, boolean isAuthRoute) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal()));

        Bandwidth limit;
        if (isAuthRoute) {
            // Credential/account endpoints: independent per-IP budget.
            limit = Bandwidth.classic(authCapacity,
                    Refill.greedy(authRefill, Duration.ofMinutes(authRefillMinutes)));
        } else if (isAnalytics) {
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
