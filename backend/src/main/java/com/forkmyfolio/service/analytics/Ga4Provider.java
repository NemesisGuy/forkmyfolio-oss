package com.forkmyfolio.service.analytics;

import com.forkmyfolio.exception.AnalyticsProviderException;
import com.forkmyfolio.model.enums.AnalyticsProviderType;
import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.service.servicemodel.AnalyticsTimeSeriesData;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Google Analytics 4 implementation of {@link AnalyticsProvider}.
 *
 * <p><strong>Scope (Phase 8.5, DEF-1):</strong> GA4 is supported at the
 * <em>tracking</em> level — an admin or owner can configure a Measurement ID
 * (public value, {@code G-XXXXXXX}) and the public pages inject the standard
 * gtag.js snippet. GA4 <em>reporting</em> (the Google Analytics Data API)
 * requires a service-account credential plus an OAuth-signing dependency and
 * a fundamentally different data model; it is explicitly deferred. Reporting
 * calls fail with a clear {@link AnalyticsProviderException} (HTTP 503), never
 * a 500, so a GA4-configured deployment degrades gracefully.</p>
 *
 * <p>A future GA4 reporting adapter would implement {@code fetchSummary},
 * {@code fetchTimeSeries} and {@code fetchMetrics} against the Data API using
 * the same {@link ResolvedAnalyticsConfig} contract (its service-account key
 * would travel through the same encrypted-secret path as the Umami API key).</p>
 */
@Component
public class Ga4Provider implements AnalyticsProvider {

    @Override
    public AnalyticsProviderType type() {
        return AnalyticsProviderType.GA4;
    }

    @Override
    public AnalyticsSnapshot fetchSummary(ResolvedAnalyticsConfig config, AnalyticsQuery query) {
        throw reportingNotImplemented();
    }

    @Override
    public AnalyticsTimeSeriesData fetchTimeSeries(ResolvedAnalyticsConfig config, AnalyticsQuery query) {
        throw reportingNotImplemented();
    }

    @Override
    public List<AnalyticsMetricEntry> fetchMetrics(ResolvedAnalyticsConfig config, AnalyticsQuery query,
                                                   AnalyticsMetricType metricType, int limit) {
        throw reportingNotImplemented();
    }

    @Override
    public ConnectionTestResult testConnection(ResolvedAnalyticsConfig config) {
        // For GA4 there is no credential to verify server-side (the Measurement
        // ID is public tracking config), so the test validates the identifier
        // format and states the reporting limitation honestly.
        String measurementId = config.websiteId();
        if (measurementId == null || !measurementId.matches("(?i)^G-[A-Z0-9]{4,12}$")) {
            return new ConnectionTestResult(false,
                    "The configured GA4 Measurement ID does not look like G-XXXXXXXXXX.", null);
        }
        return new ConnectionTestResult(true,
                "GA4 Measurement ID format is valid. Tracking will be injected on public pages. "
                        + "Note: GA4 reporting dashboards are not implemented yet.", 200);
    }

    private static AnalyticsProviderException reportingNotImplemented() {
        return new AnalyticsProviderException(
                "GA4 reporting dashboards are not implemented yet. Tracking still works; "
                        + "switch the provider to Umami for server-side dashboards.");
    }
}
