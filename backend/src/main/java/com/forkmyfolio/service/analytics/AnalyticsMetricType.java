package com.forkmyfolio.service.analytics;

/**
 * Whitelisted breakdown dimensions for analytics metrics queries.
 * Mapped to provider-specific parameter values by each provider implementation.
 */
public enum AnalyticsMetricType {
    PATH("path"),
    REFERRER("referrer"),
    COUNTRY("country"),
    BROWSER("browser"),
    OS("os"),
    DEVICE("device"),
    EVENT("event"),
    TITLE("title");

    private final String umamiType;

    AnalyticsMetricType(String umamiType) {
        this.umamiType = umamiType;
    }

    public String umamiType() {
        return umamiType;
    }

    /** Parses a metric type from API input; null when unknown (caller decides policy). */
    public static AnalyticsMetricType fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
