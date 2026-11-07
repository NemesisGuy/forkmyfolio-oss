package com.forkmyfolio.integration;

import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ApplicationStatus;
import com.forkmyfolio.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8.4 — Job Application lifecycle, data model and visibility
 * regression tests over the REAL HTTP surface with REAL JWTs against
 * disposable MySQL running the actual Flyway migrations (V1..V11).
 *
 * <p>
 * Covers:
 * <ul>
 *   <li>all 13 lifecycle statuses are accepted and persisted</li>
 *   <li>legacy statuses (INTERVIEWING, ACCEPTED) survive the V11 ENUM
 *       widening — existing records remain valid</li>
 *   <li>create defaults (status DRAFT, no status field in the create
 *       contract)</li>
 *   <li>the visibility-preservation fix: an omitted {@code visible} on PUT
 *       preserves the stored value; explicit true/false applies</li>
 *   <li>extended data model end-to-end persistence</li>
 *   <li>validation (invalid status, invalid email, bad URL)</li>
 *   <li>archived applications remain retrievable</li>
 * </ul>
 */
@Transactional
public class JobApplicationLifecycleIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String BASE = "/api/v1/me/job-applications";

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    private String jwt;
    private String email;

    @BeforeEach
    void seedUser() throws Exception {
        String suffix = runSuffix();
        email = "lifecycle-" + suffix + "@example.com";
        User user = seedUser(email, "lifecycle-" + suffix, true);
        assertThat(user.getId()).isNotNull();
        jwt = realLogin(email, PASSWORD);
    }

    private String createApplication(String title, String company) throws Exception {
        String body = """
                {"jobTitle": "%s", "companyName": "%s"}
                """.formatted(title, company);

        MvcResult result = mockMvc.perform(post(BASE)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("uuid").asText();
    }

    private String putJson(String uuid, String body) throws Exception {
        MvcResult result = mockMvc.perform(put(BASE + "/{uuid}", uuid)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------
    // Lifecycle statuses
    // ------------------------------------------------------------------

    @Test
    void allThirteenLifecycleStatuses_areAcceptedAndPersisted() throws Exception {
        String[] statuses = {
                "INTERESTED", "PREPARING", "DRAFT", "APPLIED", "ACKNOWLEDGED",
                "INTERVIEW_SCHEDULED", "INTERVIEW_COMPLETED", "TECHNICAL_ASSESSMENT",
                "OFFER", "REJECTED", "WITHDRAWN", "NO_RESPONSE", "ARCHIVED"
        };

        for (int i = 0; i < statuses.length; i++) {
            String uuid = createApplication("Role " + i, "Status Co " + i);
            putJson(uuid, "{\"status\": \"" + statuses[i] + "\"}");

            // Persisted: read back over HTTP AND straight from the database
            mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(statuses[i]));

            JobApplication entity = jobApplicationRepository.findByUuid(UUID.fromString(uuid)).orElseThrow();
            assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.valueOf(statuses[i]));
        }
    }

    @Test
    void legacyStatuses_surviveV11EnumWidening() throws Exception {
        // Simulates a pre-8.4 row: written directly with a legacy status,
        // then read through the API. V11's ENUM superset must keep it valid.
        User owner = userRepository.findByEmail(email).orElseThrow();
        JobApplication legacy = JobApplication.builder()
                .jobTitle("Legacy Interviewing Role")
                .companyName("Legacy Co")
                .status(ApplicationStatus.INTERVIEWING)
                .statusUpdatedAt(LocalDateTime.now())
                .owner(owner)
                .build();
        jobApplicationRepository.save(legacy);

        JobApplication legacyAccepted = JobApplication.builder()
                .jobTitle("Legacy Accepted Role")
                .companyName("Legacy Co")
                .status(ApplicationStatus.ACCEPTED)
                .statusUpdatedAt(LocalDateTime.now())
                .owner(owner)
                .build();
        jobApplicationRepository.save(legacyAccepted);

        mockMvc.perform(get(BASE + "/{uuid}", legacy.getUuid().toString()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEWING"));

        mockMvc.perform(get(BASE + "/{uuid}", legacyAccepted.getUuid().toString()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // And a legacy-status row can still be updated to a new lifecycle value
        putJson(legacy.getUuid().toString(), "{\"status\": \"INTERVIEW_SCHEDULED\"}");
        mockMvc.perform(get(BASE + "/{uuid}", legacy.getUuid().toString()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"));
    }

    @Test
    void invalidStatus_isRejected() throws Exception {
        String uuid = createApplication("Bad Status Role", "Validation Co");

        mockMvc.perform(put(BASE + "/{uuid}", uuid)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"NOT_A_REAL_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDefaults_toDraftWithoutStatusField() throws Exception {
        // The create contract has NO status field (TRK-400 regression:
        // sending one was the old broken contract). The service must default
        // new applications to DRAFT.
        String uuid = createApplication("Default Role", "Default Co");

        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.jobTitle").value("Default Role"));
    }

    // ------------------------------------------------------------------
    // Visibility-preservation fix (Phase 8.4 defect)
    // ------------------------------------------------------------------

    @Test
    void putOmittingVisible_preservesStoredVisibility() throws Exception {
        String uuid = createApplication("Visibility Role", "Visibility Co");

        // Explicitly hide first
        putJson(uuid, "{\"visible\": false}");
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visible").value(false));

        // THE FIX: a PUT that updates ONLY the title must NOT flip visibility
        putJson(uuid, "{\"jobTitle\": \"Renamed Role\"}");
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobTitle").value("Renamed Role"))
                .andExpect(jsonPath("$.data.visible").value(false));

        // Same for a company-only and a status-only update
        putJson(uuid, "{\"companyName\": \"Renamed Co\"}");
        putJson(uuid, "{\"status\": \"APPLIED\"}");
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visible").value(false))
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    void putExplicitVisible_trueAndFalse_apply() throws Exception {
        String uuid = createApplication("Explicit Visibility", "Visibility Co");

        putJson(uuid, "{\"visible\": true}");
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visible").value(true));

        putJson(uuid, "{\"visible\": false}");
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visible").value(false));
    }

    // ------------------------------------------------------------------
    // Extended data model
    // ------------------------------------------------------------------

    @Test
    void extendedFields_persistEndToEnd() throws Exception {
        String uuid = createApplication("Full Data Role", "Data Co");

        String updateBody = """
                {
                  "jobDescription": "Build distributed systems.",
                  "workArrangement": "HYBRID",
                  "discoveredAt": "2026-01-05",
                  "deadlineAt": "2026-02-28",
                  "followUpAt": "2026-03-10",
                  "interviewAt": "2026-03-01T10:30:00",
                  "assessmentAt": "2026-02-20",
                  "recruiterName": "Jane Recruiter",
                  "recruiterEmail": "jane@dataco.com",
                  "contactDetails": "+27 82 555 0100",
                  "salaryRange": "R45k - R55k",
                  "salaryCurrency": "ZAR",
                  "salaryPeriod": "MONTHLY"
                }
                """;
        putJson(uuid, updateBody);

        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobDescription").value("Build distributed systems."))
                .andExpect(jsonPath("$.data.workArrangement").value("HYBRID"))
                .andExpect(jsonPath("$.data.discoveredAt").value("2026-01-05"))
                .andExpect(jsonPath("$.data.deadlineAt").value("2026-02-28"))
                .andExpect(jsonPath("$.data.followUpAt").value("2026-03-10"))
                .andExpect(jsonPath("$.data.interviewAt").value(org.hamcrest.Matchers.containsString("2026-03-01T10:30:00")))
                .andExpect(jsonPath("$.data.assessmentAt").value("2026-02-20"))
                .andExpect(jsonPath("$.data.recruiterName").value("Jane Recruiter"))
                .andExpect(jsonPath("$.data.recruiterEmail").value("jane@dataco.com"))
                .andExpect(jsonPath("$.data.contactDetails").value("+27 82 555 0100"))
                .andExpect(jsonPath("$.data.salaryRange").value("R45k - R55k"))
                .andExpect(jsonPath("$.data.salaryCurrency").value("ZAR"))
                .andExpect(jsonPath("$.data.salaryPeriod").value("MONTHLY"));

        // Database-level persistence check
        JobApplication entity = jobApplicationRepository.findByUuid(UUID.fromString(uuid)).orElseThrow();
        assertThat(entity.getDeadlineAt()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(entity.getWorkArrangement()).isEqualTo("HYBRID");
    }

    @Test
    void extendedFieldValidation_rejectsBadEmailAndUrl() throws Exception {
        String uuid = createApplication("Validation Role", "Validation Co");

        mockMvc.perform(put(BASE + "/{uuid}", uuid)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recruiterEmail\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(BASE + "/{uuid}", uuid)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicationUrl\": \"not-a-url\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Archived retrieval
    // ------------------------------------------------------------------

    @Test
    void archivedApplications_remainRetrievable() throws Exception {
        String uuid = createApplication("Archived Role", "Archive Co");

        putJson(uuid, "{\"status\": \"ARCHIVED\"}");

        // Archived applications are NOT deleted: still in the list...
        MvcResult list = mockMvc.perform(get(BASE).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(list.getResponse().getContentAsString()).contains("Archived Role");

        // ...and retrievable directly
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // ...and can be un-archived (no restrictive transition graph)
        putJson(uuid, "{\"status\": \"APPLIED\"}");
        mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    // ------------------------------------------------------------------
    // Status-change bookkeeping
    // ------------------------------------------------------------------

    @Test
    void statusChange_updatesStatusUpdatedAt() throws Exception {
        String uuid = createApplication("Bookkeeping Role", "Timestamp Co");

        MvcResult before = mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        String statusUpdatedAtBefore = objectMapper.readTree(before.getResponse().getContentAsString())
                .path("data").path("statusUpdatedAt").asText();

        Thread.sleep(50); // ensure the timestamp can differ

        putJson(uuid, "{\"status\": \"OFFER\"}");

        MvcResult after = mockMvc.perform(get(BASE + "/{uuid}", uuid).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        String statusUpdatedAtAfter = objectMapper.readTree(after.getResponse().getContentAsString())
                .path("data").path("statusUpdatedAt").asText();

        assertThat(statusUpdatedAtAfter).isNotEqualTo(statusUpdatedAtBefore);
    }
}
