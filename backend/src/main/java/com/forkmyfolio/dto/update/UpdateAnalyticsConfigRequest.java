package com.forkmyfolio.dto.update;

import com.forkmyfolio.model.enums.AnalyticsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin update of the platform-wide analytics configuration.
 *
 * <p>Credential semantics: a {@code null} apiKey keeps the stored key unchanged;
 * an empty string clears it; a non-empty value replaces it. The key is never
 * returned by any read endpoint.</p>
 */
@Getter
@Setter
@Schema(description = "Update payload for the platform-wide analytics configuration.")
public class UpdateAnalyticsConfigRequest {

    @Schema(description = "Provider to activate. NONE disables analytics entirely.")
    private AnalyticsProviderType provider;

    @Schema(description = "Inject the tracking script into public pages.")
    private Boolean trackingEnabled;

    @Schema(description = "Allow server-side reporting queries against the provider API.")
    private Boolean reportingEnabled;

    @Size(max = 512, message = "Base URL must be at most 512 characters")
    @Schema(description = "Umami instance base URL (validated server-side against SSRF rules).")
    private String umamiBaseUrl;

    @Size(max = 255, message = "Website name must be at most 255 characters")
    @Schema(description = "Optional display name of the master Umami website.")
    private String umamiWebsiteName;

    @Size(max = 64, message = "Website ID must be at most 64 characters")
    @Schema(description = "Master Umami website ID (public value).")
    private String umamiWebsiteId;

    @Size(max = 255, message = "Script path must be at most 255 characters")
    @Schema(description = "Path of the Umami tracking script (validated as a strict relative path).")
    private String umamiScriptPath;

    @Size(max = 255, message = "Recorder path must be at most 255 characters")
    @Schema(description = "Path of the Umami session recorder script (validated as a strict relative path).")
    private String umamiRecorderPath;

    @Size(max = 64, message = "Measurement ID must be at most 64 characters")
    @Schema(description = "GA4 Measurement ID (G-XXXXXXX) when provider is GA4.")
    private String ga4MeasurementId;

    @Schema(description = "Load the session recorder script on public pages (experimental; privacy controls apply).")
    private Boolean recorderEnabled;

    @Schema(description = "Write-only API key. null keeps the stored key, \"\" clears it, a value replaces it.")
    private String umamiApiKey;
}
