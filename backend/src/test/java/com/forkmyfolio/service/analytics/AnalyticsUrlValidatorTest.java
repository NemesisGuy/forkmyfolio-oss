package com.forkmyfolio.service.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 8.5 (DEF-1) — SSRF policy for externally-configured analytics base
 * URLs. Externally-supplied URLs must never be able to reach loopback,
 * private (RFC1918), link-local (cloud metadata) or odd-port endpoints, and
 * must not smuggle credentials via userinfo.
 *
 * <p>Literal IP addresses are used instead of hostnames so no test performs a
 * real DNS lookup.</p>
 */
class AnalyticsUrlValidatorTest {

    private final AnalyticsUrlValidator validator = new AnalyticsUrlValidator();

    @Test
    void acceptsValidHttpsUrl_andNormalizesTrailingSlash() {
        assertThat(validator.validate("https://198.51.100.7")).isEqualTo("https://198.51.100.7");
        assertThat(validator.validate("https://198.51.100.7/")).isEqualTo("https://198.51.100.7");
        assertThat(validator.validate("  https://198.51.100.7  ")).isEqualTo("https://198.51.100.7");
    }

    @Test
    void rejectsPlainHttp_forNonLoopbackHosts() {
        assertThatThrownBy(() -> validator.validate("http://198.51.100.7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");
    }

    @Test
    void allowsPlainHttp_onlyForLoopbackDevelopment() {
        assertThat(validator.validate("http://localhost")).startsWith("http://localhost");
        assertThat(validator.validate("http://127.0.0.1")).startsWith("http://127.0.0.1");
        assertThat(validator.validate("https://localhost:8443")).startsWith("https://localhost:8443");
    }

    @Test
    void rejectsPrivateAndLinkLocalAddresses() {
        // RFC1918
        assertThatThrownBy(() -> validator.validate("https://10.1.2.3")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("https://192.168.1.1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("https://172.16.0.9")).isInstanceOf(IllegalArgumentException.class);
        // Link-local (cloud metadata endpoints)
        assertThatThrownBy(() -> validator.validate("https://169.254.169.254")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUserinfoCredentials_inUrl() {
        assertThatThrownBy(() -> validator.validate("https://user:pass@198.51.100.7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void rejectsUnexpectedPorts_butAllowsWebPorts() {
        assertThat(validator.validate("https://198.51.100.7:443")).isNotNull();
        assertThat(validator.validate("https://198.51.100.7:8443")).isNotNull();
        assertThat(validator.validate("http://localhost:80")).isNotNull();

        assertThatThrownBy(() -> validator.validate("https://198.51.100.7:8081"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
        assertThatThrownBy(() -> validator.validate("https://198.51.100.7:22"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> validator.validate("ftp://198.51.100.7")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("file:///etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("gopher://198.51.100.7")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedAndEmptyInput() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("not a url")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("https://")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isSafe_isTheNonThrowingVariant() {
        assertThat(validator.isSafe("https://198.51.100.7")).isTrue();
        assertThat(validator.isSafe("https://169.254.169.254")).isFalse();
        assertThat(validator.isSafe("garbage")).isFalse();
        assertThat(validator.isSafe(null)).isFalse();
    }

    // ------------------------------------------------------------------
    // Script-path validation (anti-injection)
    // ------------------------------------------------------------------

    @Test
    void scriptPaths_acceptRelativePaths_andPreserveValue() {
        assertThat(validator.validateScriptPath("/script.js")).isEqualTo("/script.js");
        assertThat(validator.validateScriptPath("/analytics/collect.js")).isEqualTo("/analytics/collect.js");
        assertThat(validator.validateScriptPath("  /recorder.js  ")).isEqualTo("/recorder.js");
    }

    @Test
    void scriptPaths_rejectAnythingThatIsNotARelativePath() {
        // Protocol-relative URL would escape the validated base URL's origin
        assertThatThrownBy(() -> validator.validateScriptPath("//evil.example.com/x.js"))
                .isInstanceOf(IllegalArgumentException.class);
        // Absolute URL smuggled into the path field
        assertThatThrownBy(() -> validator.validateScriptPath("https://evil.example.com/x.js"))
                .isInstanceOf(IllegalArgumentException.class);
        // javascript: and data: payloads
        assertThatThrownBy(() -> validator.validateScriptPath("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateScriptPath("data:text/html,<script>"))
                .isInstanceOf(IllegalArgumentException.class);
        // Query/fragment smuggling
        assertThatThrownBy(() -> validator.validateScriptPath("/x.js?steal=1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateScriptPath("/x.js#frag"))
                .isInstanceOf(IllegalArgumentException.class);
        // HTML/attribute-breaking characters and whitespace
        assertThatThrownBy(() -> validator.validateScriptPath("/x\" onerror=alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateScriptPath("/x y.js"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateScriptPath("/x<y>.js"))
                .isInstanceOf(IllegalArgumentException.class);
        // Empty / null
        assertThatThrownBy(() -> validator.validateScriptPath(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateScriptPath("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scriptPaths_isSafeVariantMatchesValidation() {
        assertThat(validator.isSafeScriptPath("/script.js")).isTrue();
        assertThat(validator.isSafeScriptPath("//evil.com/x.js")).isFalse();
        assertThat(validator.isSafeScriptPath("/x y")).isFalse();
        assertThat(validator.isSafeScriptPath(null)).isFalse();
    }

    // ------------------------------------------------------------------
    // GA4 Measurement ID validation
    // ------------------------------------------------------------------

    @Test
    void measurementIds_acceptTheGFormat_andNormalizeCase() {
        assertThat(validator.validateMeasurementId("G-ABCDEF12")).isEqualTo("G-ABCDEF12");
        assertThat(validator.validateMeasurementId("g-abcdef123456")).isEqualTo("G-ABCDEF123456");
        assertThat(validator.validateMeasurementId("  G-XYZ987  ")).isEqualTo("G-XYZ987");
    }

    @Test
    void measurementIds_rejectMalformedIdentifiers() {
        assertThatThrownBy(() -> validator.validateMeasurementId("UA-123456-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateMeasurementId("NOT-A-G-ID"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateMeasurementId("G-"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateMeasurementId("G-AB{};DROP"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validateMeasurementId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
