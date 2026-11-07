package com.forkmyfolio.service.servicemodel;

/**
 * A single analytics time-series bucket (DEF-1): epoch milliseconds and the
 * bucket value. Service-layer domain record mapped to the API DTO at the
 * controller boundary (NAG-01 Rule 2).
 */
public record AnalyticsPoint(long timestamp, long value) {
}
