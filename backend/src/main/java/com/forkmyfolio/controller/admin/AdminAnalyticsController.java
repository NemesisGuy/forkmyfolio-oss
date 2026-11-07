package com.forkmyfolio.controller.admin;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.AnalyticsConfigDto;
import com.forkmyfolio.dto.response.AnalyticsConnectionTestDto;
import com.forkmyfolio.dto.response.AnalyticsMetricRowDto;
import com.forkmyfolio.dto.response.AnalyticsSummaryDto;
import com.forkmyfolio.dto.response.AnalyticsTimeSeriesDto;
import com.forkmyfolio.dto.update.UpdateAnalyticsConfigRequest;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.AnalyticsConfigService;
import com.forkmyfolio.service.AnalyticsReportingService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.analytics.AnalyticsMetricType;
import com.forkmyfolio.service.analytics.AnalyticsQuery;
import com.forkmyfolio.service.servicemodel.AnalyticsConfigData;
import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.service.servicemodel.AnalyticsTimeSeriesData;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import com.forkmyfolio.service.servicemodel.UpdateAnalyticsCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin analytics endpoints (DEF-1): platform configuration management and
 * master/site-wide reporting. Reporting always queries the PLATFORM master
 * website — the scope is fixed by this controller's authorization, never by
 * request parameters. Controllers stay thin (NAG-01): all logic lives in the
 * analytics services; this boundary only maps request DTOs to service
 * commands and service domain records to response DTOs.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@Tag(name = "Admin Analytics", description = "Platform-wide analytics configuration and master reporting (admin only).")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsConfigService analyticsConfigService;
    private final AnalyticsReportingService analyticsReportingService;
    private final UserService userService;

    // --- Configuration ---

    @GetMapping("/config")
    @Operation(summary = "Get the platform analytics configuration (no credentials in the response)")
    public ResponseEntity<ApiResponseWrapper<AnalyticsConfigDto>> getConfig() {
        return ResponseEntity.ok(new ApiResponseWrapper<>(toDto(analyticsConfigService.getGlobalConfig())));
    }

    @PutMapping("/config")
    @Operation(summary = "Update the platform analytics configuration")
    public ResponseEntity<ApiResponseWrapper<AnalyticsConfigDto>> updateConfig(
            @Valid @RequestBody UpdateAnalyticsConfigRequest request) {
        UpdateAnalyticsCommand command = new UpdateAnalyticsCommand(
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
        return ResponseEntity.ok(new ApiResponseWrapper<>(toDto(analyticsConfigService.updateGlobalConfig(command))));
    }

    @PostMapping("/config/test")
    @Operation(summary = "Test the connection to the configured analytics provider")
    public ResponseEntity<ApiResponseWrapper<AnalyticsConnectionTestDto>> testConnection() {
        return ResponseEntity.ok(new ApiResponseWrapper<>(toDto(analyticsConfigService.testGlobalConnection())));
    }

    // --- Master (site-wide) reporting ---

    @GetMapping("/summary")
    @Operation(summary = "Master site-wide analytics summary")
    public ResponseEntity<ApiResponseWrapper<AnalyticsSummaryDto>> getSummary(
            @RequestParam(required = false) Long startAt,
            @RequestParam(required = false) Long endAt,
            @RequestParam(required = false) String period) {
        AnalyticsQuery query = AnalyticsQuery.of(startAt, endAt, period);
        User admin = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                toDto(analyticsReportingService.getSummary(admin, AnalyticsReportingService.Scope.PLATFORM, query))));
    }

    @GetMapping("/timeseries")
    @Operation(summary = "Master site-wide pageviews/sessions over time")
    public ResponseEntity<ApiResponseWrapper<AnalyticsTimeSeriesDto>> getTimeSeries(
            @RequestParam(required = false) Long startAt,
            @RequestParam(required = false) Long endAt,
            @RequestParam(required = false) String period) {
        AnalyticsQuery query = AnalyticsQuery.of(startAt, endAt, period);
        User admin = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                toDto(analyticsReportingService.getTimeSeries(admin, AnalyticsReportingService.Scope.PLATFORM, query))));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Master site-wide ranked metrics (pages, referrers, countries, browsers, devices, events)")
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
        User admin = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                toDto(analyticsReportingService.getMetrics(admin, AnalyticsReportingService.Scope.PLATFORM, query, metricType, limit))));
    }

    // --- DTO mapping (controller boundary; NAG-01) ---

    private static AnalyticsConfigDto toDto(AnalyticsConfigData d) {
        return new AnalyticsConfigDto(
                parseUuid(d.uuid()),
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
                d.credentialStorageAvailable(),
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
