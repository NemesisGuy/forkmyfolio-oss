package com.forkmyfolio.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Master (portfolio level) and section-level visibility semantics plus the
 * guest-pass flow, over the real HTTP surface with real JWTs.
 */
@Transactional
public class PublicPortfolioVisibilityIntegrationTest extends AbstractMySqlIntegrationTest {

        private static final String PASSWORD = "Password123!";

        private String publicSlug;
        private String privateSlug;
        private String privateOwnerEmail;
        private String publicOwnerEmail;
        private String invisibleProjectTitle;

        @BeforeEach
        void seedOwners() {
                String suffix = runSuffix();
                publicSlug = "visible-me-" + suffix;
                privateSlug = "hidden-me-" + suffix;
                publicOwnerEmail = "visible-owner-" + suffix + "@example.com";
                privateOwnerEmail = "hidden-owner-" + suffix + "@example.com";
                invisibleProjectTitle = "Invisible Project " + suffix;

                seedUser(publicOwnerEmail, publicSlug, true);
                seedUser(privateOwnerEmail, privateSlug, false);
        }

        @Test
        void privatePortfolio_isConcealedAsNotFoundToAnonymous() throws Exception {
                // Phase 7.4 (§403/404 policy): a private portfolio the guest
                // cannot see must be indistinguishable from a nonexistent slug —
                // otherwise the old 403 "This portfolio is private." leaked the
                // information that the slug exists at all.
                mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("resource_not_found"));
        }

        @Test
        void privatePortfolio_isConcealedToOtherAuthenticatedUser() throws Exception {
                // Phase 7.4 (§403/404 policy): an authenticated user who is NOT
                // the owner gets the same 404 that a nonexistent slug would
                // produce — no existence disclosure for logged-in probing either.
                String publicOwnerJwt = realLogin(publicOwnerEmail, PASSWORD);
                mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug)
                                .header("Authorization", "Bearer " + publicOwnerJwt))
                                .andExpect(status().isNotFound());
        }

        @Test
        void owner_retainsAuthorizedAccessToOwnPrivatePortfolio() throws Exception {
                // The owner must NOT be locked out by the concealment: the
                // owner-authorized branch still resolves and renders their own
                // private portfolio (this is what the Vue SPA lock screen counts on).
                String ownerJwt = realLogin(privateOwnerEmail, PASSWORD);
                mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug)
                                .header("Authorization", "Bearer " + ownerJwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.user.slug").value(privateSlug));
        }

        @Test
        void publicToPrivateToggle_enforcesImmediatelyForAnonymousVisitors() throws Exception {
                // Phase 7.4 (§403/404 policy): flipping a portfolio from public
                // to private must be enforced on the very next anonymous fetch —
                // no cache lag (the mutation evicts the publicPortfolios cache),
                // and the response must now be 404, not the old 403.
                String ownerEmail = "toggle-owner-" + runSuffix() + "@example.com";
                String slug = "toggle-" + runSuffix();
                seedUser(ownerEmail, slug, true);
                String ownerJwt = realLogin(ownerEmail, PASSWORD);

                mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                                .andExpect(status().isOk());

                mockMvc.perform(put("/api/v1/me/profile/visibility")
                                .header("Authorization", "Bearer " + ownerJwt)
                                .contentType("application/json")
                                .content("{\"publiclyVisible\": false}"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                                .andExpect(status().isNotFound());
        }

        @Test
        void publicPortfolio_isAccessibleAnonymously() throws Exception {
                mockMvc.perform(get("/api/v1/portfolios/{slug}", publicSlug))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.user.slug").value(publicSlug));
        }

        @Test
        void sectionVisibility_hidesInvisibleContentAnonymously() throws Exception {
                String suffix = runSuffix();
                String publicOwnerEmail = "section-owner-" + suffix + "@example.com";
                String slug = "section-owner-" + suffix;
                seedUser(publicOwnerEmail, slug, true);

                String jwt = realLogin(publicOwnerEmail, PASSWORD);
                mockMvc.perform(post("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "Hidden Section Project", "description": "Never shown when invisible",
                                                 "visible": false, "displayOrder": 0, "skills": []}
                                                """))
                                .andExpect(status().isCreated());

                // Anonymous section listing must NOT contain the invisible project
                MvcResult publicList = mockMvc.perform(get("/api/v1/portfolios/{slug}/projects", slug))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(publicList.getResponse().getContentAsString())
                                .doesNotContain("Hidden Section Project");

                // The visible project endpoint exposes only visible data even to
                // the anonymous public, while the full /me listing still has it.
                MvcResult meList = mockMvc.perform(get("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(meList.getResponse().getContentAsString())
                                .contains("Hidden Section Project");
        }

        @Test
        void guestPass_unlocksPrivatePortfolioReads() throws Exception {
                // The private owner creates a guest pass via their own real JWT
                String ownerJwt = realLogin(privateOwnerEmail, PASSWORD);

                MvcResult created = mockMvc.perform(post("/api/v1/management/guest-passes")
                                .header("Authorization", "Bearer " + ownerJwt)
                                .param("label", "recruiter-pass")
                                .param("durationInHours", "24"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.uuid").isNotEmpty())
                                .andReturn();
                var createdTree = objectMapper.readTree(created.getResponse().getContentAsString()).path("data");
                String guestPassUuid = createdTree.path("uuid").asText();
                String guestPassToken = createdTree.path("token").asText();

                // Anonymous exchange of the pass for a guest JWT
                MvcResult guestLogin = mockMvc.perform(post("/api/v1/auth/guest-login")
                                .contentType("application/json")
                                .content("{\"token\": \"" + guestPassToken + "\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                                .andReturn();
                String guestJwt = objectMapper.readTree(guestLogin.getResponse().getContentAsString())
                                .path("data").path("accessToken").asText();

                // The guest can now read the previously private portfolio
                mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug)
                                .header("Authorization", "Bearer " + guestJwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.user.slug").value(privateSlug));
        }

        @Test
        void guestJwt_isReadOnlyAndCannotManageGuestPasses() throws Exception {
                String ownerJwt = realLogin(privateOwnerEmail, PASSWORD);

                MvcResult created = mockMvc.perform(post("/api/v1/management/guest-passes")
                                .header("Authorization", "Bearer " + ownerJwt)
                                .param("label", "read-only-check")
                                .param("durationInHours", "24"))
                                .andExpect(status().isOk())
                                .andReturn();
                var createdTree = objectMapper.readTree(created.getResponse().getContentAsString()).path("data");
                String guestPassUuid = createdTree.path("uuid").asText();
                String guestPassToken = createdTree.path("token").asText();

                MvcResult guestLogin = mockMvc.perform(post("/api/v1/auth/guest-login")
                                .contentType("application/json")
                                .content("{\"token\": \"" + guestPassToken + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn();
                String guestJwt = objectMapper.readTree(guestLogin.getResponse().getContentAsString())
                                .path("data").path("accessToken").asText();

                // REGRESSION (Phase 7.2 fix): guest JWTs carry the owner's principal;
                // writes to /me/** MUST be denied so a guest can never mutate it.
                mockMvc.perform(post("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + guestJwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "Guest Write Attempt", "description": "Guests must never mutate",
                                                 "visible": true, "displayOrder": 0, "skills": []}
                                                """))
                                .andExpect(status().isForbidden());

                // Guests must never manage (or revoke) the owner's guest passes
                mockMvc.perform(get("/api/v1/management/guest-passes")
                                .header("Authorization", "Bearer " + guestJwt))
                                .andExpect(status().isForbidden());
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/v1/management/guest-passes/{uuid}", guestPassToken)
                                .header("Authorization", "Bearer " + guestJwt))
                                .andExpect(status().isForbidden());

                // The invisible-title reseeding used by other tests is not relevant
                // here; this field merely documents the intended seeded data shape.
                assertThat(invisibleProjectTitle).startsWith("Invisible Project");
        }
}
