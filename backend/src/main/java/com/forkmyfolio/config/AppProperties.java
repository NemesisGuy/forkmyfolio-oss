package com.forkmyfolio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cookie cookie = new Cookie();
    private final Security security = new Security();
    private final Oauth2 oauth2 = new Oauth2();
    private final Admin admin = new Admin();
    private final Analytics analytics = new Analytics();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs;
        private long refreshExpirationMs;
        private String refreshCookieName;
    }

    @Data
    public static class Cookie {
        private boolean secure;
        private String samesite;
    }

    @Data
    public static class Security {
        private String cookieDomain;
    }

    @Data
    public static class Oauth2 {
        private List<String> authorizedRedirectUris;
    }

    @Data
    public static class Admin {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }

    /**
     * Analytics (DEF-1) configuration. The encryption key protects provider API
     * credentials at rest (AES-256-GCM). There is deliberately NO default key:
     * without a real key, secret-bearing configuration cannot be saved, which
     * is the safe failure mode.
     */
    @Data
    public static class Analytics {
        /** Base64-encoded 32-byte AES key, e.g. from `openssl rand -base64 32`. */
        private String encryptionKey;
        /** Cache TTL for provider API responses (minutes). */
        private long cacheTtlMinutes = 5;
        /** Reporting requests allowed per user per minute (bucket4j). */
        private long reportingRatePerMinute = 30;
    }
}