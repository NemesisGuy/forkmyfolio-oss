package com.forkmyfolio.service.servicemodel;

import com.forkmyfolio.model.enums.AnalyticsProviderType;

/**
 * Public (non-credential) tracking configuration resolved for a portfolio
 * (DEF-1). Everything in this record is public by design — the same values
 * are visible in the rendered page's script tags. Service-layer domain record
 * mapped to the API DTO at the controller boundary (NAG-01 Rule 2).
 *
 * @param provider        which provider applies (drives the injection pattern)
 * @param enabled         whether tracking is enabled for this portfolio
 * @param scriptUrl       full URL of the tracking script
 * @param websiteId       public website/measurement ID
 * @param recorderEnabled whether the session recorder may be loaded
 * @param recorderUrl     full URL of the recorder script, when enabled
 */
public record TrackingSettings(
        AnalyticsProviderType provider,
        boolean enabled,
        String scriptUrl,
        String websiteId,
        boolean recorderEnabled,
        String recorderUrl) {
}
