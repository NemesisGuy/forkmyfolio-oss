package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.*;
import com.forkmyfolio.repository.*;
import com.forkmyfolio.service.RestoreService;
import com.forkmyfolio.service.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestoreServiceImpl implements RestoreService {

    // Services
    private final UserService userService;
    private final EntityManager entityManager;

    // Repositories for data manipulation
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final QualificationRepository qualificationRepository;
    private final TestimonialRepository testimonialRepository;
    private final PortfolioProfileRepository portfolioProfileRepository;
    private final SkillRepository skillRepository;
    private final com.forkmyfolio.repository.ContactMessageRepository contactMessageRepository;

    @Override
    @Transactional
    public void restoreFromBackup(com.forkmyfolio.model.PortfolioBackup backupData) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        log.info("Initiating restore for user: {}", currentUser.getEmail());
        performRestore(currentUser, backupData);
    }

    @Override
    @Transactional
    public void restoreUserFromBackup(User targetUser, com.forkmyfolio.model.PortfolioBackup backupData) {
        log.info("Admin initiating restore for user: {}", targetUser.getEmail());
        performRestore(targetUser, backupData);
    }

    @Override
    @Transactional
    public void restoreSystemFromBackup(List<com.forkmyfolio.model.UserFullBackup> systemBackupData) {
        log.warn("Initiating SYSTEM-WIDE restore. This is a destructive operation.");
        wipeAllData(); // Start from a clean slate

        for (com.forkmyfolio.model.UserFullBackup userBackup : systemBackupData) {
            if (userBackup == null || userBackup.getUser() == null) {
                continue;
            }
            User userFromBackup = userBackup.getUser();
            com.forkmyfolio.model.PortfolioBackup portfolioFromBackup = userBackup.getPortfolio();

            log.info("Restoring user: {}", userFromBackup.getEmail());
            // Find existing user or create a new one from the backup.
            User existingUser = userRepository.findByEmail(userFromBackup.getEmail()).orElse(null);
            User restoredUser;
            if (existingUser == null) {
                log.info("User {} not found. Creating new user from backup.", userFromBackup.getEmail());
                if (userFromBackup.getPassword() == null || userFromBackup.getPassword().isBlank()) {
                    log.warn("Backup for {} contains no password hash (legacy/V2 backup). "
                            + "Account will be created without a usable password; use password recovery to set one.",
                            userFromBackup.getEmail());
                }
                restoredUser = userRepository.save(userFromBackup);
            } else {
                // Update non-sensitive profile fields. Restore password hash only if the
                // backup carries one (V3+); otherwise preserve the existing hash.
                existingUser.setFirstName(userFromBackup.getFirstName());
                existingUser.setLastName(userFromBackup.getLastName());
                existingUser.setSlug(userFromBackup.getSlug());
                existingUser.setProfileImageUrl(userFromBackup.getProfileImageUrl());
                existingUser.setRoles(userFromBackup.getRoles());
                existingUser.setActive(userFromBackup.isActive());
                existingUser.setProvider(userFromBackup.getProvider());
                existingUser.setTermsAcceptedAt(userFromBackup.getTermsAcceptedAt());
                existingUser.setTermsVersion(userFromBackup.getTermsVersion());
                if (userFromBackup.getPassword() != null && !userFromBackup.getPassword().isBlank()) {
                    existingUser.setPassword(userFromBackup.getPassword());
                }
                restoredUser = userRepository.save(existingUser);
            }

            // Restore the portfolio data for this user
            performRestore(restoredUser, portfolioFromBackup);
        }
        log.info("System-wide restore completed.");
    }

    @Override
    @Transactional
    public void wipeAllData() {
        log.warn("Wiping all portfolio and user data from the database.");
        // Disable FK checks to avoid constraint violations during bulk delete
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
        try {
            // Delete child tables first
            entityManager.createNativeQuery("DELETE FROM contact_messages").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM refresh_tokens").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM announcement_reads").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_skills").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM project_skills").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM experience_skills").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM projects").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM key_features").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM experiences").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM qualifications").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM testimonials").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM portfolio_profiles").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM announcements").executeUpdate();
            // Phase 10 (Workstream 4): these user-scoped tables were previously
            // missed, leaving orphaned rows pointing at deleted users after a
            // system restore (FK checks are disabled during the wipe).
            entityManager.createNativeQuery("DELETE FROM user_settings").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM guest_passes").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM job_application_notes").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM job_applications").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_analytics_config").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM visitor_stats").executeUpdate();
            // Users and Skills are top-level
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM skills").executeUpdate();
        } finally {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
        }
        log.info("All data wiped successfully.");
    }

    /**
     * The core, reusable logic for restoring a single user's portfolio data.
     *
     * @param user       The user entity to restore data for.
     * @param backupData The domain container containing the portfolio data.
     */
    private void performRestore(User user, com.forkmyfolio.model.PortfolioBackup backupData) {
        if (backupData == null) {
            backupData = new com.forkmyfolio.model.PortfolioBackup();
        }
        // 1. Clear existing portfolio data for the user to ensure a clean restore
        if (user.getUserSkills() != null && !user.getUserSkills().isEmpty()) {
            userSkillRepository.deleteAll(user.getUserSkills());
            user.getUserSkills().clear();
        }
        if (user.getProjects() != null && !user.getProjects().isEmpty()) {
            projectRepository.deleteAll(user.getProjects());
            user.getProjects().clear();
        }
        if (user.getExperiences() != null && !user.getExperiences().isEmpty()) {
            experienceRepository.deleteAll(user.getExperiences());
            user.getExperiences().clear();
        }
        if (user.getQualifications() != null) {
            qualificationRepository.deleteAll(user.getQualifications());
            user.getQualifications().clear();
        }
        if (user.getTestimonials() != null) {
            testimonialRepository.deleteAll(user.getTestimonials());
            user.getTestimonials().clear();
        }

        if (user.getPortfolioProfile() != null) {
            PortfolioProfile profileToDelete = user.getPortfolioProfile();
            user.setPortfolioProfile(null);
            portfolioProfileRepository.delete(profileToDelete);
        }

        entityManager.flush();

        // 2. Restore Portfolio Profile
        if (backupData.getProfile() != null) {
            PortfolioProfile profile = backupData.getProfile();
            profile.setUser(user); // Ensure the association is correctly set to the managed user
            user.setPortfolioProfile(profile);
            portfolioProfileRepository.save(profile);
        }

        // 3. Restore Skills
        // FIX: Use a case-insensitive TreeMap for the skill cache.
        Map<String, Skill> skillCache = skillRepository.findAll().stream()
                .filter(s -> s != null && StringUtils.hasText(s.getName()))
                .collect(Collectors.toMap(
                        Skill::getName,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));

        List<UserSkill> restoredUserSkills = Optional.ofNullable(backupData.getSkills())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(us -> us.getSkill() != null && StringUtils.hasText(us.getSkill().getName()))
                .map(userSkill -> {
                    Skill originalSkill = userSkill.getSkill();
                    String skillName = originalSkill.getName();
                    Skill skill = skillCache.computeIfAbsent(skillName, name -> {
                        Skill newSkill = new Skill();
                        newSkill.setName(name);
                        newSkill.setCategory(originalSkill.getCategory());
                        newSkill.setIcon(originalSkill.getIcon());
                        return skillRepository.save(newSkill);
                    });
                    userSkill.setUser(user);
                    userSkill.setSkill(skill);
                    // Phase 10 (Workstream 4) FIX: preserve the backed-up
                    // visibility. This previously forced setVisible(true),
                    // silently un-hiding skills the user had hidden before
                    // the backup was taken (the mapper already copied the
                    // DTO's visible flag onto the entity).

                    if (!StringUtils.hasText(userSkill.getDescription())) {
                        userSkill.setDescription(skill.getDescription());
                    }
                    return userSkill;
                }).collect(Collectors.toList());
        userSkillRepository.saveAll(restoredUserSkills);

        // Create a lookup map for the newly saved skills
        Map<String, Skill> restoredSkillMap = restoredUserSkills.stream()
                .map(UserSkill::getSkill)
                .filter(Objects::nonNull)
                .filter(s -> StringUtils.hasText(s.getName()))
                .distinct()
                .collect(Collectors.toMap(Skill::getName, Function.identity(), (a, b) -> a));

        // 4. Restore Projects, Experiences, etc., using the restored skills
        if (backupData.getProjects() != null) {
            List<Project> projects = backupData.getProjects().stream().map(project -> {
                project.setUser(user);
                Set<Skill> skills = Optional.ofNullable(project.getSkills())
                        .orElse(Collections.emptySet())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(s -> restoredSkillMap.get(s.getName()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                project.setSkills(skills);
                return project;
            }).collect(Collectors.toList());
            projectRepository.saveAll(projects);
        }

        if (backupData.getExperiences() != null) {
            List<Experience> experiences = backupData.getExperiences().stream().map(experience -> {
                experience.setUser(user);
                Set<Skill> skills = Optional.ofNullable(experience.getSkills())
                        .orElse(Collections.emptySet())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(s -> restoredSkillMap.get(s.getName()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                experience.setSkills(skills);
                return experience;
            }).collect(Collectors.toList());
            experienceRepository.saveAll(experiences);
        }

        if (backupData.getQualifications() != null) {
            List<Qualification> qualifications = backupData.getQualifications().stream()
                    .peek(q -> q.setUser(user))
                    .collect(Collectors.toList());
            qualificationRepository.saveAll(qualifications);
        }

        if (backupData.getTestimonials() != null) {
            List<Testimonial> testimonials = backupData.getTestimonials().stream()
                    .peek(t -> t.setUser(user))
                    .collect(Collectors.toList());
            testimonialRepository.saveAll(testimonials);
        }

        // 5. Phase 10 (Workstream 4): restore per-user setting overrides.
        // Upsert semantics — update matching overrides in place, add new ones
        // through the cascade collection, and orphan-remove ones the backup
        // no longer contains. A delete-then-reinsert was tried first and
        // rejected: Hibernate flushes INSERTs before DELETEs, so re-inserting
        // a (user_id, name) pair that is being deleted in the same flush
        // violates the unique constraint. Legacy backups predate this field
        // (null): leave the user's current settings untouched rather than
        // wiping them — old backups must restore without destroying data
        // they never captured.
        if (backupData.getSettings() != null) {
            if (user.getUserSettings() == null) {
                user.setUserSettings(new HashSet<>());
            }
            Map<String, UserSetting> existingByName = user.getUserSettings().stream()
                    .filter(s -> s != null && StringUtils.hasText(s.getName()))
                    .collect(Collectors.toMap(UserSetting::getName, Function.identity(), (a, b) -> a));
            Set<String> backupNames = new HashSet<>();
            for (UserSetting incoming : backupData.getSettings()) {
                if (incoming == null || !StringUtils.hasText(incoming.getName())) {
                    continue;
                }
                backupNames.add(incoming.getName());
                UserSetting current = existingByName.get(incoming.getName());
                if (current != null) {
                    current.setValue(incoming.getValue());
                } else {
                    incoming.setUser(user);
                    user.getUserSettings().add(incoming);
                }
            }
            // Overrides the backup does not carry are removed (orphanRemoval).
            user.getUserSettings().removeIf(s -> s != null && s.getName() != null
                    && !backupNames.contains(s.getName()));
            log.info("Synced {} user setting overrides from backup for user {}", backupNames.size(), user.getEmail());
        }
    }
}