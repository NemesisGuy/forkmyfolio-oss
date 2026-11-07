package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.User;
import com.forkmyfolio.service.AnalyticsConfigService;
import com.forkmyfolio.service.AnalyticsReportingService;
import com.forkmyfolio.exception.AnalyticsUnavailableException;
import com.forkmyfolio.service.UserAnalyticsConfigService;
import com.forkmyfolio.service.analytics.AnalyticsMetricType;
import com.forkmyfolio.service.analytics.AnalyticsProvider;
import com.forkmyfolio.service.analytics.AnalyticsProviderRegistry;
import com.forkmyfolio.service.analytics.AnalyticsQuery;
import com.forkmyfolio.service.analytics.ResolvedAnalyticsConfig;
import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.service.servicemodel.AnalyticsTimeSeriesData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analytics reporting with server-side scoping (DEF-1).
 *
 * <p>Which Umami website is queried is decided exclusively by the caller's
 * authorization scope (admin → platform master website, owner → their own
 * effective configuration). No request parameter can redirect a query to
 * another tenant's website. Provider responses are cached briefly in memory
 * so dashboards poll rather than hammering the Umami API, and provider
 * failures surface as {@link AnalyticsUnavailableException} (HTTP 503) so
 * dashboards degrade gracefully.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsReportingServiceImpl implements AnalyticsReportingService {

    private final AnalyticsConfigService analyticsConfigService;
    private final UserAnalyticsConfigService userAnalyticsConfigService;
    private final AnalyticsProviderRegistry providerRegistry;

    /** Small bounded TTL cache: key → (expiry, value). */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final int MAX_CACHE_ENTRIES = 1_000;

    @Override
    public AnalyticsSnapshot getSummary(User user, Scope scope, AnalyticsQuery query) {
        return cached(cacheKey(scope, user, "summary", query), AnalyticsSnapshot.class,
                () -> provider(user, scope).fetchSummary(config(user, scope), query));
    }

    @Override
    public AnalyticsTimeSeriesData getTimeSeries(User user, Scope scope, AnalyticsQuery query) {
        return cached(cacheKey(scope, user, "timeseries", query), AnalyticsTimeSeriesData.class,
                () -> provider(user, scope).fetchTimeSeries(config(user, scope), query));
    }

    @Override
    public List<AnalyticsMetricEntry> getMetrics(User user, Scope scope, AnalyticsQuery query,
                                                 AnalyticsMetricType metricType, int limit) {
        return cached(cacheKey(scope, user, "metrics:" + metricType + ":" + limit, query), List.class,
                () -> provider(user, scope).fetchMetrics(config(user, scope), query, metricType, limit));
    }

    // --- helpers ---

    private AnalyticsProvider provider(User user, Scope scope) {
        return providerRegistry.get(config(user, scope).provider());
    }

    private ResolvedAnalyticsConfig config(User user, Scope scope) {
        // Scope → whose configuration is used. There is deliberately no path
        // from client input to a website ID here.
        return scope == Scope.PLATFORM
                ? analyticsConfigService.resolvePlatformReportingConfig()
                : userAnalyticsConfigService.resolveEffectiveReportingConfig(user);
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Class<T> type, java.util.function.Supplier<T> loader) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expiresAt > now) {
            return (T) entry.value;
        }
        T value = loader.get();
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(key, new CacheEntry(now + CACHE_TTL_MS, value));
        return value;
    }

    private String cacheKey(Scope scope, User user, String kind, AnalyticsQuery query) {
        // PLATFORM scope is shared across admins; OWNER scope is per user.
        String subject = scope == Scope.PLATFORM ? "platform" : "user:" + user.getId();
        return subject + "|" + kind + "|" + query.resolveStartAt() + "|" + query.resolveEndAt();
    }

    private record CacheEntry(long expiresAt, Object value) {
    }
}
