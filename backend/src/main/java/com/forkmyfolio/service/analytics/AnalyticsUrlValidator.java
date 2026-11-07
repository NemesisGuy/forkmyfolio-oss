package com.forkmyfolio.service.analytics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Validates externally-supplied analytics configuration values before they
 * are stored (DEF-1 security requirements): base URLs against SSRF, and
 * script paths / provider identifiers against script injection.
 *
 * <p>URL policy:
 * <ul>
 *   <li>Absolute http(s) URL with a host; no userinfo (user:pass@host).</li>
 *   <li>HTTPS required for non-loopback hosts (http is allowed only for
 *       loopback hosts so local development against a local Umami works).</li>
 *   <li>Resolved addresses must not be loopback/site-local (RFC1918)/link-local
 *       (169.254.x, incl. cloud metadata)/any-local/multicast — except the
 *       explicit loopback development case above.</li>
 *   <li>Only ports 80/443/8443 (plus the scheme defaults) are allowed.</li>
 * </ul>
 * Resolution happens at save time; DNS rebinding between save and call is a
 * documented residual risk (the deployment's egress policy is the stronger
 * control for that).</p>
 *
 * <p>Script-path policy (anti-injection): a script path must be a RELATIVE
 * path — a single leading slash, no scheme, no backslashes, no whitespace,
 * no quotes or HTML-significant characters, no query/fragment parts. It is
 * concatenated only onto a validated base URL, so a hostile value can never
 * turn the tracking script into an arbitrary URL or inline payload.</p>
 */
@Component
@Slf4j
public class AnalyticsUrlValidator {

    private static final int MAX_LENGTH = 512;

    /** Validates and normalizes a base URL; throws IllegalArgumentException on violation. */
    public String validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Analytics base URL is required.");
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Analytics base URL is too long.");
        }
        URI uri;
        try {
            uri = URI.create(trimmed);
            // URI.create is lenient about some malformed input; force re-parse.
            uri = new URI(trimmed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Analytics base URL is not a valid URL.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Analytics base URL must use http or https.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Analytics base URL must include a host.");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Analytics base URL must not contain credentials.");
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443 && port != 8443) {
            throw new IllegalArgumentException("Analytics base URL must use port 80, 443 or 8443.");
        }

        boolean loopbackHost = isLoopbackLiteral(host) || "localhost".equalsIgnoreCase(host);
        if (!loopbackHost && !scheme.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("Analytics base URL must use https (http is only allowed for localhost).");
        }

        if (loopbackHost) {
            return normalize(trimmed);
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Analytics base URL host could not be resolved.");
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                log.warn("Rejected analytics base URL resolving to a private/loopback address: {}", trimmed);
                throw new IllegalArgumentException(
                        "Analytics base URL must not point at private, loopback or link-local addresses.");
            }
        }
        return normalize(trimmed);
    }

    private boolean isLoopbackLiteral(String host) {
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private String normalize(String url) {
        String normalized = url;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** Checks a stored URL without re-throwing; used defensively before provider calls. */
    public boolean isSafe(String storedUrl) {
        try {
            validate(storedUrl);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Script-path validation (anti-injection)
    // ------------------------------------------------------------------

    /**
     * Characters permitted in a configurable script path: unreserved +
     * sub-delimiters + ':'/'@' (RFC3986 path-safe), '/' separators, and '%'
     * escapes. Deliberately excludes whitespace, quotes, angle brackets,
     * backslashes, '?' and '#' so a path can never break out of the URL path
     * component or carry an inline payload.
     */
    private static final java.util.regex.Pattern SAFE_PATH =
            java.util.regex.Pattern.compile("^/[A-Za-z0-9._~!$&'()*+,;=:@%/\\-]*$");

    /**
     * Validates a configurable tracking/recorder script path. Must be a
     * relative path starting with exactly one '/'. Returns the normalized
     * value; throws IllegalArgumentException on violation.
     */
    public String validateScriptPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Script path must not be empty.");
        }
        String path = rawPath.trim();
        if (path.length() > 255) {
            throw new IllegalArgumentException("Script path is too long.");
        }
        if (path.startsWith("//")) {
            throw new IllegalArgumentException("Script path must be relative (a leading '//' is a protocol-relative URL).");
        }
        if (!SAFE_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException(
                    "Script path contains characters that are not allowed in a URL path.");
        }
        return path;
    }

    /** Non-throwing variant of {@link #validateScriptPath(String)}. */
    public boolean isSafeScriptPath(String path) {
        try {
            validateScriptPath(path);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Provider identifier validation
    // ------------------------------------------------------------------

    /** GA4 Measurement IDs are public identifiers of the form G-XXXXXXXXXX. */
    private static final java.util.regex.Pattern GA4_MEASUREMENT_ID =
            java.util.regex.Pattern.compile("^G-[A-Z0-9]{4,12}$");

    /** Validates a GA4 Measurement ID; throws IllegalArgumentException on violation. */
    public String validateMeasurementId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("GA4 Measurement ID must not be empty.");
        }
        String id = rawId.trim().toUpperCase();
        if (!GA4_MEASUREMENT_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("GA4 Measurement ID must look like G-XXXXXXXXXX.");
        }
        return id;
    }

    /** Visible for tests. */
    static boolean anyPrivate(InetAddress[] addresses) {
        return Arrays.stream(addresses).anyMatch(a ->
                a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()
                        || a.isAnyLocalAddress() || a.isMulticastAddress());
    }
}
