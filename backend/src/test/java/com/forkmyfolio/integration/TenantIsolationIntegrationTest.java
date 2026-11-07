package com.forkmyfolio.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.model.enums.ApplicationStatus;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.repository.ExperienceRepository;
import com.forkmyfolio.repository.JobApplicationRepository;
import com.forkmyfolio.repository.SkillRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.repository.UserSkillRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tenant isolation (IDOR) over the REAL HTTP surface with REAL JWTs:
 * User A and User B are both authenticated users; B must never read, mutate
 * or destroy A's data, neither via resource UUIDs nor via "my X" listings.
 */
@Transactional
public class TenantIsolationIntegrationTest extends AbstractMySqlIntegrationTest {

        private static final String PASSWORD = "Password123!";

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private ExperienceRepository experienceRepository;
        @Autowired
        private SkillRepository skillRepository;
        @Autowired
        private UserSkillRepository userSkillRepository;
        @Autowired
        private JobApplicationRepository jobApplicationRepository;

        private String userAEmail;
        private String userASlug;
        private String userAJwt;

        private String userBEmail;
        private String userBSlug;
        private String userBJwt;

        @BeforeEach
        void seedTwoUsers() throws Exception {
                String suffix = runSuffix();
                userAEmail = "tenant-a-" + suffix + "@example.com";
                userASlug = "tenant-a-" + suffix;
                userBEmail = "tenant-b-" + suffix + "@example.com";
                userBSlug = "tenant-b-" + suffix;

                User userA = seedUser(userAEmail, userASlug, true);
                User userB = seedUser(userBEmail, userBSlug, true);
                assertThat(userA.getId()).isNotNull();
                assertThat(userB.getId()).isNotNull();

                userAJwt = realLogin(userAEmail, PASSWORD);
                userBJwt = realLogin(userBEmail, PASSWORD);
        }

