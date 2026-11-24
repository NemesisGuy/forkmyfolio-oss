package com.forkmyfolio.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.SkillRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Backup/restore flow over the REAL HTTP surface with real commit semantics
 * (NOTE: deliberately NOT @Transactional — restore/backup must see exactly
 * what production transactions see; a test-managed transaction can hide
 * committed data behind its snapshot).
 *
 * <p>
 * Password re-verification issues a short-lived backup-scoped token
 * (X-Backup-Token); without it, backup/restore is denied. Restoring a
 * downloaded backup must reproduce the user's data — and ONLY that user's
 * data (restore is scoped to the current user; other users must survive).
 */
public class BackupRestoreIntegrationTest extends AbstractMySqlIntegrationTest {

        private static final String PASSWORD = "Password123!";

        private String email;
        private String slug;
        private String jwt;
        private String projectTitle;

        @Autowired
        private SkillRepository skillRepository;

        @BeforeEach
        void seedUserWithContent() throws Exception {
                String suffix = runSuffix();
                email = "backup-user-" + suffix + "@example.com";
                slug = "backup-user-" + suffix;
                projectTitle = "Backup Project " + suffix;

                seedUser(email, slug, true);
                jwt = realLogin(email, PASSWORD);

                mockMvc.perform(post("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "%s", "description": "Project saved into backup %s",
                                                 "visible": true, "displayOrder": 0, "skills": [],
                                                 "keyFeatures": ["Backup-proof feature one", "Backup-proof feature two"]}
                                                """.formatted(projectTitle, suffix)))
                                .andExpect(status().isCreated());
        }

        private String obtainBackupToken() throws Exception {
                return obtainBackupToken(jwt, PASSWORD);
        }

        /** Obtains a backup-scoped token for any logged-in user (admin flows). */
        private String obtainBackupToken(String userJwt, String password) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/v1/auth/verify-password")
                                .header("Authorization", "Bearer " + userJwt)
                                .contentType("application/json")
                                .content("{\"password\":\"" + password + "\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.valid").value(true))
                                .andExpect(jsonPath("$.data.backupToken").isNotEmpty())
                                .andReturn();
                return objectMapper.readTree(result.getResponse().getContentAsString())
                                .path("data").path("backupToken").asText();
        }

        @Test
        void downloadBackup_requiresAndHonoursBackupToken() throws Exception {
                // Wrong password -> 401 (BadCredentials handle)
                mockMvc.perform(post("/api/v1/auth/verify-password")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("{\"password\":\"NotTheRightOne1!\"}"))
                                .andExpect(status().isUnauthorized());

                // Download without re-verification -> 403
                mockMvc.perform(get("/api/v1/me/backup").header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isForbidden());

                // With re-verification -> the real backup JSON
                String backupToken = obtainBackupToken();
                MvcResult download = mockMvc.perform(get("/api/v1/me/backup")
                                .header("Authorization", "Bearer " + jwt)
                                .header("X-Backup-Token", backupToken))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode backupFile = objectMapper.readTree(download.getResponse().getContentAsByteArray());
                assertThat(backupFile.path("meta").path("type").asText()).isEqualTo("user_backup");
                JsonNode portfolioData = backupFile.path("data");
                assertThat(portfolioData.isObject()).isTrue();
                assertThat(portfolioData.path("projects").isArray()).isTrue();

                boolean found = false;
                for (var node : portfolioData.path("projects")) {
                        if (projectTitle.equals(node.path("title").asText())) {
                                found = true;
                                // Phase 7.4 (§6): structured key features ride in the
                                // backup DTO in display order
                                assertThat(node.path("keyFeatures").toString())
                                                .isEqualTo("[\"Backup-proof feature one\",\"Backup-proof feature two\"]");
                        }
                }
                assertThat(found)
                                .as("the backup must contain the user's real content")
                                .isTrue();
        }

        @Test
        void restore_roundTrip_isolatedToRestoringUser() throws Exception {
                // A second, innocent user whose data restore must NOT touch:
                // porter restore is strictly user-scoped (performRestore on the
                // authenticated user only; the admin system-restore path is the
                // only flow allowed to wipe).
                String suffix = runSuffix();
                String bystanderEmail = "backup-bystander-" + suffix + "@example.com";
                seedUser(bystanderEmail, "backup-bystander-" + suffix, true);

                String backupToken = obtainBackupToken();
                MvcResult download = mockMvc.perform(get("/api/v1/me/backup")
                                .header("Authorization", "Bearer " + jwt)
                                .header("X-Backup-Token", backupToken))
                                .andExpect(status().isOk())
                                .andReturn();
                byte[] backupJson = download.getResponse().getContentAsByteArray();

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "forkmyfolio-backup.json",
                                "application/json",
                                backupJson);

                // Restore without re-verification -> 403
                mockMvc.perform(multipart("/api/v1/me/backup/restore").file(file)
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isForbidden());

                // Restore WITH re-verification -> 204
                mockMvc.perform(multipart("/api/v1/me/backup/restore").file(file)
                                .header("Authorization", "Bearer " + jwt)
                                .header("X-Backup-Token", backupToken))
                                .andExpect(status().isNoContent());

                // ISOLATION: the innocent bystander is untouched — their login
                // still succeeds after the restoring user's restore.
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content("{\"email\": \"" + bystanderEmail + "\", \"password\": \""
                                                + PASSWORD + "\"}"))
                                .andExpect(status().isOk());

                // The restoring user's data is re-imported: verify via a fresh
                // login (the restore re-creates rows; the old JWT's user id may
                // not survive the wipe-and-recreate).
                String freshJwt = realLogin(email, PASSWORD);
                MvcResult afterRestore = mockMvc.perform(get("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + freshJwt))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(afterRestore.getResponse().getContentAsString())
                                .contains(projectTitle);

                // Phase 7.4 (§6): key features survive the export -> restore
                // round trip with original order intact (per-row uuids are
                // regenerated; the restored list must match the seeded order)
                MvcResult keyFeaturesAfterRestore = mockMvc.perform(get("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + freshJwt))
                                .andExpect(status().isOk())
                                .andReturn();
                for (var node : objectMapper.readTree(keyFeaturesAfterRestore.getResponse().getContentAsString()).path("data")) {
                        if (projectTitle.equals(node.path("title").asText())) {
                                assertThat(node.path("keyFeatures").toString())
                                                .isEqualTo("[\"Backup-proof feature one\",\"Backup-proof feature two\"]");
                        }
                }
        }

        /**
         * Phase 10 (Workstream 4) round trip: per-user setting overrides and
         * per-skill visibility must survive export -> restore. Previously
         * settings were not exported at all and restore forced every skill
         * visible again.
         */
        @Test
        void restore_roundTrip_preservesSettingsAndSkillVisibility() throws Exception {
                String suffix = runSuffix();

                // 1. Add a skill to the portfolio, then hide it
                MvcResult created = mockMvc.perform(post("/api/v1/me/skills")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"name": "Backup Visibility Probe %s", "category": "Tool",
                                                 "level": "INTERMEDIATE", "visible": true,
                                                 "description": "Skill used to verify visibility survives restore."}
                                                """.formatted(suffix)))
                                .andExpect(status().isCreated())
                                .andReturn();
                String userSkillId = objectMapper.readTree(created.getResponse().getContentAsString())
                                .path("data").path("userSkillId").asText();

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/v1/me/skills/" + userSkillId)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"level": "INTERMEDIATE", "visible": false,
                                                 "description": "Skill used to verify visibility survives restore."}
                                                """))
                                .andExpect(status().isOk());

                // 2. Toggle one user setting override to "false"
                MvcResult settingsBefore = mockMvc.perform(get("/api/v1/me/settings")
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();
                String settingUuid = null;
                String settingName = null;
                for (var node : objectMapper.readTree(settingsBefore.getResponse().getContentAsString()).path("data")) {
                        if ("true".equals(node.path("value").asText())) {
                                settingUuid = node.path("uuid").asText();
                                settingName = node.path("name").asText();
                                break;
                        }
                }
                assertThat(settingUuid).as("at least one setting must default to true to toggle").isNotNull();
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/v1/me/settings")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("[{\"uuid\": \"" + settingUuid + "\", \"value\": \"false\"}]"))
                                .andExpect(status().isOk());

                // 3. Export the backup; the settings array must carry the override
                String backupToken = obtainBackupToken();
                MvcResult download = mockMvc.perform(get("/api/v1/me/backup")
                                .header("Authorization", "Bearer " + jwt)
                                .header("X-Backup-Token", backupToken))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode backupFile = objectMapper.readTree(download.getResponse().getContentAsByteArray());
                JsonNode settingsInBackup = backupFile.path("data").path("settings");
                assertThat(settingsInBackup.isArray()).isTrue();
                boolean overridePresent = false;
                for (var node : settingsInBackup) {
                        if (settingName.equals(node.path("name").asText())) {
                                overridePresent = "false".equals(node.path("value").asText());
                        }
                }
                assertThat(overridePresent)
                                .as("the backup must carry the user's setting override")
                                .isTrue();

                // 4. Restore the backup
                MockMultipartFile file = new MockMultipartFile(
                                "file", "forkmyfolio-backup.json", "application/json",
                                download.getResponse().getContentAsByteArray());
                mockMvc.perform(multipart("/api/v1/me/backup/restore").file(file)
                                .header("Authorization", "Bearer " + jwt)
                                .header("X-Backup-Token", backupToken))
                                .andExpect(status().isNoContent());

                // 5. Verify after restore: skill still hidden, override still "false"
                String freshJwt = realLogin(email, PASSWORD);
                MvcResult skillsAfter = mockMvc.perform(get("/api/v1/me/skills")
                                .header("Authorization", "Bearer " + freshJwt))
                                .andExpect(status().isOk())
                                .andReturn();
                boolean hiddenSurvived = false;
                for (var node : objectMapper.readTree(skillsAfter.getResponse().getContentAsString()).path("data")) {
                        if (("Backup Visibility Probe " + suffix).equals(node.path("name").asText())) {
                                hiddenSurvived = !node.path("visible").asBoolean();
                        }
                }
                assertThat(hiddenSurvived)
                                .as("the skill hidden before the backup must still be hidden after restore")
                                .isTrue();

                MvcResult settingsAfter = mockMvc.perform(get("/api/v1/me/settings")
                                .header("Authorization", "Bearer " + freshJwt))
                                .andExpect(status().isOk())
                                .andReturn();
                boolean overrideSurvived = false;
                for (var node : objectMapper.readTree(settingsAfter.getResponse().getContentAsString()).path("data")) {
                        if (settingName.equals(node.path("name").asText())) {
                                overrideSurvived = "false".equals(node.path("value").asText());
                        }
                }
                assertThat(overrideSurvived)
                                .as("the setting override must survive the restore round trip")
                                .isTrue();
        }

        /**
         * KNOWN_ISSUES &#167;10: a system backup must carry the GLOBAL skills
         * catalogue, and a system restore must rebuild it exactly —
         * unreferenced catalogue rows, catalogue-owned metadata (category,
         * icon, description, default level) and the admin {@code visible}
         * override all survive the round trip. Legacy (pre-catalogue) backup
         * files must still restore, with the old on-demand skill recreation.
         *
         * <p>System restore is destructive by nature, so this test is
         * self-healing: it ends by restoring the very file it downloaded,
         * leaving users and the catalogue exactly as it found them.
         */
        @Test
        void systemBackup_roundTrip_preservesGlobalSkillCatalogue() throws Exception {
                String suffix = runSuffix();
                String markerName = "Phase034 Catalogue Probe " + suffix;

                // A dedicated admin for this test — self-contained auth, no
                // dependence on bootstrap credentials or on other classes.
                String adminEmail = "backup-sysadmin-" + suffix + "@example.com";
                seedUser(adminEmail, "backup-sysadmin-" + suffix, true, Role.ADMIN);

                // Backup fidelity pins (import-direction gap surfaced by the
                // §10 UAT): a known password-change timestamp and the seeded
                // account creation time must both survive the round trip —
                // otherwise the UI forces a password change after restore.
                User adminEntity = userRepository.findByEmail(adminEmail).orElseThrow();
                adminEntity.setPasswordLastChangedAt(Instant.parse("2026-11-20T10:15:30.123456Z"));
                userRepository.save(adminEntity);
                adminEntity = userRepository.findByEmail(adminEmail).orElseThrow();
                Instant originalPasswordChangedAt = adminEntity.getPasswordLastChangedAt();
                Instant originalCreatedAt = adminEntity.getCreatedAt();

                String adminJwt = realLogin(adminEmail, PASSWORD);

                // A catalogue row NO user references, carrying admin state and
                // metadata that no seed file could reconstruct after a loss.
                Skill marker = new Skill();
                marker.setName(markerName);
                marker.setCategory("QA Probes");
                marker.setIcon("icon-phase034");
                marker.setDescription("Catalogue fidelity probe for KNOWN_ISSUES §10.");
                marker.setVisible(false);
                marker.setLevel(com.forkmyfolio.model.enums.SkillLevel.EXPERT);
                skillRepository.save(marker);
                long catalogueSize = skillRepository.count();

                // 1. The download carries the whole catalogue, marker included,
                //    with every field intact.
                String backupToken = obtainBackupToken(adminJwt, PASSWORD);
                MvcResult download = mockMvc.perform(get("/api/v1/admin/backup")
                                .header("Authorization", "Bearer " + adminJwt)
                                .header("X-Backup-Token", backupToken))
                                .andExpect(status().isOk())
                                .andReturn();
                byte[] fullBackupBytes = download.getResponse().getContentAsByteArray();
                JsonNode backupFile = objectMapper.readTree(fullBackupBytes);
                JsonNode skills = backupFile.path("skills");
                assertThat(skills.isArray())
                                .as("the system backup must carry the skills catalogue")
                                .isTrue();
                assertThat(skills.size())
                                .as("the catalogue must be exported in full")
                                .isEqualTo((int) catalogueSize);
                JsonNode markerNode = null;
                for (JsonNode node : skills) {
                        if (markerName.equals(node.path("name").asText())) {
                                markerNode = node;
                        }
                }
                assertThat(markerNode)
                                .as("the unreferenced catalogue row must ride in the backup")
                                .isNotNull();
                assertThat(markerNode.path("uuid").asText()).isEqualTo(marker.getUuid().toString());
                assertThat(markerNode.path("category").asText()).isEqualTo("QA Probes");
                assertThat(markerNode.path("icon").asText()).isEqualTo("icon-phase034");
                assertThat(markerNode.path("description").asText())
                                .isEqualTo("Catalogue fidelity probe for KNOWN_ISSUES §10.");
                assertThat(markerNode.path("level").asText()).isEqualTo("EXPERT");
                assertThat(markerNode.path("visible").asBoolean())
                                .as("the admin visible override must be exported")
                                .isFalse();

                // 2. LEGACY COMPAT: the same file with the skills array
                //    stripped (the pre-§10 format) must still restore with 204.
                //    Old behaviour applies — only skills user data references
                //    come back — so the unreferenced marker stays gone.
                com.fasterxml.jackson.databind.node.ObjectNode legacyFile = objectMapper.createObjectNode();
                legacyFile.set("meta", backupFile.path("meta"));
                legacyFile.set("data", backupFile.path("data"));
                restoreSystemAsAdmin(adminJwt, objectMapper.writeValueAsBytes(legacyFile));
                assertThat(findSkillByName(markerName))
                                .as("legacy files carry no catalogue: the unreferenced marker is not resurrected")
                                .isNull();

                // 3. NEW FORMAT: restoring the full file rebuilds the
                //    catalogue exactly. Fresh auth first — the restore wiped
                //    and recreated the users, so the old JWT/token identity
                //    no longer resolves.
                String adminJwt2 = realLogin(adminEmail, PASSWORD);
                restoreSystemAsAdmin(adminJwt2, fullBackupBytes);

                Skill restored = findSkillByName(markerName);
                assertThat(restored).as("the catalogue probe must be resurrected").isNotNull();
                assertThat(restored.getUuid())
                                .as("catalogue identity must survive the round trip")
                                .isEqualTo(marker.getUuid());
                assertThat(restored.getCategory()).isEqualTo("QA Probes");
                assertThat(restored.getIcon()).isEqualTo("icon-phase034");
                assertThat(restored.getDescription())
                                .isEqualTo("Catalogue fidelity probe for KNOWN_ISSUES §10.");
                assertThat(restored.getLevel())
                                .isEqualTo(com.forkmyfolio.model.enums.SkillLevel.EXPERT);
                assertThat(restored.isVisible())
                                .as("the admin visible override must survive restore")
                                .isFalse();
                assertThat(skillRepository.count())
                                .as("catalogue size must match the pre-restore state")
                                .isEqualTo(catalogueSize);

                // Fidelity: the restored admin keeps its password-change
                // timestamp (no forced password change) and its original
                // account creation time.
                User restoredAdmin = userRepository.findByEmail(adminEmail).orElseThrow();
                assertThat(restoredAdmin.getPasswordLastChangedAt())
                                .as("password-change timestamp must survive restore")
                                .isEqualTo(originalPasswordChangedAt);
                assertThat(restoredAdmin.getCreatedAt())
                                .as("account creation time must survive restore")
                                .isEqualTo(originalCreatedAt);
        }

        /** Performs a full system restore as admin with fresh re-verification. */
        private void restoreSystemAsAdmin(String adminJwt, byte[] backupBytes) throws Exception {
                String token = obtainBackupToken(adminJwt, PASSWORD);
                MockMultipartFile file = new MockMultipartFile(
                                "file", "forkmyfolio-system-backup.json", "application/json", backupBytes);
                mockMvc.perform(multipart("/api/v1/admin/backup/restore/system").file(file)
                                .header("Authorization", "Bearer " + adminJwt)
                                .header("X-Backup-Token", token))
                                .andExpect(status().isNoContent());
        }

        private Skill findSkillByName(String name) {
                return skillRepository.findByName(name).orElse(null);
        }
}
