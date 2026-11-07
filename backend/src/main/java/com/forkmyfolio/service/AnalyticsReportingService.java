package com.forkmyfolio.service;

import com.forkmyfolio.exception.AnalyticsUnavailableException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.analytics.AnalyticsMetricType;
import com.forkmyfolio.service.analytics.AnalyticsQuery;
import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.service.servicemodel.AnalyticsTimeSeriesData;

import java.util.List;

/**
 * Analytics reporting with server-side scoping, caching and graceful
 * degradation (DEF-1).
 *
 * <p>{@link Scope#PLATFORM} is admin-only (enforced at the controller) and
 * always reports on the master website. {@link Scope#OWNER} reports on the
 * authenticated owner's effective configuration. A caller can never influence
 * which website is queried — only the scope selects it, and the scope is
 * derived from the endpoint's authorization, never from request parameters.</p>
 *
 * <p>Methods return service-layer domain records ({@code servicemodel}), not
 * API DTOs — the controller boundary performs the DTO mapping (NAG-01
 * Rule 2). Provider failures surface as {@link AnalyticsUnavailableException}
 * (HTTP 503) so dashboards degrade gracefully.</p>
 */
public interface AnalyticsReportingService {

    enum Scope { PLATFORM, OWNER }

    AnalyticsSnapshot getSummary(User user, Scope scope, AnalyticsQuery query);

    AnalyticsTimeSeriesData getTimeSeries(User user, Scope scope, AnalyticsQuery query);

    List<AnalyticsMetricEntry> getMetrics(User user, Scope scope, AnalyticsQuery query,
                                          AnalyticsMetricType metricType, int limit);
}
