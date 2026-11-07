package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.User;
import com.forkmyfolio.service.BackupService;
import com.forkmyfolio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

        private final UserService userService;
        private final com.forkmyfolio.repository.UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public List<com.forkmyfolio.model.UserFullBackup> createFullSystemBackup() {
                // Fetch users using the simplified query (User + Profile + Roles)
                List<User> allUsers = userService.getAllUsersWithPortfolioData();
                List<com.forkmyfolio.model.UserFullBackup> systemBackupData = new ArrayList<>();

                for (User user : allUsers) {
                        // Touch lazy associations inside the transaction so the returned
                        // entities are fully initialized for DTO mapping outside of it.
                        if (user.getRoles() != null) {
                                user.getRoles().size();
                        }
                        com.forkmyfolio.model.PortfolioBackup portfolioBackup = createBackupForUser(user);
                        initializePortfolioBackup(portfolioBackup);
                        systemBackupData.add(new com.forkmyfolio.model.UserFullBackup(user, portfolioBackup));
                }

                return systemBackupData;
        }

        @Override
        @Transactional(readOnly = true)
        public com.forkmyfolio.model.PortfolioBackup createBackupForUser(User user) {
                com.forkmyfolio.model.PortfolioBackup backup = com.forkmyfolio.model.PortfolioBackup.builder()
                                .profile(user.getPortfolioProfile())
                                .projects(new ArrayList<>(user.getProjects()))
                                .skills(new ArrayList<>(user.getUserSkills()))
                                .experiences(new ArrayList<>(user.getExperiences()))
                                .testimonials(new ArrayList<>(user.getTestimonials()))
                                .qualifications(new ArrayList<>(user.getQualifications()))
                                // Phase 10 (Workstream 4): include per-user setting
                                // overrides so section visibility survives round trips.
                                .settings(user.getUserSettings() == null
                                        ? new ArrayList<>()
                                        : new ArrayList<>(user.getUserSettings()))
                                .build();
                initializePortfolioBackup(backup);
                return backup;
        }

        /**
         * Forces initialization of lazy associations so DTO mapping can happen
         * outside of the transactional boundary without LazyInitializationException.
         */
        private void initializePortfolioBackup(com.forkmyfolio.model.PortfolioBackup backup) {
                if (backup == null) {
                        return;
                }
                if (backup.getProfile() != null) {
                        backup.getProfile().getHeadline();
                        if (backup.getProfile().getUser() != null) {
                                backup.getProfile().getUser().getEmail();
                        }
                }
                if (backup.getSkills() != null) {
                        for (com.forkmyfolio.model.UserSkill us : backup.getSkills()) {
                                if (us != null && us.getSkill() != null) {
                                        us.getSkill().getName();
                                        us.getSkill().getCategory();
                                        us.getSkill().getIcon();
                                }
                        }
                }
                if (backup.getProjects() != null) {
                        for (com.forkmyfolio.model.Project p : backup.getProjects()) {
                                if (p != null && p.getSkills() != null) {
                                        for (com.forkmyfolio.model.Skill s : p.getSkills()) {
                                                if (s != null) {
                                                        s.getName();
                                                }
                                        }
                                }
                                // Phase 7.4 (§6): keyFeatures is a new lazy collection
                                // read by ProjectMapper.toDto outside this transaction.
                                if (p != null && p.getKeyFeatures() != null) {
                                        p.getKeyFeatures().size();
                                }
                        }
                }
                if (backup.getExperiences() != null) {
                        for (com.forkmyfolio.model.Experience e : backup.getExperiences()) {
                                if (e != null && e.getSkills() != null) {
                                        for (com.forkmyfolio.model.Skill s : e.getSkills()) {
                                                if (s != null) {
                                                        s.getName();
                                                }
                                        }
                                }
                        }
                }
                // Phase 10 (Workstream 4): initialize per-user settings collection.
                if (backup.getSettings() != null) {
                        for (com.forkmyfolio.model.UserSetting setting : backup.getSettings()) {
                                if (setting != null) {
                                        setting.getName();
                                        setting.getValue();
                                }
                        }
                }
        }

        @Override
        @Transactional(readOnly = true)
        public com.forkmyfolio.model.PortfolioBackup createBackupForUser(java.util.UUID userUuid) {
                User user = userRepository.findByUuid(userUuid)
                                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                                                "User not found with UUID: " + userUuid));
                return createBackupForUser(user);
        }
}
