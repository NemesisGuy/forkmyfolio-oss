package com.forkmyfolio.integration;

import com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry;
import com.forkmyfolio.service.servicemodel.AnalyticsSnapshot;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.exception.AnalyticsProviderException;
import com.forkmyfolio.service.analytics.AnalyticsQuery;
import com.forkmyfolio.service.analytics.ResolvedAnalyticsConfig;
import com.forkmyfolio.service.analytics.UmamiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8.5 (DEF-1) — Analytics security and behaviour over the REAL HTTP
 * surface with REAL JWTs (never {@code @WithMockUser}).
 *
 * <p>
 * The Umami HTTP layer is replaced by a {@code @MockBean} {@link UmamiProvider}
 * (no external calls in tests); everything else — Spring Security, JWT
 * authentication, ownership resolution, encryption, validation, error
 * mapping — is the real production stack against a real MySQL via Flyway.
 *
 * <p>
 * Security invariants under test:
 * <ul>
 *   <li>Analytics endpoints require authentication; admin endpoints require
 *       the ADMIN role.</li>
 *   <li>Provider credentials never appear in ANY response (config DTOs expose
 *       only {@code apiKeyLast4}/{@code apiKeyConfigured}) and never leak into
 *       the public {@code /api/v1/settings} payload.</li>
 *   <li>Owner configurations are strictly isolated per user.</li>
 *   <li>The queried Umami website is resolved SERVER-SIDE from the caller's
 *       identity/scope — forged {@code websiteId} request parameters are
 *       ignored (verified by capturing what the provider actually receives).</li>
 *   <li>Missing credentials / provider downtime degrade to HTTP 503, never
 *       500.</li>
 *   <li>The public tracking-config endpoint exposes only non-credential
 *       values and conceals unknown/private portfolios as 404.</li>
 * </ul>
 */
