package com.forkmyfolio.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8.4 — Job Application cross-user isolation over the REAL HTTP
 * surface with REAL JWTs (never {@code @WithMockUser}).
 *
 * <p>
 * Job applications are PRIVATE user data. User B must never read, mutate or
 * destroy User A's applications — including status changes and visibility
 * flips, which must not bypass the ownership check. Cross-tenant existence
 * is concealed as 404 (never 403) per the Phase 7.4 §403/404 policy, so uuid
 * probing yields no existence oracle.
 */
@Transactional
public class JobApplicationIsolationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String BASE = "/api/v1/me/job-applications";

    private String userAJwt;
    private String userBJwt;
    private String userAEmail;

    @BeforeEach
    void seedTwoUsers() throws Exception {
        String suffix = runSuffix();
        userAEmail = "trk-a-" + suffix + "@example.com";
        String userBSlug = "trk-b-" + suffix;

        seedUser(userAEmail, "trk-a-" + suffix, true);
        seedUser("trk-b-" + suffix + "@example.com", userBSlug, true);

        userAJwt = realLogin(userAEmail, PASSWORD);
        userBJwt = realLogin("trk-b-" + suffix + "@example.com", PASSWORD);
    }

    private String createApplicationFor(String jwt, String title) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\": \"%s\", \"companyName\": \"Private Co\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("uuid").asText();
    }

    @Test
    void userA_canCrud_ownApplications() throws Exception {
        // CREATE
        String uuid = createApplicationFor(userAJwt, "A's Own Role");

        // READ
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobTitle").value("A's Own Role"));

        // UPDATE (status change)
        mockMvc.perform(put(BASE + "/{uuid}", uuid)
                        .header("Authorization", "Bearer " + userAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INTERVIEW_SCHEDULED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"));

        // DELETE
        mockMvc.perform(delete(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void userB_cannotReadUpdateOrDelete_userAApplication() throws Exception {
        String uuidA = createApplicationFor(userAJwt, "A's Secret Role");

        // READ -> 404 (existence concealed)
        mockMvc.perform(get(BASE + "/{uuid}", uuidA).header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isNotFound());

        // UPDATE (incl. status change) -> 404, A's data unchanged
        mockMvc.perform(put(BASE + "/{uuid}", uuidA)
                        .header("Authorization", "Bearer " + userBJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isNotFound());

        // Visibility flip cannot bypass ownership either
        mockMvc.perform(put(BASE + "/{uuid}", uuidA)
                        .header("Authorization", "Bearer " + userBJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": true}"))
                .andExpect(status().isNotFound());

        // DELETE -> 404
        mockMvc.perform(delete(BASE + "/{uuid}", uuidA).header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isNotFound());

        // STATE VERIFICATION: A's application is untouched
        mockMvc.perform(get(BASE + "/{uuid}", uuidA).header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobTitle").value("A's Secret Role"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.visible").value(false));
    }

    @Test
    void listEndpoints_returnOnlyTheAuthenticatedUsersApplications() throws Exception {
        String uuidA = createApplicationFor(userAJwt, "A-only Role");
        String uuidB = createApplicationFor(userBJwt, "B-only Role");

        MvcResult aList = mockMvc.perform(get(BASE).header("Authorization", "Bearer " + userAJwt))
                .andExpect(status().isOk())
                .andReturn();
        String aBody = aList.getResponse().getContentAsString();
        assertThat(aBody).contains("A-only Role");
        assertThat(aBody).doesNotContain("B-only Role");
        assertThat(aBody).doesNotContain(uuidB);

        MvcResult bList = mockMvc.perform(get(BASE).header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isOk())
                .andReturn();
        String bBody = bList.getResponse().getContentAsString();
        assertThat(bBody).contains("B-only Role");
        assertThat(bBody).doesNotContain("A-only Role");
        assertThat(bBody).doesNotContain(uuidA);
    }

    @Test
    void guessedUuid_yieldsNoExistenceOracle() throws Exception {
        // A random (nonexistent) uuid and A's real uuid must be
        // indistinguishable to B: both 404.
        String uuidA = createApplicationFor(userAJwt, "Probe Target");
        String fakeUuid = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(get(BASE + "/{uuid}", fakeUuid).header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/{uuid}", uuidA).header("Authorization", "Bearer " + userBJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequests_cannotAccessPrivateApplications() throws Exception {
        String uuidA = createApplicationFor(userAJwt, "Unauth Target");

        // No token
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/{uuid}", uuidA)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\": \"Sneaky\", \"companyName\": \"Sneaky Co\"}"))
                .andExpect(status().isUnauthorized());

        // Garbage token
        mockMvc.perform(get(BASE).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}
