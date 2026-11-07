package com.forkmyfolio.service.analytics;

import com.forkmyfolio.exception.AnalyticsProviderException;
import com.forkmyfolio.model.enums.AnalyticsProviderType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Registry of {@link AnalyticsProvider} implementations, resolved lazily from
 * the Spring context. Keeps provider selection in one place so controllers and
 * services never depend on a concrete provider (GA4 would simply register here
 * later).
 *
 * <p>Resolution is deliberately lazy (per call, via {@link ObjectProvider})
 * rather than constructor-cached: provider beans may be replaced by test
 * doubles whose {@code type()} is only stubbed after context startup, and a
 * constructor-time snapshot would neither see nor map them.</p>
 */
@Component
public class AnalyticsProviderRegistry {

    private final ObjectProvider<AnalyticsProvider> providers;

    public AnalyticsProviderRegistry(ObjectProvider<AnalyticsProvider> providers) {
        this.providers = providers;
    }

    public AnalyticsProvider get(AnalyticsProviderType type) {
        return providers.stream()
                .filter(provider -> provider.type() == type)
                .findFirst()
                .orElseThrow(() -> new AnalyticsProviderException("No analytics provider registered for " + type));
    }
}
