package com.forkmyfolio.integration;

import com.forkmyfolio.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 0.31 (TASKS §H1 / KNOWN_ISSUES §14): regression pins for the silent
 * unpublish defect on {@code PUT /api/v1/me/profile}.
 *
 * <p>
 * <b>The defect:</b> {@code UpdatePortfolioProfileRequest} declared
 * {@code visible}/{@code publiclyVisible} as primitive booleans and
 * {@code createOrUpdateProfile} copied them unconditionally, so any profile
 * save whose body omitted those keys (the cover-letter editor
 * {@code UserProfileEditPage.vue} never sends them) wrote
 * {@code is_public=0 AND visible=0} with HTTP 200 — a silent takedown of the
 * public portfolio. Proven on DEV 2026-11-23 and again during the owner's
 * 2026-11-24 report.
 * </p>
 *
 * <p>
 * <b>The fix:</b> the two fields were removed from the request DTO, mapper and
 * service — a profile save can no longer touch visibility at all. The only
 * writers are {@code PUT /me/profile/visibility} and
 * {@code PUT /me/profile/indexing}.
 * </p>
 *
 * <p>
 * Deliberately not {@code @Transactional}: assertions must observe committed
 * state exactly as a browser would (and the guest 404/200 control reads
 * through Redis, which a rolled-back transaction would desynchronise).
 * </p>
 */
public class ProfileVisibilityPartialUpdateIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String PASSWORD = "Password123!";

    private String jwt;
    private String slug;

    @BeforeEach
    void seedPublishedOwnerAndLogin() throws Exception {
        String suffix = runSuffix();
        slug = "vispin-" + suffix;
        User owner = seedUser("vispin-" + suffix + "@example.com", slug, true);
        jwt = realLogin(owner.getEmail(), PASSWORD);
    }

    private void putProfile(String body) throws Exception {
        mockMvc.perform(put("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private void assertFlags(boolean expectedPublic, boolean expectedVisible) throws Exception {
        mockMvc.perform(get("/api/v1/me/profile").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublic").value(expectedPublic))
                .andExpect(jsonPath("$.data.visible").value(expectedVisible));
    }

    private void assertGuestSeesPortfolio(int expectedStatus) throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().is(expectedStatus));
    }

    /**
     * THE owner repro: a text-only profile save (exactly what the cover-letter
     * editor sends) must leave the published state alone. Pre-fix this failed
     * on the very first assertion (isPublic flipped true -> false) and on the
     * guest control (200 -> 404 silent takedown).
     */
    @Test
    void partialProfileSave_withoutVisibilityKeys_mustNotUnpublish() throws Exception {
        assertFlags(true, true);
        assertGuestSeesPortfolio(200);

        putProfile("""
                {"headline": "Edited headline (H1 pin)",
                 "summary": "Edited summary — must NOT unpublish the portfolio."}""");

        // Flags untouched...
        assertFlags(true, true);
        // ...and the public portfolio is still reachable for anonymous visitors.
        assertGuestSeesPortfolio(200);
    }

    /**
     * Structural guarantee of the fix: even EXPLICIT false visibility keys in
     * a profile save are ignored — the DTO no longer carries them. Only the
     * dedicated endpoints can change visibility.
     */
    @Test
    void profileSave_withExplicitFalseVisibilityKeys_isIgnored() throws Exception {
        assertFlags(true, true);

        putProfile("""
                {"headline": "Trying to sneak a takedown",
                 "visible": false, "isPublic": false, "publiclyVisible": false}""");

        assertFlags(true, true);
        assertGuestSeesPortfolio(200);
    }

    /**
     * Control: the dedicated visibility endpoint remains the single writer and
     * still works end to end (write -> flags -> guest 404/200 through cache).
     */
    @Test
    void dedicatedVisibilityEndpoint_isStillAuthoritative() throws Exception {
        mockMvc.perform(put("/api/v1/me/profile/visibility")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPublic\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublic").value(false));

        assertFlags(false, true);          // visible (section) flag untouched
        assertGuestSeesPortfolio(404);      // taken private, anon locked out

        mockMvc.perform(put("/api/v1/me/profile/visibility")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPublic\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublic").value(true));

        assertFlags(true, true);
        assertGuestSeesPortfolio(200);
    }

    /**
     * The cover-letter editor's payload verbatim (UserProfileEditPage.vue
     * sends text fields only) followed by a section-settings save: neither may
     * move the visibility flags. This is the exact two-step sequence the owner
     * described ("system disregards the current settings").
     */
    @Test
    void coverLetterStyleSave_thenSectionSave_neverTouchesVisibility() throws Exception {
        putProfile("""
                {"headline": "Cover letter flow",
                 "summary": "Dear Hiring Team, ...",
                 "coverLetterTemplate": "Dear Hiring Manager,\\n\\nI am applying..."}""");
        assertFlags(true, true);

        // A second, fuller save (as Manage Profile sends: text + stale keys).
        putProfile("""
                {"headline": "Cover letter flow v2",
                 "summary": "Updated summary.",
                 "location": "Remote",
                 "isPublic": true, "visible": true}""");
        assertFlags(true, true);
        assertGuestSeesPortfolio(200);
    }
}
