package com.forkmyfolio.model;

import com.forkmyfolio.model.enums.AnalyticsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Platform-wide (singleton) analytics provider configuration.
 *
 * <p>Deliberately a separate table from {@link Setting}: {@code GET /api/v1/settings}
 * is publicly readable and returns every settings row, so it must never hold
 * credentials. The Umami API key in this table is encrypted at rest and is only
 * ever surfaced through admin-scoped endpoints as a masked hint.</p>
 */
@Entity
@Table(name = "analytics_config")
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Platform-wide analytics provider configuration (admin only).")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class AnalyticsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "The internal database identifier.", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    @Schema(description = "The external unique identifier (UUID) for API usage.", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @Schema(description = "The analytics provider used for platform-level tracking and reporting.")
    private AnalyticsProviderType provider = AnalyticsProviderType.NONE;

    @Column(name = "tracking_enabled", nullable = false)
    @Schema(description = "Whether the public tracking script is injected into public portfolio pages.")
    private boolean trackingEnabled = false;

    @Column(name = "reporting_enabled", nullable = false)
    @Schema(description = "Whether server-side reporting (dashboards) may query the provider API.")
    private boolean reportingEnabled = false;

    @Column(name = "umami_base_url", length = 512)
    @Schema(description = "Base URL of the Umami instance, e.g. https://metrics.example.com")
    private String umamiBaseUrl;

    @Column(name = "umami_website_name", length = 255)
    @Schema(description = "Optional display name of the master Umami website (shown in the admin UI).")
    private String umamiWebsiteName;

    @Column(name = "umami_website_id", length = 64)
    @Schema(description = "Public Umami website ID for the master/site-wide website.")
    private String umamiWebsiteId;

    /**
     * Path of the tracking script on the Umami instance. Defaults to Umami's
     * upstream product default; editable through the admin GUI so
     * non-standard deployments (proxies, path prefixes) work without a
     * frontend rebuild. Validated as a strict relative path.
     */
    @Column(name = "umami_script_path", nullable = false, length = 255)
    @Schema(description = "Path of the Umami tracking script on the instance (default /script.js).")
    private String umamiScriptPath = "/script.js";

    @Column(name = "umami_recorder_path", nullable = false, length = 255)
    @Schema(description = "Path of the Umami session recorder script on the instance (default /recorder.js).")
    private String umamiRecorderPath = "/recorder.js";

    @Column(name = "ga4_measurement_id", length = 64)
    @Schema(description = "Google Analytics 4 Measurement ID (G-XXXXXXX) when provider is GA4.")
    private String ga4MeasurementId;

    @Column(name = "recorder_enabled", nullable = false)
    @Schema(description = "Whether the session recorder script may be loaded on public pages (experimental; privacy controls documented in DEF-1).")
    private boolean recorderEnabled = false;

    /**
     * AES-256-GCM encrypted API key: Base64(iv || ciphertext || tag).
     * Never serialized to any API response — DTOs expose only {@code last4}.
     */
    @Column(name = "umami_api_key_cipher", columnDefinition = "TEXT")
    @Schema(hidden = true)
    private String umamiApiKeyCipher;

    @Column(name = "umami_api_key_last4", length = 4)
    @Schema(description = "Last 4 characters of the stored API key, for admin verification only.", accessMode = Schema.AccessMode.READ_ONLY)
    private String umamiApiKeyLast4;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
