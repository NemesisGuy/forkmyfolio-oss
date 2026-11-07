package com.forkmyfolio.service;

import com.forkmyfolio.exception.AnalyticsUnavailableException;
import com.forkmyfolio.model.AnalyticsConfig;
import com.forkmyfolio.service.analytics.ResolvedAnalyticsConfig;
import com.forkmyfolio.service.servicemodel.AnalyticsConfigData;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import com.forkmyfolio.service.servicemodel.UpdateAnalyticsCommand;

/**
 * Platform-wide analytics configuration management (admin-scoped, DEF-1).
 *
 * <p>Methods return service-layer domain records ({@code servicemodel}), not
 * API DTOs — the controller boundary performs the DTO mapping (NAG-01
 * Rule 2).</p>
 */
public interface AnalyticsConfigService {

    /** Current platform configuration (creates the singleton row lazily). */
    AnalyticsConfigData getGlobalConfig();

    /**
     * Updates the platform configuration. Validates the provider URL (SSRF
     * policy) and credential semantics; returns the sanitized view.
     */
    AnalyticsConfigData updateGlobalConfig(UpdateAnalyticsCommand command);

    /** Tests connectivity to the configured provider using the stored credential. */
    ConnectionTestResult testGlobalConnection();

    /**
     * Resolves the platform reporting configuration (decrypted credential
     * included) for server-side use. Throws {@link AnalyticsUnavailableException}
     * when reporting is not usable (disabled, unconfigured or missing
     * credentials).
     */
    ResolvedAnalyticsConfig resolvePlatformReportingConfig();

    /**
     * Entity accessor for internal service-layer consumers that need the raw
     * row (e.g. per-owner resolution of inherited platform values). Not
     * exposed to controllers.
     */
    AnalyticsConfig getGlobalConfigEntity();
}
