package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.AnalyticsOwnerMode;
import com.forkmyfolio.model.enums.AnalyticsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Owner view of their analytics configuration. Contains no credentials.
 * The effective* fields show the server-resolved outcome (inherit / own / disabled)
 * so the UI can display what actually applies without doing its own resolution.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "The authenticated owner's analytics configuration. Contains no credentials.")
public class UserAnalyticsConfigDto {

    private UUID uuid;

    @Schema(description = "INHERIT: platform config. OWN: this row's config. DISABLED: analytics off.")
    private AnalyticsOwnerMode mode;

    @Schema(description = "Provider used when mode is OWN.")
    private AnalyticsProviderType provider;

    private boolean trackingEnabled;
    private boolean reportingEnabled;

    @Schema(description = "Owner's own Umami base URL (mode OWN).")
    private String umamiBaseUrl;

    @Schema(description = "Optional display name of the owner's Umami website.")
    private String umamiWebsiteName;

    @Schema(description = "Owner's own Umami website ID (mode OWN).")
    private String umamiWebsiteId;

    @Schema(description = "Path of the Umami tracking script on the owner's instance.")
    private String umamiScriptPath;

    @Schema(description = "Path of the Umami session recorder script on the owner's instance.")
    private String umamiRecorderPath;

    @Schema(description = "Owner's own GA4 Measurement ID (G-XXXXXXX) when provider is GA4.")
    private String ga4MeasurementId;

    @Schema(description = "Whether the session recorder script may be loaded on this owner's public pages (mode OWN).")
    private boolean recorderEnabled;

    @Schema(description = "Last 4 characters of the stored API key, or null when none is stored.")
    private String apiKeyLast4;

    @Schema(description = "True when an API key is stored server-side for this owner.")
    private boolean apiKeyConfigured;

    @Schema(description = "Server-resolved provider actually in effect for this owner.")
    private AnalyticsProviderType effectiveProvider;

    @Schema(description = "Server-resolved tracking outcome for this owner's public pages.")
    private boolean effectiveTrackingEnabled;

    @Schema(description = "Server-resolved reporting outcome for this owner's dashboard.")
    private boolean effectiveReportingEnabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
