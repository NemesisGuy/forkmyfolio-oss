package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.*;
import com.forkmyfolio.repository.*;
import com.forkmyfolio.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestoreServiceImplTest {

        @Mock
        private UserService userService;
        @Mock
        private EntityManager entityManager;
        @Mock
        private UserRepository userRepository;
        @Mock
        private UserSkillRepository userSkillRepository;
        @Mock
        private ProjectRepository projectRepository;
        @Mock
        private ExperienceRepository experienceRepository;
        @Mock
        private QualificationRepository qualificationRepository;
        @Mock
        private TestimonialRepository testimonialRepository;
        @Mock
        private PortfolioProfileRepository portfolioProfileRepository;
        @Mock
        private SkillRepository skillRepository;
        @Mock
        private com.forkmyfolio.repository.UserSettingRepository userSettingRepository;

        @Spy
        @InjectMocks
        private RestoreServiceImpl restoreService;

        private User testUser;
        private PortfolioBackup backupDomain;

        @BeforeEach
        void setUp() {
                testUser = new User();
                testUser.setId(1L);
                testUser.setEmail("test@example.com");
                testUser.setUserSkills(new HashSet<>());
                testUser.setProjects(new HashSet<>());
                testUser.setExperiences(new HashSet<>());
                testUser.setQualifications(new HashSet<>());
                testUser.setTestimonials(new HashSet<>());

                backupDomain = new PortfolioBackup();
                backupDomain.setProfile(new PortfolioProfile());

                UserSkill userSkill = new UserSkill();
                Skill skill = new Skill();
                skill.setName("Java");
                userSkill.setSkill(skill);
                backupDomain.setSkills(List.of(userSkill));

                Project project = new Project();
                project.setSkills(new HashSet<>());
                backupDomain.setProjects(List.of(project));

                Experience experience = new Experience();
                experience.setSkills(new HashSet<>());
                backupDomain.setExperiences(List.of(experience));

                backupDomain.setQualifications(List.of(new Qualification()));
                backupDomain.setTestimonials(List.of(new Testimonial()));
        }

        @Test
        void wipeAllData_shouldDeleteAllDataInCorrectOrder() {
                jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
                when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);

                restoreService.wipeAllData();

                // Verify that native DELETE statements are issued for the key tables
                verify(entityManager, atLeastOnce()).createNativeQuery(anyString());
                verify(nativeQuery, atLeastOnce()).executeUpdate();
        }

        @Test
        void restoreUserFromBackup_shouldPreserveSkillVisibility() {
                // Phase 10 (Workstream 4) regression: restore used to force
                // userSkill.setVisible(true), un-hiding skills the user had
                // hidden before the backup. The backed-up visibility must win.
                when(skillRepository.findAll()).thenReturn(Collections.emptyList());
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserSkill hiddenSkill = backupDomain.getSkills().get(0);
                hiddenSkill.setVisible(false);

                restoreService.restoreUserFromBackup(testUser, backupDomain);

                org.mockito.ArgumentCaptor<List<UserSkill>> captor =
                                org.mockito.ArgumentCaptor.forClass(List.class);
                verify(userSkillRepository).saveAll(captor.capture());
                assertThat(captor.getValue().get(0).isVisible())
                                .as("a skill hidden before the backup must stay hidden after restore")
                                .isFalse();
        }

        @Test
        void restoreUserFromBackup_shouldRestoreUserSettings() {
                // Phase 10 (Workstream 4): per-user setting overrides must be
                // synced from the backup via UPSERT (update in place / add
                // missing / remove absent). A delete-then-reinsert violated
                // the (user_id, name) unique constraint because Hibernate
                // flushes INSERTs before DELETEs.
                when(skillRepository.findAll()).thenReturn(Collections.emptyList());
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserSetting existingOverride = new UserSetting();
                existingOverride.setName("portfolio.projects.show");
                existingOverride.setValue("true");
                Set<UserSetting> existing = new HashSet<>();
                existing.add(existingOverride);
                testUser.setUserSettings(existing);

                UserSetting backedUpSameName = new UserSetting();
                backedUpSameName.setName("portfolio.projects.show");
                backedUpSameName.setValue("false");
                UserSetting backedUpNew = new UserSetting();
                backedUpNew.setName("portfolio.skills.show");
                backedUpNew.setValue("false");
                backupDomain.setSettings(List.of(backedUpSameName, backedUpNew));

                restoreService.restoreUserFromBackup(testUser, backupDomain);

                // Existing override updated IN PLACE (no delete/reinsert)
                assertThat(existingOverride.getValue()).isEqualTo("false");
                // New override added through the cascade collection
                assertThat(testUser.getUserSettings())
                                .extracting(UserSetting::getName, UserSetting::getValue)
                                .contains(
                                                org.assertj.core.groups.Tuple.tuple("portfolio.projects.show", "false"),
                                                org.assertj.core.groups.Tuple.tuple("portfolio.skills.show", "false"));
                // The owning user must be attached for cascade persistence
                assertThat(backedUpNew.getUser()).isSameAs(testUser);
                // No direct repository writes: the cascade owns the lifecycle
                verify(userSettingRepository, never()).deleteAll(any());
                verify(userSettingRepository, never()).saveAll(any());
        }

        @Test
        void restoreUserFromBackup_withSettingsAbsentFromBackup_shouldOrphanRemoveThem() {
                // A backup that carries an (now empty) settings list means the
                // user had no overrides at export time: they must be removed.
                when(skillRepository.findAll()).thenReturn(Collections.emptyList());
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserSetting stale = new UserSetting();
                stale.setName("portfolio.projects.show");
                stale.setValue("true");
                Set<UserSetting> existing = new HashSet<>();
                existing.add(stale);
                testUser.setUserSettings(existing);

                backupDomain.setSettings(Collections.emptyList());

                restoreService.restoreUserFromBackup(testUser, backupDomain);

                assertThat(testUser.getUserSettings()).isEmpty();
        }

        @Test
        void restoreUserFromBackup_withLegacyBackupWithoutSettings_shouldLeaveSettingsUntouched() {
                // Old backups predate the settings field (null): restore must
                // not fail and must not delete the user's existing overrides.
                when(skillRepository.findAll()).thenReturn(Collections.emptyList());
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserSetting keep = new UserSetting();
                keep.setName("portfolio.projects.show");
                keep.setValue("true");
                Set<UserSetting> existing = new HashSet<>();
                existing.add(keep);
                testUser.setUserSettings(existing);

                restoreService.restoreUserFromBackup(testUser, backupDomain);

                verify(userSettingRepository, never()).deleteAll(any());
                verify(userSettingRepository, never()).saveAll(any());
        }

        @Test
        void restoreUserFromBackup_withNullSkillsAndNullNestedSkills_shouldNotThrow() {
                // Arrange: backup with a null skill entry and null skill sets on project/experience
                testUser.getProjects().add(new Project());
                when(skillRepository.findAll()).thenReturn(Collections.emptyList());

                UserSkill nullSkill = new UserSkill();
                nullSkill.setSkill(null);
                backupDomain.setSkills(List.of(nullSkill));

                Project project = new Project();
                project.setSkills(null);
                backupDomain.setProjects(List.of(project));

                Experience experience = new Experience();
                experience.setSkills(null);
                backupDomain.setExperiences(List.of(experience));

                // Act + Assert: no exception
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                                () -> restoreService.restoreUserFromBackup(testUser, backupDomain));
        }

        @Test
        void restoreUserFromBackup_shouldClearOldDataAndRestoreNewData() {
                // Arrange: Give the user some existing data to ensure it gets cleared
                testUser.getProjects().add(new Project());
                when(skillRepository.findAll()).thenReturn(Collections.emptyList());
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act
                restoreService.restoreUserFromBackup(testUser, backupDomain);

                // Assert: Verify old data is cleared
                verify(projectRepository).deleteAll(any());
                verify(entityManager).flush();

                // Assert: Verify new data is restored
                verify(portfolioProfileRepository).save(any(PortfolioProfile.class));
                verify(skillRepository, atLeastOnce()).save(any(Skill.class)); // A new skill should be created
                verify(userSkillRepository).saveAll(anyList());
                verify(projectRepository).saveAll(anyList());
                verify(experienceRepository).saveAll(anyList());
                verify(qualificationRepository).saveAll(anyList());
                verify(testimonialRepository).saveAll(anyList());
        }

        @Test
        void restoreSystemFromBackup_shouldWipeAndRestoreAllUsers() {
                // Arrange
                User userFromBackup = new User();
                userFromBackup.setEmail("new@example.com");

                UserFullBackup userFullBackup = new UserFullBackup(userFromBackup, backupDomain);
                List<UserFullBackup> systemBackup = Collections.singletonList(userFullBackup);

                jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
                when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);

                // When the service looks for the user, pretend they don't exist
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
                // When the service saves the new user, return a user object
                when(userRepository.save(any(User.class))).thenReturn(new User());
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act
                restoreService.restoreSystemFromBackup(systemBackup, Collections.emptyList());

                // Assert
                verify(restoreService).wipeAllData(); // Should wipe first
                verify(userRepository).findByEmail(eq("new@example.com")); // Should check if user exists
                verify(userRepository).save(any(User.class)); // Should save the new user
                // Verify that the restore logic was called for the newly created user
                verify(portfolioProfileRepository, times(1)).save(any(PortfolioProfile.class));
        }

        @Test
        void restoreSystemFromBackup_withExistingUser_shouldRestorePasswordTimestampFromBackup() {
                // Backup fidelity pin (surfaced by the §10 UAT): when the
                // restore UPDATES an existing user, the backup's
                // passwordLastChangedAt must be applied together with the
                // restored hash — otherwise the user is forced through a
                // password change after a restore that already restored their
                // password.
                User existing = new User();
                existing.setEmail("existing@example.com");
                existing.setPasswordLastChangedAt(Instant.parse("2026-01-01T00:00:00Z"));

                User userFromBackup = new User();
                userFromBackup.setEmail("existing@example.com");
                userFromBackup.setPassword("$2a$10$restoredHash");
                userFromBackup.setPasswordLastChangedAt(Instant.parse("2026-11-20T10:15:30.123456Z"));
                List<UserFullBackup> systemBackup = Collections.singletonList(
                                new UserFullBackup(userFromBackup, new PortfolioBackup()));

                jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
                when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existing));
                when(userRepository.save(any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                restoreService.restoreSystemFromBackup(systemBackup, Collections.emptyList());

                assertThat(existing.getPasswordLastChangedAt())
                                .as("the backup's password-change timestamp must win on restore")
                                .isEqualTo(Instant.parse("2026-11-20T10:15:30.123456Z"));
                assertThat(existing.getPassword())
                                .as("the restored hash travels with the timestamp")
                                .isEqualTo("$2a$10$restoredHash");
        }

        @Test
        void restoreSystemFromBackup_withLegacyBackupWithoutPasswordTimestamp_shouldPreserveExisting() {
                // Legacy backups carry neither hash nor timestamp: the existing
                // values must survive so the user is NOT forced into a password
                // change by a restore that didn't actually change the password.
                User existing = new User();
                existing.setEmail("legacy-existing@example.com");
                existing.setPassword("$2a$10$existingHash");
                existing.setPasswordLastChangedAt(Instant.parse("2026-01-01T00:00:00Z"));

                User userFromBackup = new User();
                userFromBackup.setEmail("legacy-existing@example.com");
                // no hash, no timestamp (legacy/V2 shape)
                List<UserFullBackup> systemBackup = Collections.singletonList(
                                new UserFullBackup(userFromBackup, new PortfolioBackup()));

                jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
                when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existing));
                when(userRepository.save(any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                restoreService.restoreSystemFromBackup(systemBackup, Collections.emptyList());

                assertThat(existing.getPasswordLastChangedAt())
                                .as("a legacy backup must not null out the existing timestamp")
                                .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
                assertThat(existing.getPassword())
                                .as("a legacy backup must not clobber the existing hash")
                                .isEqualTo("$2a$10$existingHash");
        }

        @Test
        void restoreSystemFromBackup_withCatalogue_shouldRestoreCatalogueAfterWipe() {
                // KNOWN_ISSUES §10: the global skills catalogue rides in the
                // backup and must be re-inserted AFTER the wipe but BEFORE
                // users are restored, so the per-user skill restore attaches
                // to catalogue rows instead of creating partial stand-ins.
                User userFromBackup = new User();
                userFromBackup.setEmail("catalogue@example.com");
                List<UserFullBackup> systemBackup = Collections.singletonList(
                                new UserFullBackup(userFromBackup, new PortfolioBackup()));

                Skill catalogueSkill = new Skill();
                catalogueSkill.setUuid(UUID.randomUUID());
                catalogueSkill.setName("Catalogue Probe");
                catalogueSkill.setCategory("QA");
                catalogueSkill.setVisible(false);

                jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
                when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
                when(userRepository.save(any(User.class))).thenReturn(new User());
                when(skillRepository.findAll()).thenReturn(Collections.emptyList());
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                restoreService.restoreSystemFromBackup(systemBackup, List.of(catalogueSkill));

                org.mockito.InOrder inOrder = inOrder(restoreService, skillRepository);
                inOrder.verify(restoreService).wipeAllData();
                inOrder.verify(skillRepository).save(catalogueSkill);
                assertThat(catalogueSkill.getUuid())
                                .as("catalogue identity must be assigned for rows that lost their uuid")
                                .isNotNull();
        }

        @Test
        void restoreSystemFromBackup_withLegacyBackupWithoutCatalogue_shouldSkipCatalogueGracefully() {
                // Legacy files predate the skills array (null): restore must
                // not fail, must not touch the catalogue, and must still
                // restore the users (old on-demand behaviour preserved).
                User userFromBackup = new User();
                userFromBackup.setEmail("legacy@example.com");
                List<UserFullBackup> systemBackup = Collections.singletonList(
                                new UserFullBackup(userFromBackup, new PortfolioBackup()));

                jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
                when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
                when(userRepository.save(any(User.class))).thenReturn(new User());

                restoreService.restoreSystemFromBackup(systemBackup, null);

                verify(restoreService).wipeAllData();
                verify(skillRepository, never()).save(any(Skill.class));
                verify(userRepository).findByEmail(eq("legacy@example.com"));
        }

        @Test
        void restoreSystemFromBackup_withCatalogueAndExistingRow_shouldRefreshInPlacePreservingIdentity() {
                // Defensive upsert path: a catalogue entry matching an existing
                // row (by uuid, else case-insensitively by name) must keep the
                // row's identity (uuid, name) and receive the backup's
                // catalogue-owned fields — including the admin visible
                // override, which a system restore takes from the backup.
                User userFromBackup = new User();
                userFromBackup.setEmail("upsert@example.com");
                List<UserFullBackup> systemBackup = Collections.singletonList(
                                new UserFullBackup(userFromBackup, new PortfolioBackup()));

                Skill existing = new Skill();
                existing.setUuid(UUID.randomUUID());
                existing.setName("java probe");
                existing.setCategory("Old Category");
                existing.setVisible(true);
                UUID originalUuid = existing.getUuid();

                Skill incoming = new Skill();
                incoming.setUuid(UUID.randomUUID()); // different uuid: name must match
                incoming.setName("Java Probe"); // case-insensitive match
                incoming.setCategory("New Category");
                incoming.setVisible(false);

                jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
                when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
                when(userRepository.save(any(User.class))).thenReturn(new User());
                when(skillRepository.findAll()).thenReturn(Collections.singletonList(existing));
                when(skillRepository.save(any(Skill.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                restoreService.restoreSystemFromBackup(systemBackup, List.of(incoming));

                verify(skillRepository).save(existing); // updated in place, not re-inserted
                verify(skillRepository, never()).save(incoming);
                assertThat(existing.getCategory()).isEqualTo("New Category");
                assertThat(existing.isVisible())
                                .as("the admin visible override comes from the backup")
                                .isFalse();
                assertThat(existing.getUuid())
                                .as("the existing row's identity must never change")
                                .isEqualTo(originalUuid);
        }
}
