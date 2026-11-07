package com.forkmyfolio.service.analytics;

import com.forkmyfolio.exception.AnalyticsProviderException;
import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.service.servicemodel.AnalyticsTimeSeriesData;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;

import java.util.List;

/**
 * Provider-agnostic analytics reporting port (DEF-1).
 *
 * <p>Implementations translate the platform's data model into provider-specific
 * API calls. Umami is the first implementation; a future GA4 adapter would
 * implement the same contract using the Google Analytics Data API (its
 * Measurement ID is public tracking config; its service-account credential is
 * a secret handled exactly like the Umami API key).</p>
 *
 * <p>Methods return service-layer domain records ({@code servicemodel}), not
 * API DTOs — the controller boundary performs the DTO mapping (NAG-01
 * Rule 2). Implementations must be side-effect free (read-only reporting) and
 * must throw {@link AnalyticsProviderException} on transport/auth failures so
 * the reporting service can degrade gracefully.</p>
 */
public interface AnalyticsProvider {

    /** The provider type this implementation serves. */
    com.forkmyfolio.model.enums.AnalyticsProviderType type();

    /**
     * Aggregated summary for a date range (pageviews, visitors, visits,
     * bounces, total time) with a previous-period comparison when supported.
     */
    AnalyticsSnapshot fetchSummary(ResolvedAnalyticsConfig config, AnalyticsQuery query);

    /** Pageview/session counts over time. */
    AnalyticsTimeSeriesData fetchTimeSeries(ResolvedAnalyticsConfig config, AnalyticsQuery query);

    /** A ranked breakdown (top pages, referrers, countries, ...). */
    List<AnalyticsMetricEntry> fetchMetrics(ResolvedAnalyticsConfig config, AnalyticsQuery query,
                                            AnalyticsMetricType metricType, int limit);

    /** Cheap connectivity/credential check against the configured website. */
    ConnectionTestResult testConnection(ResolvedAnalyticsConfig config);
}
