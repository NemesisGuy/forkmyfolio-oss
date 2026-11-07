package com.forkmyfolio.service.analytics;

import com.forkmyfolio.exception.AnalyticsProviderException;
import com.forkmyfolio.model.enums.AnalyticsProviderType;
import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsPoint;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.service.servicemodel.AnalyticsTimeSeriesData;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Umami (v3.x) implementation of {@link AnalyticsProvider}.
 *
 * <p>Verified against Umami 3.4.1:
 * authenticated with a self-hosted API key via {@code Authorization: Bearer umami_…};
 * reporting via {@code /api/websites/{id}/stats}, {@code /pageviews} and
 * {@code /metrics?type=…}. All calls are read-only and carry the credential
 * only inside the backend — the credential never reaches any client.</p>
 */
@Component
@Slf4j
public class UmamiProvider implements AnalyticsProvider {
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    private static final int MAX_METRIC_LIMIT = 50;

    /** RestClient instances are immutable and safe to reuse per base URL. */
    private final Map<String, RestClient> clientsByBaseUrl = new ConcurrentHashMap<>();

    @Override
    public AnalyticsProviderType type() {
        return AnalyticsProviderType.UMAMI;
    }

    @Override
    public AnalyticsSnapshot fetchSummary(ResolvedAnalyticsConfig config, AnalyticsQuery query) {
        try {
            UmamiStats stats = client(config).get()
                    .uri(uri -> uri.path("/api/websites/{id}/stats")
                            .queryParam("startAt", query.resolveStartAt())
                            .queryParam("endAt", query.resolveEndAt())
                            .build(config.websiteId()))
                    .header("Authorization", bearer(config))
                    .retrieve()
                    .body(UmamiStats.class);
            if (stats == null) {
                throw new AnalyticsProviderException("Umami returned an empty stats response");
            }
            UmamiStats prev = stats.getComparison();
            AnalyticsSnapshot.Comparison comparison = prev == null ? null
                    : new AnalyticsSnapshot.Comparison(
                    nz(prev.getPageviews()), nz(prev.getVisitors()), nz(prev.getVisits()),
                    nz(prev.getBounces()), nz(prev.getTotaltime()));
            return new AnalyticsSnapshot(
                    nz(stats.getPageviews()), nz(stats.getVisitors()), nz(stats.getVisits()),
                    nz(stats.getBounces()), nz(stats.getTotaltime()), comparison);
        } catch (RestClientResponseException e) {
            throw new AnalyticsProviderException(authAwareMessage("stats", e), e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new AnalyticsProviderException("Could not reach the Umami instance (network error while fetching stats).", e);
        }
    }

    @Override
    public AnalyticsTimeSeriesData fetchTimeSeries(ResolvedAnalyticsConfig config, AnalyticsQuery query) {
        String unit = resolveUnit(query);
        try {
            UmamiPageviews body = client(config).get()
                    .uri(uri -> uri.path("/api/websites/{id}/pageviews")
                            .queryParam("startAt", query.resolveStartAt())
                            .queryParam("endAt", query.resolveEndAt())
                            .queryParam("unit", unit)
                            .build(config.websiteId()))
                    .header("Authorization", bearer(config))
                    .retrieve()
                    .body(UmamiPageviews.class);
            List<AnalyticsPoint> pageviews = new ArrayList<>();
            List<AnalyticsPoint> sessions = new ArrayList<>();
            if (body != null) {
                if (body.getPageviews() != null) {
                    body.getPageviews().forEach(p -> pageviews.add(new AnalyticsPoint(nz(p.getX()), nz(p.getY()))));
                }
                if (body.getSessions() != null) {
                    body.getSessions().forEach(p -> sessions.add(new AnalyticsPoint(nz(p.getX()), nz(p.getY()))));
                }
            }
            return new AnalyticsTimeSeriesData(pageviews, sessions);
        } catch (RestClientResponseException e) {
            throw new AnalyticsProviderException(authAwareMessage("pageviews", e), e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new AnalyticsProviderException("Could not reach the Umami instance (network error while fetching pageviews).", e);
        }
    }

    @Override
    public List<AnalyticsMetricEntry> fetchMetrics(ResolvedAnalyticsConfig config, AnalyticsQuery query,
                                                   AnalyticsMetricType metricType, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_METRIC_LIMIT);
        try {
            UmamiMetricRow[] rows = client(config).get()
                    .uri(uri -> uri.path("/api/websites/{id}/metrics")
                            .queryParam("type", metricType.umamiType())
                            .queryParam("startAt", query.resolveStartAt())
                            .queryParam("endAt", query.resolveEndAt())
                            .queryParam("limit", safeLimit)
                            .build(config.websiteId()))
                    .header("Authorization", bearer(config))
                    .retrieve()
                    .body(UmamiMetricRow[].class);
            List<AnalyticsMetricEntry> result = new ArrayList<>();
            if (rows != null) {
                for (UmamiMetricRow row : rows) {
                    result.add(new AnalyticsMetricEntry(row.getX(), nz(row.getY())));
                }
            }
            return result;
        } catch (RestClientResponseException e) {
            throw new AnalyticsProviderException(authAwareMessage("metrics", e), e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new AnalyticsProviderException("Could not reach the Umami instance (network error while fetching metrics).", e);
        }
    }

    @Override
    public ConnectionTestResult testConnection(ResolvedAnalyticsConfig config) {
        try {
            client(config).get()
                    .uri(uri -> uri.path("/api/websites/{id}").build(config.websiteId()))
                    .header("Authorization", bearer(config))
                    .retrieve()
                    .toBodilessEntity();
            return new ConnectionTestResult(true,
                    "Successfully authenticated to the Umami instance and accessed the configured website.", 200);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            String message = switch (code) {
                case 401 -> "The Umami API key was rejected (401). Check the stored credential.";
                case 403 -> "The Umami API key is not permitted to access this website (403).";
                case 404 -> "The configured website ID was not found on this Umami instance (404).";
                default -> "Umami responded with HTTP " + code + ".";
            };
            return new ConnectionTestResult(false, message, code);
        } catch (RestClientException e) {
            log.warn("Umami connection test failed (transport): {}", e.getMessage());
            return new ConnectionTestResult(false,
                    "Could not reach the Umami instance. Verify the base URL and that the instance is online.", null);
        }
    }

    private RestClient client(ResolvedAnalyticsConfig config) {
        String baseUrl = normalizeBaseUrl(config.baseUrl());
        return clientsByBaseUrl.computeIfAbsent(baseUrl, url -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
            factory.setReadTimeout(READ_TIMEOUT_MS);
            return RestClient.builder()
                    .baseUrl(url)
                    .defaultHeader("Accept", "application/json")
                    .requestFactory(factory)
                    .build();
        });
    }

    /**
     * P10 fix: the Authorization header is now set PER REQUEST (see call
     * sites) instead of baked into the cached RestClient. The previous
     * implementation cached clients per base URL with the credential captured
     * at creation time, which meant (a) replacing an API key had no effect
     * until restart (the stale credential kept failing), and (b) two owners
     * configuring the SAME Umami instance would have shared one client — and
     * therefore ONE owner's credential — a cross-tenant credential leak.
     */
    private static String bearer(ResolvedAnalyticsConfig config) {
        return "Bearer " + config.apiKey();
    }

    /**
     * P10 fix: status-aware failure messages for reporting calls. The
     * dashboard previously showed every HTTP failure as a generic "request
     * failed"/"unreachable" pair, which misdiagnosed an invalid API key as a
     * connectivity outage. Mirrors the testConnection message mapping so the
     * dashboard and the connection test tell the same story.
     */
    private static String authAwareMessage(String operation, RestClientResponseException e) {
        int code = e.getStatusCode().value();
        return switch (code) {
            case 401 -> "The Umami API key was rejected (401) while fetching " + operation
                    + ". Re-save a valid API key in the analytics settings.";
            case 403 -> "The Umami API key is not permitted to access this website (403) while fetching " + operation + ".";
            case 404 -> "The configured website ID was not found on this Umami instance (404) while fetching " + operation + ".";
            case 429 -> "The Umami instance is rate-limiting requests (429) while fetching " + operation + ". Try again shortly.";
            default -> "Umami responded with HTTP " + code + " while fetching " + operation + ".";
        };
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String resolveUnit(AnalyticsQuery query) {
        long span = query.resolveEndAt() - query.resolveStartAt();
        // Hourly buckets for short ranges, daily otherwise.
        return span <= 48L * 60 * 60 * 1000 ? "hour" : "day";
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    // --- Minimal Jackson-bound shapes for the Umami v3 API ---

    @lombok.Data
    static class UmamiStats {
        private Long pageviews;
        private Long visitors;
        private Long visits;
        private Long bounces;
        private Long totaltime;
        private UmamiStats comparison;
    }

    @lombok.Data
    static class UmamiPageviews {
        private List<UmamiPoint> pageviews;
        private List<UmamiPoint> sessions;
    }

    @lombok.Data
    static class UmamiPoint {
        private Long x;
        private Long y;
    }

    @lombok.Data
    static class UmamiMetricRow {
        private String x;
        private Long y;
    }
}