@Transactional
class AnalyticsSecurityIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String ADMIN_BASE = "/api/v1/admin/analytics";
    private static final String ME_BASE = "/api/v1/me/analytics";
    private static final String PUBLIC_BASE = "/api/v1/public/analytics";
    /** Synthetic fixture UUIDs — the application must never depend on any real deployment's identifiers. */
    private static final String PLATFORM_WEBSITE_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    private static final String FORGED_WEBSITE_ID = "c9bf9e57-1685-4c56-b3b0-3f2a5d4c1e77";
    /** Literal public TEST-NET IPs: no DNS dependency, never private/loopback. */
    private static final String PLATFORM_BASE_URL = "https://198.51.100.7";
    private static final String OWNER_BASE_URL = "https://198.51.100.8";
    private static final String PLATFORM_API_KEY = "umami_platform_key_abcdef123456";
    private static final String OWNER_A_API_KEY = "umami_owner_a_key_987654";

    @MockBean
    private UmamiProvider umamiProvider;

    private String adminJwt;
    private String userAJwt;
    private String userBJwt;
    private String slugA;
    private String slugB;
    private String privateSlug;

    @BeforeEach
    void seedUsersAndStubProvider() throws Exception {
        String suffix = runSuffix();
        slugA = "ana-a-" + suffix;
        slugB = "ana-b-" + suffix;
        privateSlug = "ana-priv-" + suffix;

        seedUser("ana-admin-" + suffix + "@example.com", "ana-admin-" + suffix, true, Role.ADMIN);
        seedUser("ana-a-" + suffix + "@example.com", slugA, true);
        seedUser("ana-b-" + suffix + "@example.com", slugB, true);
        seedUser("ana-priv-" + suffix + "@example.com", privateSlug, false);

        adminJwt = realLogin("ana-admin-" + suffix + "@example.com", PASSWORD);
        userAJwt = realLogin("ana-a-" + suffix + "@example.com", PASSWORD);
        userBJwt = realLogin("ana-b-" + suffix + "@example.com", PASSWORD);

        // @MockBean resets between tests; re-stub the provider identity each time.
        when(umamiProvider.type()).thenReturn(com.forkmyfolio.model.enums.AnalyticsProviderType.UMAMI);
    }

    // ------------------------------------------------------------------
    // Authentication / authorization
    // ------------------------------------------------------------------

    @Test
    void unauthenticatedRequests_areRejectedOnScopedEndpoints_butPublicConfigIsPermitAll() throws Exception {
        // Scoped endpoints: 401
        mockMvc.perform(get(ADMIN_BASE + "/config")).andExpect(status().isUnauthorized());
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_BASE + "/config")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_BASE + "/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ME_BASE + "/config/test")).andExpect(status().isUnauthorized());

        // Public tracking-config is permitAll: never 401 (200 disabled-state or 404 concealed).
        mockMvc.perform(get(PUBLIC_BASE + "/" + slugA + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void ownerIsForbidden_fromAdminAnalyticsEndpoints() throws Exception {
        mockMvc.perform(get(ADMIN_BASE + "/config").header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + userAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\": \"UMAMI\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_BASE + "/summary").header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN_BASE + "/config/test").header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Admin configuration: CRUD + credential hygiene
    // ------------------------------------------------------------------

    @Test
    void adminConfigCrud_roundTrip_andApiKeyNeverInResponses() throws Exception {
        // CREATE/UPDATE with a credential
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider": "UMAMI", "trackingEnabled": true, "reportingEnabled": true,
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s", "umamiApiKey": "%s"}
                                """.formatted(PLATFORM_BASE_URL, PLATFORM_WEBSITE_ID, PLATFORM_API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("UMAMI"))
                .andExpect(jsonPath("$.data.trackingEnabled").value(true))
                .andExpect(jsonPath("$.data.reportingEnabled").value(true))
                .andExpect(jsonPath("$.data.umamiBaseUrl").value(PLATFORM_BASE_URL))
                .andExpect(jsonPath("$.data.umamiWebsiteId").value(PLATFORM_WEBSITE_ID))
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.apiKeyLast4").value(PLATFORM_API_KEY.substring(PLATFORM_API_KEY.length() - 4)));

        // READ-back: the raw key material must never appear anywhere in the body.
        MvcResult read = mockMvc.perform(get(ADMIN_BASE + "/config").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(true))
                .andReturn();
        String body = read.getResponse().getContentAsString();
        assertThat(body).doesNotContain(PLATFORM_API_KEY);
        assertThat(body).doesNotContain("umamiApiKey");
        assertThat(body).doesNotContain("cipher");

        // null apiKey → kept; empty string → cleared.
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportingEnabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(true));

        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umamiApiKey\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(false))
                .andExpect(jsonPath("$.data.apiKeyLast4").isEmpty());
    }

    @Test
    void publicSettingsEndpoint_neverContainsAnalyticsConfigOrCredentials() throws Exception {
        // Configure analytics fully (including a secret) first.
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider": "UMAMI", "trackingEnabled": true, "reportingEnabled": true,
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s", "umamiApiKey": "%s"}
                                """.formatted(PLATFORM_BASE_URL, PLATFORM_WEBSITE_ID, PLATFORM_API_KEY)))
                .andExpect(status().isOk());

        // GET /api/v1/settings is permitAll and returns ALL settings rows:
        // it must contain no analytics rows and no credential material.
        MvcResult settings = mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isOk())
                .andReturn();
        String body = settings.getResponse().getContentAsString().toLowerCase();
        assertThat(body).doesNotContain("umami");
        assertThat(body).doesNotContain("analytics");
        assertThat(body).doesNotContain(PLATFORM_API_KEY.toLowerCase());
        assertThat(body).doesNotContain(PLATFORM_WEBSITE_ID);
    }

    // ------------------------------------------------------------------
    // Reporting: server-side scoping and isolation
    // ------------------------------------------------------------------

    @Test
    void inheritMode_reportingResolvesToThePlatformWebsite() throws Exception {
        configurePlatformAnalytics(true);

        // Distinct historic range per test: the reporting cache is shared
        // across tests in this class (5-min TTL), keys include the range.
        long start = 1_700_000_000_000L;
        long end = start + 86_400_000L;

        when(umamiProvider.fetchSummary(any(), any())).thenReturn(new AnalyticsSnapshot(10, 5, 6, 2, 300, null));

        mockMvc.perform(get(ME_BASE + "/summary")
                        .header("Authorization", "Bearer " + userBJwt) // B has no config row → INHERIT
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageviews").value(10))
                .andExpect(jsonPath("$.data.visitors").value(5));

        ArgumentCaptor<ResolvedAnalyticsConfig> captor = ArgumentCaptor.forClass(ResolvedAnalyticsConfig.class);
        verify(umamiProvider).fetchSummary(captor.capture(), any(AnalyticsQuery.class));
        ResolvedAnalyticsConfig resolved = captor.getValue();
        assertThat(resolved.websiteId()).isEqualTo(PLATFORM_WEBSITE_ID);
        assertThat(resolved.source()).isEqualTo(ResolvedAnalyticsConfig.Source.PLATFORM);
        assertThat(resolved.apiKey()).isEqualTo(PLATFORM_API_KEY);
    }

    @Test
    void ownMode_reportingUsesTheOwnersOwnWebsite_andForgedParamsAreIgnored() throws Exception {
        String ownWebsiteId = "11111111-2222-3333-4444-555555555555";
        mockMvc.perform(put(ME_BASE + "/config")
                        .header("Authorization", "Bearer " + userAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "OWN", "provider": "UMAMI", "trackingEnabled": true, "reportingEnabled": true,
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s", "umamiApiKey": "%s"}
                                """.formatted(OWNER_BASE_URL, ownWebsiteId, OWNER_A_API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("OWN"))
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(true));

        long start = 1_700_200_000_000L;
        long end = start + 86_400_000L;

        when(umamiProvider.fetchSummary(any(), any())).thenReturn(new AnalyticsSnapshot(7, 3, 3, 1, 120, null));

        // Forged attempt: A tries to redirect the query at a DIFFERENT website
        // via a request parameter. The controller does not bind it; the
        // resolved config must still point at A's own website.
        mockMvc.perform(get(ME_BASE + "/summary")
                        .header("Authorization", "Bearer " + userAJwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end))
                        .queryParam("websiteId", FORGED_WEBSITE_ID)
                        .queryParam("apiKey", PLATFORM_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageviews").value(7));

        ArgumentCaptor<ResolvedAnalyticsConfig> captor = ArgumentCaptor.forClass(ResolvedAnalyticsConfig.class);
        verify(umamiProvider).fetchSummary(captor.capture(), any(AnalyticsQuery.class));
        ResolvedAnalyticsConfig resolved = captor.getValue();
        assertThat(resolved.websiteId()).isEqualTo(ownWebsiteId);
        assertThat(resolved.baseUrl()).isEqualTo(OWNER_BASE_URL);
        assertThat(resolved.apiKey()).isEqualTo(OWNER_A_API_KEY); // decrypted server-side
        assertThat(resolved.source()).isEqualTo(ResolvedAnalyticsConfig.Source.OWNER);
    }

    @Test
    void ownerConfigs_areStrictlyIsolated_betweenUsers() throws Exception {
        String aWebsiteId = "aaaaaaaa-1111-2222-3333-444444444444";
        String bWebsiteId = "bbbbbbbb-1111-2222-3333-444444444444";

        mockMvc.perform(put(ME_BASE + "/config")
                        .header("Authorization", "Bearer " + userAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "OWN", "provider": "UMAMI",
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s", "umamiApiKey": "%s"}
                                """.formatted(OWNER_BASE_URL, aWebsiteId, OWNER_A_API_KEY)))
                .andExpect(status().isOk());

        // B reads their own config: it must NOT contain any of A's values.
        MvcResult bConfig = mockMvc.perform(get(ME_BASE + "/config").header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("INHERIT")) // synthesized default
                .andReturn();
        String bBody = bConfig.getResponse().getContentAsString();
        assertThat(bBody).doesNotContain(aWebsiteId);
        assertThat(bBody).doesNotContain(OWNER_A_API_KEY);
        assertThat(bBody).doesNotContain(OWNER_A_API_KEY.substring(OWNER_A_API_KEY.length() - 4));

        // B cannot mutate A's row (there is no path that accepts another
        // user's identity): B's PUT only touches B's own row.
        mockMvc.perform(put(ME_BASE + "/config")
                        .header("Authorization", "Bearer " + userBJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\": \"DISABLED\"}"))
                .andExpect(status().isOk());

        // A's configuration is untouched by B's update.
        mockMvc.perform(get(ME_BASE + "/config").header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("OWN"))
                .andExpect(jsonPath("$.data.umamiWebsiteId").value(aWebsiteId));
    }

    @Test
    void disabledMode_reportingIsUnavailable503_andPublicTrackingIsOff() throws Exception {
        configurePlatformAnalytics(true);

        mockMvc.perform(put(ME_BASE + "/config")
                        .header("Authorization", "Bearer " + userAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\": \"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("DISABLED"))
                .andExpect(jsonPath("$.data.effectiveTrackingEnabled").value(false));

        long start = 1_700_400_000_000L;
        long end = start + 86_400_000L;

        mockMvc.perform(get(ME_BASE + "/summary")
                        .header("Authorization", "Bearer " + userAJwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("analytics_unavailable"));

        // Public pages of a disabled owner carry no tracking.
        mockMvc.perform(get(PUBLIC_BASE + "/" + slugA + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.scriptUrl").isEmpty())
                .andExpect(jsonPath("$.data.websiteId").isEmpty());
    }

    @Test
    void missingCredentials_reportingIsUnavailable503() throws Exception {
        // Provider configured but NO API key stored: tracking may work,
        // reporting must fail gracefully with 503 (not 500, not 200).
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider": "UMAMI", "trackingEnabled": true, "reportingEnabled": true,
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s"}
                                """.formatted(PLATFORM_BASE_URL, PLATFORM_WEBSITE_ID)))
                .andExpect(status().isOk());

        long start = 1_700_500_000_000L;
        long end = start + 86_400_000L;

        mockMvc.perform(get(ADMIN_BASE + "/summary")
                        .header("Authorization", "Bearer " + adminJwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("analytics_unavailable"));
    }

    @Test
    void providerDowntime_returnsGraceful503_never500() throws Exception {
        configurePlatformAnalytics(true);

        long start = 1_700_600_000_000L;
        long end = start + 86_400_000L;

        when(umamiProvider.fetchSummary(any(), any()))
                .thenThrow(new AnalyticsProviderException("Umami instance is unreachable"));

        mockMvc.perform(get(ADMIN_BASE + "/summary")
                        .header("Authorization", "Bearer " + adminJwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("analytics_provider_unavailable"));

        // Same for the owner-scoped surface.
        when(umamiProvider.fetchTimeSeries(any(), any()))
                .thenThrow(new AnalyticsProviderException("Umami instance is unreachable"));
        mockMvc.perform(get(ME_BASE + "/timeseries")
                        .header("Authorization", "Bearer " + userBJwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end + 1)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("analytics_provider_unavailable"));
    }

    @Test
    void adminReporting_successPath_flowsProviderDataThrough() throws Exception {
        configurePlatformAnalytics(true);

        long start = 1_700_700_000_000L;
        long end = start + 86_400_000L;

        when(umamiProvider.fetchSummary(any(), any())).thenReturn(
                new AnalyticsSnapshot(120, 45, 50, 12, 5400, null));
        when(umamiProvider.fetchMetrics(any(), any(), any(), anyInt())).thenReturn(
                List.of(new com.forkmyfolio.service.servicemodel.AnalyticsMetricEntry("/projects", 60)));

        mockMvc.perform(get(ADMIN_BASE + "/summary")
                        .header("Authorization", "Bearer " + adminJwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageviews").value(120))
                .andExpect(jsonPath("$.data.visitors").value(45));

        mockMvc.perform(get(ADMIN_BASE + "/metrics")
                        .header("Authorization", "Bearer " + adminJwt)
                        .queryParam("type", "PATH")
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("/projects"))
                .andExpect(jsonPath("$.data[0].value").value(60));
    }

    // ------------------------------------------------------------------
    // Input validation
    // ------------------------------------------------------------------

    @Test
    void invalidDateRanges_areRejectedWith400() throws Exception {
        configurePlatformAnalytics(true);

        String jwt = adminJwt;
        long start = 1_700_800_000_000L;

        // end before start
        mockMvc.perform(get(ADMIN_BASE + "/summary").header("Authorization", "Bearer " + jwt)
                        .queryParam("startAt", String.valueOf(start + 86_400_000L))
                        .queryParam("endAt", String.valueOf(start)))
                .andExpect(status().isBadRequest());
        // zero/negative epoch
        mockMvc.perform(get(ADMIN_BASE + "/summary").header("Authorization", "Bearer " + jwt)
                        .queryParam("startAt", "0").queryParam("endAt", String.valueOf(start)))
                .andExpect(status().isBadRequest());
        // range > 366 days
        mockMvc.perform(get(ADMIN_BASE + "/summary").header("Authorization", "Bearer " + jwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(start + 400L * 86_400_000L)))
                .andExpect(status().isBadRequest());
        // unknown period
        mockMvc.perform(get(ADMIN_BASE + "/summary").header("Authorization", "Bearer " + jwt)
                        .queryParam("period", "13months"))
                .andExpect(status().isBadRequest());
        // period AND explicit range together
        mockMvc.perform(get(ADMIN_BASE + "/summary").header("Authorization", "Bearer " + jwt)
                        .queryParam("period", "7d")
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(start + 86_400_000L)))
                .andExpect(status().isBadRequest());
        // unsupported metric type
        mockMvc.perform(get(ADMIN_BASE + "/metrics").header("Authorization", "Bearer " + jwt)
                        .queryParam("type", "PASSWORDS"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Public tracking config
    // ------------------------------------------------------------------

    @Test
    void publicTrackingConfig_exposesOnlyNonCredentialValues() throws Exception {
        configurePlatformAnalytics(true);

        MvcResult result = mockMvc.perform(get(PUBLIC_BASE + "/" + slugB + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.scriptUrl").value(PLATFORM_BASE_URL + "/script.js"))
                .andExpect(jsonPath("$.data.websiteId").value(PLATFORM_WEBSITE_ID))
                .andExpect(jsonPath("$.data.recorderEnabled").value(false))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(PLATFORM_API_KEY);
        assertThat(body).doesNotContain("apiKey");
        assertThat(body).doesNotContain("cipher");
    }

    @Test
    void publicTrackingConfig_concealsUnknownAndPrivatePortfoliosAs404() throws Exception {
        configurePlatformAnalytics(true);

        mockMvc.perform(get(PUBLIC_BASE + "/no-such-portfolio/tracking-config"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(PUBLIC_BASE + "/" + privateSlug + "/tracking-config"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicTrackingConfig_ownerOwnMode_usesTheOwnersOwnWebsite() throws Exception {
        configurePlatformAnalytics(true);

        String ownWebsiteId = "cccccccc-1111-2222-3333-444444444444";
        mockMvc.perform(put(ME_BASE + "/config")
                        .header("Authorization", "Bearer " + userAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "OWN", "provider": "UMAMI", "trackingEnabled": true,
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s"}
                                """.formatted(OWNER_BASE_URL, ownWebsiteId)))
                .andExpect(status().isOk());

        mockMvc.perform(get(PUBLIC_BASE + "/" + slugA + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.websiteId").value(ownWebsiteId))
                .andExpect(jsonPath("$.data.scriptUrl").value(OWNER_BASE_URL + "/script.js"));

        // B still inherits the platform website — A's choice never leaks.
        mockMvc.perform(get(PUBLIC_BASE + "/" + slugB + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.websiteId").value(PLATFORM_WEBSITE_ID));
    }

    @Test
    void publicTrackingConfig_recorderOffByDefault_andNeverOnWithoutExplicitConsent() throws Exception {
        configurePlatformAnalytics(true);

        // Platform recorder defaults to false; flip it on explicitly and verify
        // the recorder URL appears (still non-credential values only).
        mockMvc.perform(get(PUBLIC_BASE + "/" + slugB + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recorderEnabled").value(false))
                .andExpect(jsonPath("$.data.recorderUrl").isEmpty());

        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recorderEnabled\": true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PUBLIC_BASE + "/" + slugB + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recorderEnabled").value(true))
                .andExpect(jsonPath("$.data.recorderUrl").value(PLATFORM_BASE_URL + "/recorder.js"));
    }

    // ------------------------------------------------------------------
    // Provider-agnostic configuration (self-hostable deployment independence)
    // ------------------------------------------------------------------

    @Test
    void scriptPaths_areConfigurable_andReflectedInPublicConfig() throws Exception {
        // A deployment hosting Umami behind a non-standard script name/path
        // must work without any code or frontend-image change.
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider": "UMAMI", "trackingEnabled": true, "reportingEnabled": false,
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s",
                                 "umamiScriptPath": "/analytics/collect.js", "umamiRecorderPath": "/analytics/record.js"}
                                """.formatted(PLATFORM_BASE_URL, PLATFORM_WEBSITE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.umamiScriptPath").value("/analytics/collect.js"))
                .andExpect(jsonPath("$.data.umamiRecorderPath").value("/analytics/record.js"));

        mockMvc.perform(get(PUBLIC_BASE + "/" + slugB + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scriptUrl").value(PLATFORM_BASE_URL + "/analytics/collect.js"));
    }

    @Test
    void scriptPathInjection_attemptsAreRejected() throws Exception {
        // Protocol-relative URL (would bypass the validated base URL's origin)
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umamiScriptPath\": \"//evil.example.com/x.js\"}"))
                .andExpect(status().isBadRequest());
        // Absolute URL smuggled into the path field
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umamiScriptPath\": \"https://evil.example.com/x.js\"}"))
                .andExpect(status().isBadRequest());
        // Query/fragment smuggling and HTML/attribute-breaking characters
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umamiScriptPath\": \"/x.js?steal=1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umamiRecorderPath\": \"/x\\\" onerror=alert(1)\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ga4_isConfigurable_tracksViaGtag_andReportingIsGracefullyUnavailable() throws Exception {
        // Configure GA4 as the platform provider (no Umami fields required).
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider": "GA4", "trackingEnabled": true, "reportingEnabled": true,
                                 "ga4MeasurementId": "G-ABCDEF1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("GA4"))
                .andExpect(jsonPath("$.data.ga4MeasurementId").value("G-ABCDEF1234"));

        // Public tracking config: gtag.js + Measurement ID, no recorder.
        mockMvc.perform(get(PUBLIC_BASE + "/" + slugB + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("GA4"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.scriptUrl")
                        .value("https://www.googletagmanager.com/gtag/js?id=G-ABCDEF1234"))
                .andExpect(jsonPath("$.data.websiteId").value("G-ABCDEF1234"))
                .andExpect(jsonPath("$.data.recorderEnabled").value(false));

        // Reporting is explicitly not implemented yet → graceful 503 (never 500).
        long start = 1_700_900_000_000L;
        long end = start + 86_400_000L;
        mockMvc.perform(get(ADMIN_BASE + "/summary")
                        .header("Authorization", "Bearer " + adminJwt)
                        .queryParam("startAt", String.valueOf(start))
                        .queryParam("endAt", String.valueOf(end)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("analytics_unavailable"));

        // Connection test validates the Measurement ID format honestly.
        MvcResult test = mockMvc.perform(post(ADMIN_BASE + "/config/test")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andReturn();
        String body = test.getResponse().getContentAsString();
        assertThat(body).contains("true");
        assertThat(body).contains("not implemented");

        // Invalid Measurement IDs are rejected at save time.
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ga4MeasurementId\": \"NOT-A-G-ID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void freshDeployment_hasNoAnalyticsDependency_andPublicPagesCarryNoTracking() throws Exception {
        // A brand-new deployment (no configuration performed at all): the
        // settings table must be free of analytics rows and the public
        // tracking config must be disabled — zero dependence on any external
        // analytics infrastructure until an admin configures it via the GUI.
        MvcResult settings = mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(settings.getResponse().getContentAsString().toLowerCase()).doesNotContain("umami");

        mockMvc.perform(get(PUBLIC_BASE + "/" + slugB + "/tracking-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.provider").value("NONE"));

        // Owner dashboards are unavailable until configured.
        mockMvc.perform(get(ME_BASE + "/summary").header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isServiceUnavailable());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void configurePlatformAnalytics(boolean enabled) throws Exception {
        mockMvc.perform(put(ADMIN_BASE + "/config")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider": "UMAMI", "trackingEnabled": %s, "reportingEnabled": %s,
                                 "umamiBaseUrl": "%s", "umamiWebsiteId": "%s", "umamiApiKey": "%s"}
                                """.formatted(enabled, enabled, PLATFORM_BASE_URL, PLATFORM_WEBSITE_ID, PLATFORM_API_KEY)))
                .andExpect(status().isOk());
    }
}
