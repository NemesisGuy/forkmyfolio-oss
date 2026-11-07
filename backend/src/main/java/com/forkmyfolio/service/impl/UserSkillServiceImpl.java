package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceAlreadyExistsException;
import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.repository.UserSkillRepository;
import com.forkmyfolio.service.SkillService;
import com.forkmyfolio.service.UserSkillService;
import org.hibernate.Hibernate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSkillServiceImpl implements UserSkillService {

    private final UserSkillRepository userSkillRepository;
    private final SkillService skillService;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserSkill> getUserSkillLookupMap(User user) {
        List<UserSkill> userSkills = userSkillRepository.findByUserWithSkill(user);
        return userSkills.stream()
                .filter(us -> us.getSkill() != null && us.getSkill().getUuid() != null)
                .collect(Collectors.toMap(us -> us.getSkill().getUuid(), us -> us, (a, b) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSkill> getAllSkillsForUser(User user) {
        return userSkillRepository.findByUserWithSkill(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSkill getSkillForUser(User user, UUID userSkillUuid) {
        UserSkill userSkill = userSkillRepository.findByUuid(userSkillUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UserSkill relationship with UUID " + userSkillUuid + " not found."));

        if (!userSkill.getUser().getId().equals(user.getId())) {
            // Phase 7.4 (403/404 policy): cross-tenant access is concealed as
            // 404 (same as the uuid-miss branch above) - never disclose that
            // a foreign relationship exists.
            throw new ResourceNotFoundException(
                    "UserSkill relationship with UUID " + userSkillUuid + " not found.");
        }
        // FIX: Initialize the lazy-loaded Skill entity before returning from the
        // transactional method.
        // This prevents a LazyInitializationException when the controller's mapper
        // accesses the skill.
        Hibernate.initialize(userSkill.getSkill());
        return userSkill;
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public UserSkill addSkillToUser(User user, Skill skillDetails, UserSkill userSkillDetails) {
        // 1. Find or create the global skill. This is delegated to SkillService.
        Skill globalSkill = skillService.findOrCreateSkill(skillDetails);

        // 2. Check if the user already has this skill.
        if (userSkillRepository.existsByUserAndSkill(user, globalSkill)) {
            throw new ResourceAlreadyExistsException("User already has the skill: " + globalSkill.getName());
        }

        // 3. Assemble the new UserSkill relationship.
        userSkillDetails.setUser(user);
        userSkillDetails.setSkill(globalSkill);

        // 4. Save and return the new relationship.
        return userSkillRepository.save(userSkillDetails);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public UserSkill updateSkillForUser(UUID userSkillUuid, UserSkill userSkillUpdates, User user) {
        // 1. Fetch the existing UserSkill and verify ownership.
        UserSkill existingUserSkill = getSkillForUser(user, userSkillUuid);

        // 2. Apply updates.
        existingUserSkill.setLevel(userSkillUpdates.getLevel());
        existingUserSkill.setVisible(userSkillUpdates.isVisible());
        existingUserSkill.setDescription(userSkillUpdates.getDescription());

        // 3. Save and return the updated entity.
        return userSkillRepository.save(existingUserSkill);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public void removeSkillFromUser(UUID userSkillUuid, User user) {
        // 1. Fetch the UserSkill to delete and verify ownership.
        UserSkill userSkillToDelete = getSkillForUser(user, userSkillUuid);

        // 2. Delete the relationship.
        userSkillRepository.delete(userSkillToDelete);
    }
}
