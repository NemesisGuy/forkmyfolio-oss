package com.forkmyfolio.controller.management;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.AnalyticsConnectionTestDto;
import com.forkmyfolio.dto.response.AnalyticsMetricRowDto;
import com.forkmyfolio.dto.response.AnalyticsSummaryDto;
import com.forkmyfolio.dto.response.AnalyticsTimeSeriesDto;
import com.forkmyfolio.dto.response.UserAnalyticsConfigDto;
import com.forkmyfolio.dto.update.UpdateUserAnalyticsConfigRequest;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.AnalyticsReportingService;
import com.forkmyfolio.service.UserAnalyticsConfigService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.analytics.AnalyticsMetricType;
import com.forkmyfolio.service.analytics.AnalyticsQuery;
import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.service.servicemodel.AnalyticsTimeSeriesData;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import com.forkmyfolio.service.servicemodel.UpdateUserAnalyticsCommand;
import com.forkmyfolio.service.servicemodel.UserAnalyticsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Owner analytics endpoints (DEF-1): the authenticated owner's configuration
 * and their own portfolio's analytics.
 *
 * <p>Tenant isolation: the owner is resolved from the authenticated principal
 * (never from a request parameter) and the queried website is resolved
 * server-side from that owner's stored configuration. Forged site IDs, slugs
 * or user IDs in the request cannot redirect a query to another tenant's data.</p>
 *
 * <p>Controllers stay thin (NAG-01): this boundary only maps request DTOs to
 * service commands and service domain records to response DTOs.</p>
 */
@RestController
@RequestMapping("/api/v1/me/analytics")
@Tag(name = "My Analytics", description = "The authenticated owner's analytics configuration and portfolio reporting.")
@RequiredArgsConstructor
public class MyAnalyticsController {

    private final UserAnalyticsConfigService userAnalyticsConfigService;
    private final AnalyticsReportingService analyticsReportingService;
    private final UserService userService;

    // --- Configuration ---

