package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.BadRequestException;
import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserAnalyticsConfig;
import com.forkmyfolio.model.enums.AnalyticsOwnerMode;
import com.forkmyfolio.model.enums.AnalyticsProviderType;
import com.forkmyfolio.repository.UserAnalyticsConfigRepository;
import com.forkmyfolio.crypto.CredentialEncryptionService;
import com.forkmyfolio.exception.AnalyticsUnavailableException;
import com.forkmyfolio.service.AnalyticsConfigService;
import com.forkmyfolio.service.UserAnalyticsConfigService;
import com.forkmyfolio.service.PortfolioProfileService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.analytics.AnalyticsProviderRegistry;
import com.forkmyfolio.service.analytics.AnalyticsUrlValidator;
import com.forkmyfolio.service.analytics.ResolvedAnalyticsConfig;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import com.forkmyfolio.service.servicemodel.TrackingSettings;
import com.forkmyfolio.service.servicemodel.UpdateUserAnalyticsCommand;
import com.forkmyfolio.service.servicemodel.UserAnalyticsData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Per-owner analytics configuration (DEF-1).
 *
 * <p>Isolation invariant: every operation is scoped to the {@link User}
 * instance the controller resolved from the authenticated principal. There is
 * no code path that accepts a user ID, slug or website ID from the client to
 * select whose configuration is read or written — the slug-based public
 * endpoint resolves the owner server-side and exposes only non-credential
 * tracking values.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAnalyticsConfigServiceImpl implements UserAnalyticsConfigService {

    private final UserAnalyticsConfigRepository userAnalyticsConfigRepository;
    private final AnalyticsConfigService analyticsConfigService;
    private final CredentialEncryptionService encryptionService;
    private final AnalyticsProviderRegistry providerRegistry;
    private final AnalyticsUrlValidator urlValidator;
    private final UserService userService;
    private final PortfolioProfileService portfolioProfileService;

    @Override
    @Transactional
    public UserAnalyticsData getMyConfig(User user) {
        UserAnalyticsConfig config = userAnalyticsConfigRepository.findByUserId(user.getId()).orElse(null);
        if (config == null) {
            // Synthesized default (INHERIT) without persisting a row.
            return toData(user, null);
        }
        return toData(user, config);
    }

    @Override
    @Transactional
    public UserAnalyticsData updateMyConfig(User user, UpdateUserAnalyticsCommand command) {
        UserAnalyticsConfig config = userAnalyticsConfigRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserAnalyticsConfig created = new UserAnalyticsConfig(user);
                    return userAnalyticsConfigRepository.save(created);
                });

        if (command.mode() != null) {
            config.setMode(command.mode());
        }
        if (command.provider() != null) {
            config.setProvider(command.provider());
        }
        if (command.trackingEnabled() != null) {
            config.setTrackingEnabled(command.trackingEnabled());
        }
        if (command.reportingEnabled() != null) {
            config.setReportingEnabled(command.reportingEnabled());
        }
        if (command.umamiBaseUrl() != null) {
            config.setUmamiBaseUrl(command.umamiBaseUrl().isBlank()
                    ? null : urlValidator.validate(command.umamiBaseUrl()));
        }
        if (command.umamiWebsiteName() != null) {
            String name = command.umamiWebsiteName().trim();
            config.setUmamiWebsiteName(name.isEmpty() ? null : name);
        }
        if (command.umamiWebsiteId() != null) {
            String websiteId = command.umamiWebsiteId().trim();
            config.setUmamiWebsiteId(websiteId.isEmpty() ? null : websiteId);
        }
        if (command.umamiScriptPath() != null) {
            String path = command.umamiScriptPath().trim();
            config.setUmamiScriptPath(path.isEmpty()
                    ? "/script.js" : urlValidator.validateScriptPath(path));
        }
        if (command.umamiRecorderPath() != null) {
            String path = command.umamiRecorderPath().trim();
            config.setUmamiRecorderPath(path.isEmpty()
                    ? "/recorder.js" : urlValidator.validateScriptPath(path));
        }
        if (command.ga4MeasurementId() != null) {
            String id = command.ga4MeasurementId().trim();
            config.setGa4MeasurementId(id.isEmpty() ? null : urlValidator.validateMeasurementId(id));
        }
        if (command.recorderEnabled() != null) {
            config.setRecorderEnabled(command.recorderEnabled());
        }
        applyCredential(config, command.umamiApiKey());

        validateCompleteness(config);
        UserAnalyticsConfig saved = userAnalyticsConfigRepository.save(config);
        log.info("Analytics configuration updated for user {}: mode={}", user.getEmail(), saved.getMode());
        return toData(user, saved);
    }

    @Override
    @Transactional
    public ConnectionTestResult testMyConnection(User user) {
        ResolvedAnalyticsConfig resolved;
        try {
            resolved = resolveEffectiveReportingConfig(user);
        } catch (AnalyticsUnavailableException e) {
            throw new BadRequestException(e.getMessage());
        }
        return providerRegistry.get(resolved.provider()).testConnection(resolved);
    }

    @Override
    @Transactional
    public ResolvedAnalyticsConfig resolveEffectiveReportingConfig(User user) {
        UserAnalyticsConfig config = userAnalyticsConfigRepository.findByUserId(user.getId()).orElse(null);

        if (config == null || config.getMode() == AnalyticsOwnerMode.INHERIT) {
            return analyticsConfigService.resolvePlatformReportingConfig();
        }
        if (config.getMode() == AnalyticsOwnerMode.DISABLED) {
            throw new AnalyticsUnavailableException("Analytics is disabled for this portfolio.");
        }
        // OWN
        if (config.getProvider() == AnalyticsProviderType.NONE) {
            throw new AnalyticsUnavailableException("The analytics configuration for this portfolio is incomplete.");
        }
        if (config.getProvider() == AnalyticsProviderType.GA4) {
            throw new AnalyticsUnavailableException(
                    "GA4 reporting dashboards are not implemented yet. Tracking still works; "
                            + "switch the provider to Umami for server-side dashboards.");
        }
        if (!config.isReportingEnabled()) {
            throw new AnalyticsUnavailableException("Analytics reporting is disabled for this portfolio.");
        }
        if (isBlank(config.getUmamiBaseUrl()) || isBlank(config.getUmamiWebsiteId())) {
            throw new AnalyticsUnavailableException("The analytics configuration for this portfolio is incomplete.");
        }
        String apiKey = decryptOrNull(config);
        if (isBlank(apiKey)) {
            throw new AnalyticsUnavailableException("Analytics reporting credentials are missing for this portfolio.");
        }
        return new ResolvedAnalyticsConfig(AnalyticsProviderType.UMAMI,
                config.getUmamiBaseUrl(), config.getUmamiWebsiteId(), apiKey,
                ResolvedAnalyticsConfig.Source.OWNER);
    }

    @Override
    @Transactional
    public TrackingSettings getPublicTrackingConfigForSlug(String slug) {
        // Resolve the owner server-side; conceal unknown/private portfolios with 404
        // exactly like the public portfolio endpoint (Phase 7.4 policy).
        User owner = userService.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("User with slug '" + slug + "' not found."));
        if (!portfolioProfileService.getProfileByUser(owner).isPubliclyVisible()) {
            throw new ResourceNotFoundException("User with slug '" + slug + "' not found.");
        }

        UserAnalyticsConfig config = userAnalyticsConfigRepository.findByUserId(owner.getId()).orElse(null);

        // DISABLED (explicit) → tracking off.
        if (config != null && config.getMode() == AnalyticsOwnerMode.DISABLED) {
            return disabledConfig();
        }

        // OWN → the owner's own provider configuration (public values only).
        if (config != null && config.getMode() == AnalyticsOwnerMode.OWN && config.isTrackingEnabled()) {
            TrackingSettings own = buildPublicConfig(config.getProvider(),
                    config.getUmamiBaseUrl(), config.getUmamiWebsiteId(),
                    config.getUmamiScriptPath(), config.getUmamiRecorderPath(),
                    config.getGa4MeasurementId(), config.isRecorderEnabled());
            if (own != null) {
                return own;
            }
            // Incomplete OWN configuration falls through to the platform config.
        }

        // INHERIT (or incomplete OWN) → platform tracking config.
        var platform = analyticsConfigService.getGlobalConfigEntity();
        TrackingSettings inherited = buildPublicConfig(platform.getProvider(),
                platform.getUmamiBaseUrl(), platform.getUmamiWebsiteId(),
                platform.getUmamiScriptPath(), platform.getUmamiRecorderPath(),
                platform.getGa4MeasurementId(), platform.isRecorderEnabled());
        return inherited != null ? inherited : disabledConfig();
    }

    /**
     * Builds the public (non-credential) tracking payload for a provider
     * configuration, or null when the configuration is incomplete/unsupported
     * for tracking. Everything returned here is public by design — the same
     * values are visible in the rendered page's script tags.
     */
    private TrackingSettings buildPublicConfig(AnalyticsProviderType provider,
                                               String umamiBaseUrl, String umamiWebsiteId,
                                               String umamiScriptPath, String umamiRecorderPath,
                                               String ga4MeasurementId, boolean recorderEnabled) {
        if (provider == AnalyticsProviderType.UMAMI
                && !isBlank(umamiBaseUrl) && !isBlank(umamiWebsiteId)
                && urlValidator.isSafe(umamiBaseUrl)
                && urlValidator.isSafeScriptPath(umamiScriptPath)) {
            String baseUrl = stripTrailingSlash(umamiBaseUrl);
            String scriptUrl = baseUrl + umamiScriptPath;
            boolean recorder = recorderEnabled
                    && !isBlank(umamiRecorderPath)
                    && urlValidator.isSafeScriptPath(umamiRecorderPath);
            return new TrackingSettings(AnalyticsProviderType.UMAMI, true,
                    scriptUrl, umamiWebsiteId, recorder,
                    recorder ? baseUrl + umamiRecorderPath : null);
        }
        if (provider == AnalyticsProviderType.GA4 && !isBlank(ga4MeasurementId)) {
            // GA4's gtag.js endpoint is a property of the GA4 product itself.
            return new TrackingSettings(AnalyticsProviderType.GA4, true,
                    "https://www.googletagmanager.com/gtag/js?id=" + ga4MeasurementId,
                    ga4MeasurementId, false, null);
        }
        return null;
    }

    // --- helpers ---

    private void applyCredential(UserAnalyticsConfig config, String apiKey) {
        if (apiKey == null) {
            return;
        }
        if (apiKey.isBlank()) {
            config.setUmamiApiKeyCipher(null);
            config.setUmamiApiKeyLast4(null);
            return;
        }
        if (!encryptionService.isAvailable()) {
            throw new BadRequestException(
                    "Credential storage is not available (ANALYTICS_ENCRYPTION_KEY is not configured on the server).");
        }
        config.setUmamiApiKeyCipher(encryptionService.encrypt(apiKey.trim()));
        String key = apiKey.trim();
        config.setUmamiApiKeyLast4(key.length() <= 4 ? key : key.substring(key.length() - 4));
    }

    private void validateCompleteness(UserAnalyticsConfig config) {
        if (config.getMode() != AnalyticsOwnerMode.OWN) {
            return;
        }
        switch (config.getProvider()) {
            case UMAMI -> {
                if (isBlank(config.getUmamiBaseUrl()) || isBlank(config.getUmamiWebsiteId())) {
                    throw new BadRequestException(
                            "Analytics mode OWN with provider UMAMI requires both the instance base URL and the website ID.");
                }
                if (!urlValidator.isSafe(config.getUmamiBaseUrl())) {
                    throw new BadRequestException("The stored Umami base URL is no longer valid.");
                }
                if (!urlValidator.isSafeScriptPath(config.getUmamiScriptPath())
                        || !urlValidator.isSafeScriptPath(config.getUmamiRecorderPath())) {
                    throw new BadRequestException("The stored Umami script paths are no longer valid.");
                }
            }
            case GA4 -> {
                if (isBlank(config.getGa4MeasurementId())) {
                    throw new BadRequestException("Analytics mode OWN with provider GA4 requires a Measurement ID (G-XXXXXXXXXX).");
                }
            }
            case NONE -> {
                // No requirements.
            }
        }
    }

    private String decryptOrNull(UserAnalyticsConfig config) {
        if (isBlank(config.getUmamiApiKeyCipher())) {
            return null;
        }
        try {
            return encryptionService.decrypt(config.getUmamiApiKeyCipher());
        } catch (Exception e) {
            log.error("Failed to decrypt the analytics credential for user {}: {}",
                    config.getUser().getEmail(), e.getMessage());
            return null;
        }
    }

    private TrackingSettings disabledConfig() {
        return new TrackingSettings(AnalyticsProviderType.NONE, false, null, null, false, null);
    }

    private String stripTrailingSlash(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private UserAnalyticsData toData(User user, UserAnalyticsConfig config) {
        if (config == null) {
            // Effective resolution for the synthesized INHERIT default.
            var platform = analyticsConfigService.getGlobalConfigEntity();
            boolean platformActive = platform.getProvider() != AnalyticsProviderType.NONE;
            return new UserAnalyticsData(null, AnalyticsOwnerMode.INHERIT, AnalyticsProviderType.NONE,
                    false, false, null, null, null, null, null, null, false, null, false,
                    platformActive ? platform.getProvider() : AnalyticsProviderType.NONE,
                    platformActive && platform.isTrackingEnabled(),
                    platformActive && platform.isReportingEnabled(),
                    null, null);
        }
        ResolvedAnalyticsConfig effective;
        try {
            effective = resolveEffectiveReportingConfig(user);
        } catch (AnalyticsUnavailableException e) {
            effective = null;
        }
        return new UserAnalyticsData(
                config.getUuid() == null ? null : config.getUuid().toString(),
                config.getMode(),
                config.getProvider(),
                config.isTrackingEnabled(),
                config.isReportingEnabled(),
                config.getUmamiBaseUrl(),
                config.getUmamiWebsiteName(),
                config.getUmamiWebsiteId(),
                config.getUmamiScriptPath(),
                config.getUmamiRecorderPath(),
                config.getGa4MeasurementId(),
                config.isRecorderEnabled(),
                config.getUmamiApiKeyLast4(),
                !isBlank(config.getUmamiApiKeyCipher()),
                effective == null ? AnalyticsProviderType.NONE : effective.provider(),
                isTrackingEffectivelyEnabled(config),
                effective != null,
                config.getCreatedAt(),
                config.getUpdatedAt());
    }

    /**
     * Tracking resolution mirrors getPublicTrackingConfigForSlug: OWN rows use
     * their own provider config; INHERIT rows follow the platform config.
     */
    private boolean isTrackingEffectivelyEnabled(UserAnalyticsConfig config) {
        if (config.getMode() == AnalyticsOwnerMode.DISABLED) {
            return false;
        }
        if (config.getMode() == AnalyticsOwnerMode.OWN) {
            return switch (config.getProvider()) {
                case UMAMI -> config.isTrackingEnabled()
                        && !isBlank(config.getUmamiBaseUrl())
                        && !isBlank(config.getUmamiWebsiteId());
                case GA4 -> config.isTrackingEnabled() && !isBlank(config.getGa4MeasurementId());
                case NONE -> false;
            };
        }
        var platform = analyticsConfigService.getGlobalConfigEntity();
        return platform.getProvider() != AnalyticsProviderType.NONE && platform.isTrackingEnabled();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
