package com.forkmyfolio.exception;

/**
 * DEF-1: analytics reporting is not usable for the requested scope — the
 * provider is unconfigured, reporting is disabled, or credentials are
 * missing. Translated to a graceful HTTP 503 by {@link GlobalExceptionHandler}.
 */
public class AnalyticsUnavailableException extends RuntimeException {

    public AnalyticsUnavailableException(String message) {
        super(message);
    }
}
