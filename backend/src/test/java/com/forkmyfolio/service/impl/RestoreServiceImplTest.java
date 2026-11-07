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
                restoreService.restoreSystemFromBackup(systemBackup);

                // Assert
                verify(restoreService).wipeAllData(); // Should wipe first
                verify(userRepository).findByEmail(eq("new@example.com")); // Should check if user exists
                verify(userRepository).save(any(User.class)); // Should save the new user
                // Verify that the restore logic was called for the newly created user
                verify(portfolioProfileRepository, times(1)).save(any(PortfolioProfile.class));
        }
}
