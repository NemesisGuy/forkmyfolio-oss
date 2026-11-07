package com.forkmyfolio.service.servicemodel;

/**
 * One ranked analytics metric row (DEF-1): a label (path, referrer, country,
 * …) and its count. Service-layer domain record mapped to the API DTO at the
 * controller boundary (NAG-01 Rule 2).
 */
public record AnalyticsMetricEntry(String name, long value) {
}
