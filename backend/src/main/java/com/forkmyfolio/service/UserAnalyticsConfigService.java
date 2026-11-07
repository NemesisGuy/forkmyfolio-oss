package com.forkmyfolio.service;

import com.forkmyfolio.exception.AnalyticsUnavailableException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.analytics.ResolvedAnalyticsConfig;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import com.forkmyfolio.service.servicemodel.TrackingSettings;
import com.forkmyfolio.service.servicemodel.UpdateUserAnalyticsCommand;
import com.forkmyfolio.service.servicemodel.UserAnalyticsData;

/**
 * Per-owner analytics configuration and effective-resolution (DEF-1).
 *
 * <p>Security invariants:
 * <ul>
 *   <li>Every method is scoped to the {@link User} passed in by the controller
 *       (which resolves it from the authenticated principal) — a caller can
 *       never address another user's configuration.</li>
 *   <li>Website IDs are always resolved server-side from the owning user's
 *       stored configuration; browser-supplied site IDs are never trusted.</li>
 *   <li>Public tracking config exposes only non-credential values.</li>
 * </ul></p>
 *
 * <p>Methods return service-layer domain records ({@code servicemodel}), not
 * API DTOs — the controller boundary performs the DTO mapping (NAG-01
 * Rule 2).</p>
 */
public interface UserAnalyticsConfigService {

    /** The authenticated owner's configuration (synthesized default when no row exists). */
    UserAnalyticsData getMyConfig(User user);

    /** Creates or updates the authenticated owner's configuration row. */
    UserAnalyticsData updateMyConfig(User user, UpdateUserAnalyticsCommand command);

    /** Tests connectivity for the owner's effective reporting configuration. */
    ConnectionTestResult testMyConnection(User user);

    /**
     * Resolves the owner's effective reporting configuration (inherit →
     * platform config, own → their row, disabled → unavailable).
     */
    ResolvedAnalyticsConfig resolveEffectiveReportingConfig(User user);

    /**
     * Resolves the public (non-credential) tracking configuration for a
     * portfolio slug. Returns a disabled config for slugs that are unknown
     * or private (callers may also conceal via 404).
     */
    TrackingSettings getPublicTrackingConfigForSlug(String slug);
}
