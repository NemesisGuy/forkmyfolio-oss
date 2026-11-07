package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.*;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private com.forkmyfolio.repository.UserRepository userRepository;

    @InjectMocks
    private BackupServiceImpl backupService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setPortfolioProfile(new PortfolioProfile());
        testUser.setProjects(Set.of(new Project()));

        // Correctly set up Skill and UserSkill
        Skill skill = new Skill();
        skill.setName("Java");

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(testUser);
        userSkill.setSkill(skill);
        userSkill.setLevel(SkillLevel.EXPERT);
        userSkill.setVisible(true);
        testUser.setUserSkills(Set.of(userSkill));

        testUser.setExperiences(Set.of(new Experience()));
        testUser.setTestimonials(Set.of(new Testimonial()));
        testUser.setQualifications(Set.of(new Qualification()));

        // Phase 10 (Workstream 4): seed a per-user setting override
        UserSetting setting = new UserSetting();
        setting.setName("portfolio.projects.show");
        setting.setValue("false");
        testUser.setUserSettings(Set.of(setting));
    }

    @Test
    void createBackupForUser_withFullData_shouldPopulateAllFields() {
        // Act
        PortfolioBackup backup = backupService.createBackupForUser(testUser);

        // Assert
        assertNotNull(backup);
        assertNotNull(backup.getProfile());
        assertEquals(1, backup.getProjects().size());
        assertEquals(1, backup.getSkills().size());
        assertEquals(1, backup.getExperiences().size());
        assertEquals(1, backup.getTestimonials().size());
        assertEquals(1, backup.getQualifications().size());
        // Phase 10 (Workstream 4): per-user setting overrides ride in the backup
        assertNotNull(backup.getSettings());
        assertEquals(1, backup.getSettings().size());
        assertEquals("portfolio.projects.show", backup.getSettings().get(0).getName());
        assertEquals("false", backup.getSettings().get(0).getValue());
    }

    @Test
    void createBackupForUser_withEmptyData_shouldReturnEmptyContainer() {
        // Arrange
        User emptyUser = new User();
        emptyUser.setPortfolioProfile(null);
        emptyUser.setProjects(Collections.emptySet());
        emptyUser.setUserSkills(Collections.emptySet());
        emptyUser.setExperiences(Collections.emptySet());
        emptyUser.setTestimonials(Collections.emptySet());
        emptyUser.setQualifications(Collections.emptySet());

        // Act
        PortfolioBackup backup = backupService.createBackupForUser(emptyUser);

        // Assert
        assertNotNull(backup);
        assertNull(backup.getProfile());
        assertTrue(backup.getProjects().isEmpty());
        assertTrue(backup.getSkills().isEmpty());
        assertTrue(backup.getExperiences().isEmpty());
        assertTrue(backup.getTestimonials().isEmpty());
        assertTrue(backup.getQualifications().isEmpty());
        // Phase 10 (Workstream 4): null settings collection exports as empty
        assertNotNull(backup.getSettings());
        assertTrue(backup.getSettings().isEmpty());
    }

    @Test
    void createFullSystemBackup_shouldReturnList() {
        // Arrange
        when(userService.getAllUsersWithPortfolioData()).thenReturn(Collections.singletonList(testUser));

        // Act
        java.util.List<UserFullBackup> result = backupService.createFullSystemBackup();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0).getUser());
        assertNotNull(result.get(0).getPortfolio());
        verify(userService).getAllUsersWithPortfolioData();
    }
}
