package com.forkmyfolio.model;

import com.forkmyfolio.model.enums.AnalyticsOwnerMode;
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
 * Per-owner analytics configuration. One row per user at most.
 *
 * <p>Owners may inherit the platform configuration (public tracking attributed
 * to the master website), configure their own Umami instance/website, or
 * disable analytics for their portfolio entirely. Ownership of the configured
 * website is established server-side: reporting endpoints always resolve the
 * website ID from this row for the authenticated user and never trust a
 * browser-supplied site ID.</p>
 */
@Entity
@Table(name = "user_analytics_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "The authenticated user's analytics configuration.")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class UserAnalyticsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "The internal database identifier.", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    @Schema(description = "The external unique identifier (UUID) for API usage.", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(hidden = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    @Schema(description = "INHERIT: use the platform config. OWN: use this row's provider config. DISABLED: no analytics.")
    private AnalyticsOwnerMode mode = AnalyticsOwnerMode.INHERIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @Schema(description = "Provider used when mode is OWN.")
    private AnalyticsProviderType provider = AnalyticsProviderType.NONE;

    @Column(name = "tracking_enabled", nullable = false)
    @Schema(description = "Whether the tracking script is injected on this owner's public pages (mode OWN only).")
    private boolean trackingEnabled = true;

    @Column(name = "reporting_enabled", nullable = false)
    @Schema(description = "Whether this owner's analytics dashboard may query the provider API.")
    private boolean reportingEnabled = true;

    @Column(name = "umami_base_url", length = 512)
    @Schema(description = "Owner's own Umami base URL (mode OWN, provider UMAMI).")
    private String umamiBaseUrl;

    @Column(name = "umami_website_name", length = 255)
    @Schema(description = "Optional display name of the owner's Umami website (shown in the owner UI).")
    private String umamiWebsiteName;

    @Column(name = "umami_website_id", length = 64)
    @Schema(description = "Owner's own Umami website ID (mode OWN, provider UMAMI).")
    private String umamiWebsiteId;

    @Column(name = "umami_script_path", nullable = false, length = 255)
    @Schema(description = "Path of the Umami tracking script on the owner's instance (default /script.js).")
    private String umamiScriptPath = "/script.js";

    @Column(name = "umami_recorder_path", nullable = false, length = 255)
    @Schema(description = "Path of the Umami session recorder script on the owner's instance (default /recorder.js).")
    private String umamiRecorderPath = "/recorder.js";

    @Column(name = "ga4_measurement_id", length = 64)
    @Schema(description = "Owner's own Google Analytics 4 Measurement ID (G-XXXXXXX) when provider is GA4.")
    private String ga4MeasurementId;

    @Column(name = "recorder_enabled", nullable = false)
    @Schema(description = "Whether the session recorder script may be loaded on this owner's public pages (mode OWN only).")
    private boolean recorderEnabled = false;

    @Column(name = "umami_api_key_cipher", columnDefinition = "TEXT")
    @Schema(hidden = true)
    private String umamiApiKeyCipher;

    @Column(name = "umami_api_key_last4", length = 4)
    @Schema(description = "Last 4 characters of the stored API key, for owner verification only.", accessMode = Schema.AccessMode.READ_ONLY)
    private String umamiApiKeyLast4;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserAnalyticsConfig(User user) {
        this.user = user;
    }

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
