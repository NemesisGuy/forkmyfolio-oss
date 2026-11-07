package com.forkmyfolio.exception;

/**
 * DEF-1: an analytics provider (e.g. Umami) could not be reached or returned
 * an error. Translated to a graceful HTTP 503 by
 * {@link GlobalExceptionHandler} — never a 500 — so dashboards degrade
 * cleanly when the provider is down or misconfigured. Messages are curated
 * provider-layer strings (no secrets, no URLs) and are safe to surface.
 */
public class AnalyticsProviderException extends RuntimeException {

    private final Integer providerStatus;

    public AnalyticsProviderException(String message) {
        this(message, null, null);
    }

    public AnalyticsProviderException(String message, Integer providerStatus) {
        this(message, providerStatus, null);
    }

    public AnalyticsProviderException(String message, Throwable cause) {
        this(message, null, cause);
    }

    private AnalyticsProviderException(String message, Integer providerStatus, Throwable cause) {
        super(message, cause);
        this.providerStatus = providerStatus;
    }

    /** HTTP status the provider returned, when the failure was an HTTP error. */
    public Integer getProviderStatus() {
        return providerStatus;
    }
}
