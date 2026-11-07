package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.repository.SkillRepository;
import com.forkmyfolio.repository.UserSkillRepository;
import com.forkmyfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserSkill> getAllSkillsForUser(User user) {
        return userSkillRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSkill getSkillForUser(User user, UUID userSkillUuid) {
        UserSkill userSkill = userSkillRepository.findByUuid(userSkillUuid)
                .orElseThrow(() -> new ResourceNotFoundException("UserSkill with UUID: " + userSkillUuid));

        if (!userSkill.getUser().getId().equals(user.getId())) {
            // Phase 7.4 (403/404 policy): cross-tenant access is concealed
            // as 404, matching the uuid-miss branch above.
            throw new ResourceNotFoundException("UserSkill with UUID: " + userSkillUuid);
        }
        return userSkill;
    }

    @Override
    @Transactional
    public UserSkill addSkillToUser(User user, Skill skillDetails, UserSkill userSkillDetails) {
        // FIX: Delegate to the new findOrCreateSkill method to reduce duplication.
        Skill skill = findOrCreateSkill(skillDetails);

        // Create the new UserSkill relationship using the found/created global skill
        // and the user-specific details.
        UserSkill newUserSkill = new UserSkill();
        newUserSkill.setUser(user);
        newUserSkill.setSkill(skill);
        newUserSkill.setLevel(userSkillDetails.getLevel());
        newUserSkill.setVisible(userSkillDetails.isVisible());
        newUserSkill.setDescription(userSkillDetails.getDescription());

        return userSkillRepository.save(newUserSkill);
    }

    @Override
    @Transactional
    public UserSkill updateSkillForUser(UUID userSkillUuid, UserSkill userSkillUpdates, User user) {
        UserSkill existingUserSkill = getSkillForUser(user, userSkillUuid); // This re-uses the permission check
        existingUserSkill.setLevel(userSkillUpdates.getLevel());
        existingUserSkill.setVisible(userSkillUpdates.isVisible());
        existingUserSkill.setDescription(userSkillUpdates.getDescription());
        return userSkillRepository.save(existingUserSkill);
    }

    @Override
    @Transactional
    public void removeSkillFromUser(UUID userSkillUuid, User user) {
        UserSkill userSkill = getSkillForUser(user, userSkillUuid);
        userSkillRepository.delete(userSkill);
    }

    @Override
    @Transactional
    public Set<Skill> findOrCreateSkills(Set<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return Set.of();
        }
        return skillNames.stream()
                .map(name -> skillRepository.findByName(name)
                        .orElseGet(() -> {
                            Skill newSkill = new Skill();
                            newSkill.setName(name);
                            // Category and Icon would be null here, which is acceptable
                            // as they can be updated later via a dedicated admin interface.
                            return skillRepository.save(newSkill);
                        }))
                .collect(Collectors.toSet());
    }

    @Override
    public List<Skill> getAllPlatformSkills() {
        return skillRepository.findAll();
    }

    /**
     * FIX: Implemented the missing findOrCreateSkill method to satisfy the SkillService interface.
     * This method finds a skill by name or creates a new one if it doesn't exist.
     *
     * @param skillDetails A transient Skill entity with the details of the skill to find or create.
     * @return The persisted Skill entity.
     */
    @Override
    @Transactional
    public Skill findOrCreateSkill(Skill skillDetails) {
        return skillRepository.findByName(skillDetails.getName())
                .orElseGet(() -> {
                    Skill newSkill = new Skill();
                    newSkill.setName(skillDetails.getName());
                    newSkill.setCategory(skillDetails.getCategory());
                    newSkill.setIcon(skillDetails.getIcon());
                    newSkill.setDescription(skillDetails.getDescription());
                    return skillRepository.save(newSkill);
                });
    }
}
