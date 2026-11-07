package com.forkmyfolio.service.impl;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.exception.BadRequestException;
import com.forkmyfolio.model.AnalyticsConfig;
import com.forkmyfolio.model.enums.AnalyticsProviderType;
import com.forkmyfolio.repository.AnalyticsConfigRepository;
import com.forkmyfolio.crypto.CredentialEncryptionService;
import com.forkmyfolio.service.AnalyticsConfigService;
import com.forkmyfolio.exception.AnalyticsUnavailableException;
import com.forkmyfolio.service.analytics.AnalyticsProviderRegistry;
import com.forkmyfolio.service.analytics.AnalyticsUrlValidator;
import com.forkmyfolio.service.analytics.ResolvedAnalyticsConfig;
import com.forkmyfolio.service.servicemodel.AnalyticsConfigData;
import com.forkmyfolio.service.servicemodel.ConnectionTestResult;
import com.forkmyfolio.service.servicemodel.UpdateAnalyticsCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Platform-wide analytics configuration (DEF-1). Secrets are encrypted at
 * rest and never leave the server: responses expose only a masked hint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConfigServiceImpl implements AnalyticsConfigService {

    private final AnalyticsConfigRepository analyticsConfigRepository;
    private final CredentialEncryptionService encryptionService;
    private final AnalyticsProviderRegistry providerRegistry;
    private final AnalyticsUrlValidator urlValidator;

    @Override
    @Transactional
    public AnalyticsConfigData getGlobalConfig() {
        return toData(getOrCreateSingleton());
    }

    @Override
    @Transactional
    public AnalyticsConfig getGlobalConfigEntity() {
        return getOrCreateSingleton();
    }

    @Override
    @Transactional
    public AnalyticsConfigData updateGlobalConfig(UpdateAnalyticsCommand command) {
        AnalyticsConfig config = getOrCreateSingleton();

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
            // Empty string clears; otherwise validate (SSRF policy) and store.
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
            // Empty restores the Umami upstream default; otherwise strict
            // relative-path validation (anti script-injection).
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
        AnalyticsConfig saved = analyticsConfigRepository.save(config);
        log.info("Analytics platform configuration updated: provider={}, tracking={}, reporting={}",
                saved.getProvider(), saved.isTrackingEnabled(), saved.isReportingEnabled());
        return toData(saved);
    }

    @Override
    @Transactional
    public ConnectionTestResult testGlobalConnection() {
        AnalyticsConfig config = getOrCreateSingleton();
        if (config.getProvider() == AnalyticsProviderType.NONE) {
            throw new BadRequestException("Select an analytics provider before testing the connection.");
        }
        if (config.getProvider() == AnalyticsProviderType.GA4) {
            if (isBlank(config.getGa4MeasurementId())) {
                throw new BadRequestException("Configure the GA4 Measurement ID before testing the connection.");
            }
            ResolvedAnalyticsConfig ga4 = new ResolvedAnalyticsConfig(
                    AnalyticsProviderType.GA4, null, config.getGa4MeasurementId(), null,
                    ResolvedAnalyticsConfig.Source.PLATFORM);
            return providerRegistry.get(AnalyticsProviderType.GA4).testConnection(ga4);
        }
        // UMAMI
        if (isBlank(config.getUmamiBaseUrl()) || isBlank(config.getUmamiWebsiteId())) {
            throw new BadRequestException("Configure the Umami base URL and website ID before testing the connection.");
        }
        if (isBlank(config.getUmamiApiKeyCipher())) {
            return new ConnectionTestResult(false,
                    "No API key is stored. Save an API key first — the Umami reporting API requires it.", null);
        }
        ResolvedAnalyticsConfig resolved = new ResolvedAnalyticsConfig(
                AnalyticsProviderType.UMAMI,
                config.getUmamiBaseUrl(),
                config.getUmamiWebsiteId(),
                decryptOrNull(config),
                ResolvedAnalyticsConfig.Source.PLATFORM);
        return providerRegistry.get(AnalyticsProviderType.UMAMI).testConnection(resolved);
    }

    @Override
    @Transactional
    public ResolvedAnalyticsConfig resolvePlatformReportingConfig() {
        AnalyticsConfig config = getOrCreateSingleton();
        if (config.getProvider() == AnalyticsProviderType.NONE) {
            throw new AnalyticsUnavailableException("Analytics reporting is not configured for this platform.");
        }
        if (!config.isReportingEnabled()) {
            throw new AnalyticsUnavailableException("Analytics reporting is disabled for this platform.");
        }
        if (config.getProvider() == AnalyticsProviderType.GA4) {
            throw new AnalyticsUnavailableException(
                    "GA4 reporting dashboards are not implemented yet. Tracking still works; "
                            + "switch the provider to Umami for server-side dashboards.");
        }
        if (isBlank(config.getUmamiBaseUrl()) || isBlank(config.getUmamiWebsiteId())) {
            throw new AnalyticsUnavailableException("Analytics reporting is not configured for this platform.");
        }
        String apiKey = decryptOrNull(config);
        if (isBlank(apiKey)) {
            throw new AnalyticsUnavailableException("Analytics reporting credentials are missing.");
        }
        return new ResolvedAnalyticsConfig(AnalyticsProviderType.UMAMI,
                config.getUmamiBaseUrl(), config.getUmamiWebsiteId(), apiKey,
                ResolvedAnalyticsConfig.Source.PLATFORM);
    }

    // --- helpers ---

    private AnalyticsConfig getOrCreateSingleton() {
        return analyticsConfigRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            log.info("Creating the singleton analytics configuration row");
            return analyticsConfigRepository.save(new AnalyticsConfig());
        });
    }

    /**
     * Credential semantics: null keeps the stored key, empty clears it,
     * a non-empty value replaces it (encrypted when storage is available).
     */
    private void applyCredential(AnalyticsConfig config, String apiKey) {
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

    /**
     * A provider activation must come with its provider-specific required
     * configuration; this keeps the stored config internally consistent.
     */
    private void validateCompleteness(AnalyticsConfig config) {
        switch (config.getProvider()) {
            case UMAMI -> {
                if (isBlank(config.getUmamiBaseUrl()) || isBlank(config.getUmamiWebsiteId())) {
                    throw new BadRequestException(
                            "Provider UMAMI requires both the instance base URL and the master website ID.");
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
                    throw new BadRequestException("Provider GA4 requires a Measurement ID (G-XXXXXXXXXX).");
                }
            }
            case NONE -> {
                // No requirements.
            }
        }
    }

    private String decryptOrNull(AnalyticsConfig config) {
        if (isBlank(config.getUmamiApiKeyCipher())) {
            return null;
        }
        try {
            return encryptionService.decrypt(config.getUmamiApiKeyCipher());
        } catch (Exception e) {
            log.error("Failed to decrypt the platform analytics credential: {}", e.getMessage());
            return null;
        }
    }

    private AnalyticsConfigData toData(AnalyticsConfig config) {
        return new AnalyticsConfigData(
                config.getUuid() == null ? null : config.getUuid().toString(),
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
                encryptionService.isAvailable(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
