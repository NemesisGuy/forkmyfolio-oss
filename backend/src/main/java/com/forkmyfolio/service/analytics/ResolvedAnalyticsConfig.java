package com.forkmyfolio.service.analytics;

import com.forkmyfolio.model.enums.AnalyticsProviderType;

/**
 * A fully-resolved, credential-bearing analytics configuration ready for a
 * provider call. Produced exclusively server-side by the config services —
 * never from browser input.
 *
 * @param provider    the provider to use
 * @param baseUrl     provider base URL (Umami instance root)
 * @param websiteId   provider website/property identifier
 * @param apiKey      decrypted API credential (nullable for tracking-only use)
 * @param source      whether this came from the platform config or an owner's own
 */
public record ResolvedAnalyticsConfig(
        AnalyticsProviderType provider,
        String baseUrl,
        String websiteId,
        String apiKey,
        Source source) {

    public enum Source { PLATFORM, OWNER }

    public boolean isReportingCapable() {
        return provider == AnalyticsProviderType.UMAMI
                && baseUrl != null && !baseUrl.isBlank()
                && websiteId != null && !websiteId.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }
}
