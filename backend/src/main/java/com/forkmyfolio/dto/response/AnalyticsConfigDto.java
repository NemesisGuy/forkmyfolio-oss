package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.AnalyticsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin view of the platform-wide analytics configuration.
 * The API key is never returned — only a masked hint and a configured flag.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Platform-wide analytics configuration (admin only). Contains no credentials.")
public class AnalyticsConfigDto {

    private UUID uuid;

    @Schema(description = "Active analytics provider.")
    private AnalyticsProviderType provider;

    @Schema(description = "Whether the tracking script is injected into public pages.")
    private boolean trackingEnabled;

    @Schema(description = "Whether server-side reporting may query the provider API.")
    private boolean reportingEnabled;

    @Schema(description = "Umami instance base URL (public value).")
    private String umamiBaseUrl;

    @Schema(description = "Optional display name of the master Umami website.")
    private String umamiWebsiteName;

    @Schema(description = "Master Umami website ID (public value).")
    private String umamiWebsiteId;

    @Schema(description = "Path of the Umami tracking script on the instance.")
    private String umamiScriptPath;

    @Schema(description = "Path of the Umami session recorder script on the instance.")
    private String umamiRecorderPath;

    @Schema(description = "GA4 Measurement ID (G-XXXXXXX) when provider is GA4.")
    private String ga4MeasurementId;

    @Schema(description = "Whether the session recorder script may be loaded on public pages (experimental).")
    private boolean recorderEnabled;

    @Schema(description = "Last 4 characters of the stored API key, or null when none is stored.")
    private String apiKeyLast4;

    @Schema(description = "True when an API key is stored server-side.")
    private boolean apiKeyConfigured;

    @Schema(description = "True when the server has an encryption key and can store credentials.")
    private boolean credentialStorageAvailable;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
