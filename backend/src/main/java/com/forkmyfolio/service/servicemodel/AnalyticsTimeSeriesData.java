package com.forkmyfolio.service.servicemodel;

import java.util.List;

/**
 * Analytics time-series result (DEF-1): parallel pageview and session series.
 * Service-layer domain record mapped to the API DTO at the controller
 * boundary (NAG-01 Rule 2).
 */
public record AnalyticsTimeSeriesData(
        List<AnalyticsPoint> pageviews,
        List<AnalyticsPoint> sessions) {
}
