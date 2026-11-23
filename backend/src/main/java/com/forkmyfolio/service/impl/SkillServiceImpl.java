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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    /**
     * Phase 0.28 (C1): category given to a skill the owner invented inline
     * (typed straight into a job experience) so it is never category-less.
     */
    static final String DEFAULT_INLINE_CATEGORY = "Custom";

    /**
     * Phase 0.28 (C1): icon given to an inline-created skill so its badge
     * always renders a visible icon (Font Awesome 6 solid puzzle piece).
     */
    static final String DEFAULT_INLINE_ICON = "fa-solid fa-puzzle-piece";

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
            // MUST be mutable: the caller assigns this straight onto a managed
            // entity (e.g. Experience.setSkills) and Hibernate's merge replaces
            // the collection contents via clear(). Returning the immutable
            // Set.of() here threw UnsupportedOperationException -> HTTP 500 ->
            // full transaction rollback, so ANY edit to an experience without
            // skills (e.g. a position change) silently failed to persist.
            return new HashSet<>();
        }
        return skillNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .map(name -> skillRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> {
                            Skill newSkill = new Skill();
                            newSkill.setName(name);
                            // Phase 0.28 (C1): a skill invented inside a job experience used to be
                            // stored NAME-ONLY, so every badge rendered with no icon and the skill
                            // had no category. Inline-created skills now get sane defaults; skills
                            // that already exist in the catalogue keep their own metadata.
                            newSkill.setCategory(DEFAULT_INLINE_CATEGORY);
                            newSkill.setIcon(DEFAULT_INLINE_ICON);
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
