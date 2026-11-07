package com.forkmyfolio.service.servicemodel;

import java.util.List;

/**
 * Analytics summary result for a reporting period (DEF-1). A service-layer
 * domain record — the controller boundary maps it to the API DTO, keeping
 * DTOs out of the service layer (NAG-01 Rule 2).
 *
 * @param pageviews        total pageviews in the period
 * @param visitors         unique visitors in the period
 * @param visits           distinct visits/sessions in the period
 * @param bounces          single-page visits
 * @param totalTimeSeconds total time on site in seconds
 * @param comparison       previous-period comparison, or null when not requested
 */
public record AnalyticsSnapshot(
        long pageviews,
        long visitors,
        long visits,
        long bounces,
        long totalTimeSeconds,
        Comparison comparison) {

    public record Comparison(
            long pageviews,
            long visitors,
            long visits,
            long bounces,
            long totalTimeSeconds) {
    }
}
