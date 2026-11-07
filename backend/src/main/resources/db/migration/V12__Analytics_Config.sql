-- V12 (Phase 8.5): Analytics provider configuration (DEF-1).
--
-- Design notes:
--  * Analytics configuration is deliberately NOT stored in the public `settings`
--    table. GET /api/v1/settings is permitAll and returns every settings row, so
--    that table must never hold credentials. These tables are only reachable via
--    admin-scoped / owner-scoped endpoints.
--  * API keys are stored encrypted at rest (AES-256-GCM, key from the
--    ANALYTICS_ENCRYPTION_KEY environment variable) as Base64(iv || ciphertext || tag).
--  * umami_website_id and umami_base_url are public tracking values by design
--    (they are embedded in public pages by the tracker script), but they live
--    here so the admin manages one coherent configuration.
--  * Script/recorder paths default to the Umami UPSTREAM product defaults
--    (/script.js, /recorder.js) — they are deployment-independent and editable
--    through the GUI. No deployment-specific values are seeded anywhere.
--  * GA4 is a supported tracking provider: ga4_measurement_id is the public
--    Measurement ID (G-XXXXXXX). GA4 REPORTING (Data API) is explicitly
--    deferred and returns a graceful 503; its future service-account secret
--    would follow the same encrypted-secret path as the Umami API key.
--  * Provider-specific fields are namespaced per provider (umami_*, ga4_*)
--    rather than a JSON blob: typed columns give validation, Swagger docs and
--    DTO typing for free while remaining extensible (a new provider adds new
--    namespaced columns + an AnalyticsProvider implementation, nothing else).

CREATE TABLE analytics_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    provider ENUM('NONE', 'UMAMI', 'GA4') NOT NULL DEFAULT 'NONE',
    tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reporting_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    umami_base_url VARCHAR(512) DEFAULT NULL,
    umami_website_name VARCHAR(255) DEFAULT NULL,
    umami_website_id VARCHAR(64) DEFAULT NULL,
    umami_script_path VARCHAR(255) NOT NULL DEFAULT '/script.js',
    umami_recorder_path VARCHAR(255) NOT NULL DEFAULT '/recorder.js',
    ga4_measurement_id VARCHAR(64) DEFAULT NULL,
    recorder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    umami_api_key_cipher TEXT DEFAULT NULL,
    umami_api_key_last4 VARCHAR(4) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_analytics_config_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_analytics_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    mode ENUM('INHERIT', 'OWN', 'DISABLED') NOT NULL DEFAULT 'INHERIT',
    provider ENUM('NONE', 'UMAMI', 'GA4') NOT NULL DEFAULT 'NONE',
    tracking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reporting_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    umami_base_url VARCHAR(512) DEFAULT NULL,
    umami_website_name VARCHAR(255) DEFAULT NULL,
    umami_website_id VARCHAR(64) DEFAULT NULL,
    umami_script_path VARCHAR(255) NOT NULL DEFAULT '/script.js',
    umami_recorder_path VARCHAR(255) NOT NULL DEFAULT '/recorder.js',
    ga4_measurement_id VARCHAR(64) DEFAULT NULL,
    recorder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    umami_api_key_cipher TEXT DEFAULT NULL,
    umami_api_key_last4 VARCHAR(4) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_user_analytics_uuid (uuid),
    UNIQUE KEY uk_user_analytics_user (user_id),
    CONSTRAINT fk_user_analytics_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
