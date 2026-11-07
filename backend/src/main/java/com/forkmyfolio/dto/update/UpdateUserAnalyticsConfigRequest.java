package com.forkmyfolio.dto.update;

import com.forkmyfolio.model.enums.AnalyticsOwnerMode;
import com.forkmyfolio.model.enums.AnalyticsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Owner update of their personal analytics configuration.
 * Credential semantics match {@link UpdateAnalyticsConfigRequest}: null keeps,
 * empty clears, a value replaces. Owners can only ever affect their own row —
 * the controller resolves the row from the authenticated principal.
 */
@Getter
@Setter
@Schema(description = "Update payload for the authenticated owner's analytics configuration.")
public class UpdateUserAnalyticsConfigRequest {

    @Schema(description = "INHERIT: platform config. OWN: this row's config. DISABLED: analytics off.")
    private AnalyticsOwnerMode mode;

    @Schema(description = "Provider used when mode is OWN.")
    private AnalyticsProviderType provider;

    @Schema(description = "Inject the tracking script into this owner's public pages (mode OWN).")
    private Boolean trackingEnabled;

    @Schema(description = "Allow this owner's dashboard to query the provider API.")
    private Boolean reportingEnabled;

    @Size(max = 512, message = "Base URL must be at most 512 characters")
    @Schema(description = "Owner's own Umami base URL (validated server-side against SSRF rules).")
    private String umamiBaseUrl;

    @Size(max = 255, message = "Website name must be at most 255 characters")
    @Schema(description = "Optional display name of the owner's Umami website.")
    private String umamiWebsiteName;

    @Size(max = 64, message = "Website ID must be at most 64 characters")
    @Schema(description = "Owner's own Umami website ID.")
    private String umamiWebsiteId;

    @Size(max = 255, message = "Script path must be at most 255 characters")
    @Schema(description = "Path of the Umami tracking script (validated as a strict relative path).")
    private String umamiScriptPath;

    @Size(max = 255, message = "Recorder path must be at most 255 characters")
    @Schema(description = "Path of the Umami session recorder script (validated as a strict relative path).")
    private String umamiRecorderPath;

    @Size(max = 64, message = "Measurement ID must be at most 64 characters")
    @Schema(description = "Owner's own GA4 Measurement ID (G-XXXXXXX) when provider is GA4.")
    private String ga4MeasurementId;

    @Schema(description = "Load the session recorder script on this owner's public pages (mode OWN; privacy controls apply).")
    private Boolean recorderEnabled;

    @Schema(description = "Write-only API key. null keeps the stored key, \"\" clears it, a value replaces it.")
    private String umamiApiKey;
}
