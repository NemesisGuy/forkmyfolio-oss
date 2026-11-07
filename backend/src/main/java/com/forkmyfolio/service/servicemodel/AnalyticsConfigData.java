package com.forkmyfolio.service.servicemodel;

import com.forkmyfolio.model.enums.AnalyticsProviderType;

import java.time.LocalDateTime;

/**
 * Flattened, credential-free view of the platform analytics configuration
 * (DEF-1). Carries every value the API response needs so the controller can
 * build its DTO without touching the JPA entity (NAG-01: no entity calls in
 * controllers, no DTOs in services).
 *
 * @param uuid                       row UUID
 * @param provider                   active provider
 * @param trackingEnabled            tracking script injected on public pages
 * @param reportingEnabled           server-side dashboards enabled
 * @param umamiBaseUrl               Umami instance base URL (public value)
 * @param umamiWebsiteName           optional display name
 * @param umamiWebsiteId             master Umami website ID (public value)
 * @param umamiScriptPath            tracking script path on the instance
 * @param umamiRecorderPath          recorder script path on the instance
 * @param ga4MeasurementId           GA4 Measurement ID when provider is GA4
 * @param recorderEnabled            session recorder allowed on public pages
 * @param apiKeyLast4                last 4 characters of the stored API key
 * @param apiKeyConfigured           whether an API key is stored
 * @param credentialStorageAvailable whether AES credential storage is usable
 * @param createdAt                  row creation timestamp
 * @param updatedAt                  row last-update timestamp
 */
public record AnalyticsConfigData(
        String uuid,
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
        boolean credentialStorageAvailable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
