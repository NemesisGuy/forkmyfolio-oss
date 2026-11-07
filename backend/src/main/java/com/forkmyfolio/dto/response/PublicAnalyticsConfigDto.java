package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.AnalyticsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The public, non-credential tracking configuration for a portfolio slug,
 * consumed by the Nuxt public pages to inject the tracking script.
 * Everything in this payload is public by design (the same values are visible
 * in the rendered page's script tag).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public tracking configuration for a portfolio slug. Contains no credentials.")
public class PublicAnalyticsConfigDto {

    @Schema(description = "The analytics provider this portfolio tracks against (drives the injection pattern).")
    private AnalyticsProviderType provider;

    @Schema(description = "Whether tracking is enabled for this portfolio.")
    private boolean enabled;

    @Schema(description = "Full URL of the tracking script, e.g. https://metrics.example.com/script.js")
    private String scriptUrl;

    @Schema(description = "Public website/measurement ID the pageview is attributed to.")
    private String websiteId;

    @Schema(description = "Whether the session recorder script may be loaded on this portfolio (experimental, off by default).")
    private boolean recorderEnabled;

    @Schema(description = "Full URL of the recorder script, when recorderEnabled is true.")
    private String recorderUrl;
}
