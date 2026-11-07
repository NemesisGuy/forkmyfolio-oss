package com.forkmyfolio.service.servicemodel;

import com.forkmyfolio.model.enums.AnalyticsProviderType;

/**
 * Command object for updating the platform analytics configuration (DEF-1).
 * Carries the controller-boundary request into the service layer without
 * exposing DTOs to services (NAG-01 Rule 2). Null means "leave unchanged"
 * per field; the empty-string clearing semantics are documented on the
 * request DTO and preserved here.
 *
 * @param provider          provider to activate (null keeps current)
 * @param trackingEnabled   tracking toggle (null keeps current)
 * @param reportingEnabled  reporting toggle (null keeps current)
 * @param umamiBaseUrl      Umami base URL (null keeps, "" clears)
 * @param umamiWebsiteName  display name (null keeps, "" clears)
 * @param umamiWebsiteId    master website ID (null keeps, "" clears)
 * @param umamiScriptPath   tracking script path (null keeps, "" restores default)
 * @param umamiRecorderPath recorder script path (null keeps, "" restores default)
 * @param ga4MeasurementId  GA4 Measurement ID (null keeps, "" clears)
 * @param recorderEnabled   recorder toggle (null keeps current)
 * @param umamiApiKey       write-only credential (null keeps, "" clears, value replaces)
 */
public record UpdateAnalyticsCommand(
        AnalyticsProviderType provider,
        Boolean trackingEnabled,
        Boolean reportingEnabled,
        String umamiBaseUrl,
        String umamiWebsiteName,
        String umamiWebsiteId,
        String umamiScriptPath,
        String umamiRecorderPath,
        String ga4MeasurementId,
        Boolean recorderEnabled,
        String umamiApiKey) {
}