    @GetMapping("/config")
    @Operation(summary = "Get my analytics configuration (no credentials in the response)")
    public ResponseEntity<ApiResponseWrapper<UserAnalyticsConfigDto>> getMyConfig() {
        User user = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(toDto(userAnalyticsConfigService.getMyConfig(user))));
    }

    @PutMapping("/config")
    @Operation(summary = "Update my analytics configuration")
    public ResponseEntity<ApiResponseWrapper<UserAnalyticsConfigDto>> updateMyConfig(
            @Valid @RequestBody UpdateUserAnalyticsConfigRequest request) {
        User user = userService.getCurrentAuthenticatedUser();
        UpdateUserAnalyticsCommand command = new UpdateUserAnalyticsCommand(
                request.getMode(),
                request.getProvider(),
                request.getTrackingEnabled(),
                request.getReportingEnabled(),
                request.getUmamiBaseUrl(),
                request.getUmamiWebsiteName(),
                request.getUmamiWebsiteId(),
                request.getUmamiScriptPath(),
                request.getUmamiRecorderPath(),
                request.getGa4MeasurementId(),
                request.getRecorderEnabled(),
                request.getUmamiApiKey());
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                toDto(userAnalyticsConfigService.updateMyConfig(user, command))));
    }

    @PostMapping("/config/test")
    @Operation(summary = "Test the connection for my effective analytics configuration")
    public ResponseEntity<ApiResponseWrapper<AnalyticsConnectionTestDto>> testMyConnection() {
        User user = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(toDto(userAnalyticsConfigService.testMyConnection(user))));
    }

    // --- Own-portfolio reporting ---

    @GetMapping("/summary")
    @Operation(summary = "Analytics summary for my own public portfolio")
    public ResponseEntity<ApiResponseWrapper<AnalyticsSummaryDto>> getSummary(
            @RequestParam(required = false) Long startAt,
            @RequestParam(required = false) Long endAt,
            @RequestParam(required = false) String period) {
        AnalyticsQuery query = AnalyticsQuery.of(startAt, endAt, period);
        User user = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                toDto(analyticsReportingService.getSummary(user, AnalyticsReportingService.Scope.OWNER, query))));
    }

    @GetMapping("/timeseries")
    @Operation(summary = "Pageviews/sessions over time for my own public portfolio")
    public ResponseEntity<ApiResponseWrapper<AnalyticsTimeSeriesDto>> getTimeSeries(
            @RequestParam(required = false) Long startAt,
            @RequestParam(required = false) Long endAt,
            @RequestParam(required = false) String period) {
        AnalyticsQuery query = AnalyticsQuery.of(startAt, endAt, period);
        User user = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                toDto(analyticsReportingService.getTimeSeries(user, AnalyticsReportingService.Scope.OWNER, query))));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Ranked metrics for my own public portfolio (pages, referrers, countries, browsers, devices, events)")
    public ResponseEntity<ApiResponseWrapper<List<AnalyticsMetricRowDto>>> getMetrics(
            @RequestParam String type,
            @RequestParam(required = false) Long startAt,
            @RequestParam(required = false) Long endAt,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "10") int limit) {
        AnalyticsMetricType metricType = AnalyticsMetricType.fromString(type);
        if (metricType == null) {
            throw new IllegalArgumentException("Unsupported metric type. Allowed: path, referrer, country, browser, os, device, event, title.");
        }
        AnalyticsQuery query = AnalyticsQuery.of(startAt, endAt, period);
        User user = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                toDto(analyticsReportingService.getMetrics(user, AnalyticsReportingService.Scope.OWNER, query, metricType, limit))));
    }

    // --- DTO mapping (controller boundary; NAG-01) ---

    private static UserAnalyticsConfigDto toDto(UserAnalyticsData d) {
        return new UserAnalyticsConfigDto(
                parseUuid(d.uuid()),
                d.mode(),
                d.provider(),
                d.trackingEnabled(),
                d.reportingEnabled(),
                d.umamiBaseUrl(),
                d.umamiWebsiteName(),
                d.umamiWebsiteId(),
                d.umamiScriptPath(),
                d.umamiRecorderPath(),
                d.ga4MeasurementId(),
                d.recorderEnabled(),
                d.apiKeyLast4(),
                d.apiKeyConfigured(),
                d.effectiveProvider(),
                d.effectiveTrackingEnabled(),
                d.effectiveReportingEnabled(),
                d.createdAt(),
                d.updatedAt());
    }

    private static AnalyticsConnectionTestDto toDto(ConnectionTestResult r) {
        return new AnalyticsConnectionTestDto(r.success(), r.message(), r.providerStatus());
    }

    private static AnalyticsSummaryDto toDto(AnalyticsSnapshot s) {
        AnalyticsSummaryDto.Comparison comparison = s.comparison() == null ? null
                : new AnalyticsSummaryDto.Comparison(
                s.comparison().pageviews(),
                s.comparison().visitors(),
                s.comparison().visits(),
                s.comparison().bounces(),
                s.comparison().totalTimeSeconds());
        return new AnalyticsSummaryDto(
                s.pageviews(), s.visitors(), s.visits(), s.bounces(), s.totalTimeSeconds(), comparison);
    }

    private static AnalyticsTimeSeriesDto toDto(AnalyticsTimeSeriesData d) {
        return new AnalyticsTimeSeriesDto(
                d.pageviews().stream()
                        .map(p -> new AnalyticsTimeSeriesDto.Point(p.timestamp(), p.value()))
                        .toList(),
                d.sessions().stream()
                        .map(p -> new AnalyticsTimeSeriesDto.Point(p.timestamp(), p.value()))
                        .toList());
    }

    private static List<AnalyticsMetricRowDto> toDto(List<AnalyticsMetricEntry> entries) {
        return entries.stream()
                .map(e -> new AnalyticsMetricRowDto(e.name(), e.value()))
                .toList();
    }

    private static UUID parseUuid(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }
}
