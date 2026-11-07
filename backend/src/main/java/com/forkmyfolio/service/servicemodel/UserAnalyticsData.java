package com.forkmyfolio.service.servicemodel;

import com.forkmyfolio.model.enums.AnalyticsOwnerMode;
import com.forkmyfolio.model.enums.AnalyticsProviderType;

import java.time.LocalDateTime;

/**
 * Flattened, credential-free view of one owner's analytics configuration with
 * its effective resolution (DEF-1). Carries every value the API response
 * needs so the controller can build its DTO without touching the JPA entity
 * (NAG-01: no entity calls in controllers, no DTOs in services).
 *
 * @param uuid                       row UUID (null for the synthesized INHERIT default)
 * @param mode                       INHERIT / OWN / DISABLED
 * @param provider                   provider selected for OWN mode
 * @param trackingEnabled            owner's tracking preference
 * @param reportingEnabled           owner's reporting preference
 * @param umamiBaseUrl               owner's Umami base URL (OWN mode)
 * @param umamiWebsiteName           optional display name (OWN mode)
 * @param umamiWebsiteId             owner's Umami website ID (OWN mode)
 * @param umamiScriptPath            tracking script path (OWN mode)
 * @param umamiRecorderPath          recorder script path (OWN mode)
 * @param ga4MeasurementId           owner's GA4 Measurement ID (OWN mode)
 * @param recorderEnabled            recorder allowed on this owner's pages
 * @param apiKeyLast4                last 4 characters of the stored API key
 * @param apiKeyConfigured           whether an API key is stored
 * @param effectiveProvider          provider actually in effect after resolution
 * @param effectiveTrackingEnabled   whether tracking actually applies
 * @param effectiveReportingEnabled  whether reporting is actually available
 * @param createdAt                  row creation timestamp (null when synthesized)
 * @param updatedAt                  row last-update timestamp (null when synthesized)
 */
public record UserAnalyticsData(
        String uuid,
        AnalyticsOwnerMode mode,
        AnalyticsProviderType provider,
        boolean trackingEnabled,
        boolean reportingEnabled,
        String umamiBaseUrl,
        String umamiWebsiteName,
        String umamiWebsiteId,
        String umamiScriptPath,
        String umamiRecorderPath,
        String ga4MeasurementId,
        boolean recorderEnabled,
        String apiKeyLast4,
        boolean apiKeyConfigured,
        AnalyticsProviderType effectiveProvider,
        boolean effectiveTrackingEnabled,
        boolean effectiveReportingEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