        private String createProjectFor(String jwt, String title, int displayOrder, boolean visible) throws Exception {
                String body = """
                                {"title": "%s", "description": "Tenant isolation project %s",
                                 "visible": %s, "displayOrder": %d, "skills": []}
                                """.formatted(title, title, visible, displayOrder);

                mockMvc.perform(post("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content(body))
                                .andExpect(status().isCreated());

                MvcResult list = mockMvc.perform(get("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();
                var tree = objectMapper.readTree(list.getResponse().getContentAsString()).path("data");
                for (var node : tree) {
                        if (title.equals(node.path("title").asText())) {
                                return node.path("uuid").asText();
                        }
                }
                throw new AssertionError("created project not found in listing");
        }

        @Test
        void userB_cannotReadOrMutate_userAProject() throws Exception {
                String titleA = "Project-Of-A";
                String uuidA = createProjectFor(userAJwt, titleA, 0, true);

                // --- READ isolation -------------------------------------------------
                // B's own listing must not contain A's project
                MvcResult bList = mockMvc.perform(get("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(bList.getResponse().getContentAsString()).doesNotContain(titleA);

                // Direct-uuid read by B -> 404, NOT 403: Phase 7.4 (§403/404
                // policy) conceals cross-tenant existence. A foreign uuid is
                // indistinguishable from a nonexistent one on EVERY direct-uuid
                // path (read, update, delete); probing must never yield an
                // existence oracle.
                mockMvc.perform(get("/api/v1/me/projects/{uuid}", uuidA)
                                .header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isNotFound());

                // --- MUTATION isolation ---------------------------------------------
                // B tries to update A's project -> also 404 (same concealment;
                // previously 403, which revealed that the uuid existed).
                mockMvc.perform(put("/api/v1/me/projects/{uuid}", uuidA)
                                .header("Authorization", "Bearer " + userBJwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "Hijacked-By-B", "description": "attempted unauthorized update",
                                                 "visible": true, "displayOrder": 0, "skills": []}
                                                """))
                                .andExpect(status().isNotFound());

                // B tries to delete A's project -> also 404 (own-uuid-miss and
                // cross-tenant deletion are indistinguishable).
                mockMvc.perform(delete("/api/v1/me/projects/{uuid}", uuidA)
                                .header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isNotFound());

                // --- STATE VERIFICATION ----------------------------------------------
                // A still owns an unmodified project
                mockMvc.perform(get("/api/v1/me/projects/{uuid}", uuidA)
                                .header("Authorization", "Bearer " + userAJwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.title").value(titleA))
                                .andExpect(jsonPath("$.data.description").isString())
                                .andExpect(jsonPath("$.data.description").value(
                                                org.hamcrest.Matchers.containsString("Tenant isolation")));
        }

        @Test
        void crossTenant_otherResourceTypes_areConcealedAsNotFound() throws Exception {
                // Phase 7.4 (§403/404 policy sweep): EVERY direct-uuid resource
                // type must conceal cross-tenant existence as 404 — never 403 —
                // so uuid probing never yields an existence oracle.
                User userA = userRepository.findByEmail(userAEmail).orElseThrow();

                Experience experience = new Experience();
                experience.setUser(userA);
                experience.setJobTitle("Architect");
                experience.setCompanyName("Company A");
                experience.setStartDate(java.time.LocalDate.of(2020, 1, 1));
                experienceRepository.save(experience);

                Skill skill = new Skill();
                skill.setName("Uncommon-Skill-" + runSuffix());
                skillRepository.save(skill);
                UserSkill userSkill = new UserSkill();
                userSkill.setUser(userA);
                userSkill.setSkill(skill);
                userSkill.setLevel(SkillLevel.ADVANCED);
                userSkillRepository.save(userSkill);

                JobApplication application = new JobApplication();
                application.setOwner(userA);
                application.setJobTitle("Platform Engineer");
                application.setCompanyName("Company A");
                application.setStatus(ApplicationStatus.APPLIED);
                application.setStatusUpdatedAt(java.time.LocalDateTime.now());
                jobApplicationRepository.save(application);

                // B probes all three of A's uuids -> all 404, never 403.
                mockMvc.perform(get("/api/v1/me/experiences/{uuid}", experience.getUuid())
                                .header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isNotFound());

                mockMvc.perform(get("/api/v1/me/skills/{uuid}", userSkill.getUuid())
                                .header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isNotFound());

                mockMvc.perform(get("/api/v1/me/job-applications/{uuid}", application.getUuid())
                                .header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isNotFound());
        }

        @Test
        void me_profileEndpoints_returnOnlyOwnData() throws Exception {
                // Each user sees exactly their own profile data
                mockMvc.perform(get("/api/v1/me/profile").header("Authorization", "Bearer " + userAJwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.headline").value("Headline of " + userASlug));

                mockMvc.perform(get("/api/v1/me/profile").header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.headline").value("Headline of " + userBSlug));

                // B's own "me" identity is B, never A
                mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value(userBEmail))
                                .andExpect(jsonPath("$.data.slug").value(userBSlug));
        }

        @Test
        void contactMessages_areScopedToTheirOwner() throws Exception {
                // Anonymous visitor submits a contact message to A's public portfolio
                mockMvc.perform(post("/api/v1/portfolios/{slug}/contact-messages", userASlug)
                                .contentType("application/json")
                                .content("""
                                                {"name": "Curious Visitor", "email": "visitor@example.com",
                                                 "message": "Hello A, I would like to hire you."}
                                                """))
                                .andExpect(status().is2xxSuccessful());

                // A sees it in their inbox ...
                MvcResult aInbox = mockMvc.perform(get("/api/v1/me/contact-messages")
                                .header("Authorization", "Bearer " + userAJwt))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(aInbox.getResponse().getContentAsString())
                                .contains("Hello A, I would like to hire you.");

                // ... B must NOT see A's inbox content
                MvcResult bInbox = mockMvc.perform(get("/api/v1/me/contact-messages")
                                .header("Authorization", "Bearer " + userBJwt))
                                .andExpect(status().isOk())
                                .andReturn();
                String bInboxBody = bInbox.getResponse().getContentAsString();
                assertThat(bInboxBody).doesNotContain("Hello A, I would like to hire you.");
                assertThat(bInboxBody).doesNotContain(userASlug);
        }
}
