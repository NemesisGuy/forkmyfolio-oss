package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.ExperienceRepository;
import com.forkmyfolio.service.ExperienceService;
import com.forkmyfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final SkillService skillService;

    @Override
    @Transactional(readOnly = true)
    public List<Experience> getExperiencesForUser(User user) {
        // The service layer's responsibility is to fetch the domain entities.
        List<Experience> experiences = experienceRepository.findByUserOrderByDisplayOrderAsc(user);
        // We initialize the skills collection to prevent LazyInitializationException
        // later in the mapping process.
        experiences.forEach(exp -> Hibernate.initialize(exp.getSkills()));
        return experiences;
    }

    @Override
    @Transactional(readOnly = true)
    public Experience findExperienceByUuidAndUser(UUID uuid, User user) {
        Experience experience = experienceRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Experience with UUID: " + uuid));

        if (!experience.getUser().getId().equals(user.getId())) {
            // Phase 7.4 (403/404 policy): cross-tenant access is concealed
            // as 404, matching the uuid-miss branch above.
            throw new ResourceNotFoundException("Experience with UUID: " + uuid);
        }

        // Initialize the skills collection to prevent LazyInitializationException in
        // the mapper.
        Hibernate.initialize(experience.getSkills());
        return experience;
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#owner.slug")
    public Experience createExperience(Experience experienceDetails, Set<String> skillNames, User owner) {
        // Set the owner of the experience, which is a service-layer responsibility.
        experienceDetails.setUser(owner);

        // Delegate to SkillService to find or create the associated global skills.
        Set<Skill> skills = skillService.findOrCreateSkills(skillNames);
        experienceDetails.setSkills(skills);

        return experienceRepository.save(experienceDetails);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#currentUser.slug")
    public Experience updateExperience(UUID uuid, Experience updatedExperienceData, Set<String> skillNames,
            User currentUser) {
        Experience existingExperience = findExperienceByUuidAndUser(uuid, currentUser);

        // Update all fields from the provided data object
        existingExperience.setJobTitle(updatedExperienceData.getJobTitle());
        existingExperience.setCompanyName(updatedExperienceData.getCompanyName());
        existingExperience.setCompanyUrl(updatedExperienceData.getCompanyUrl());
        existingExperience.setCompanyLogoUrl(updatedExperienceData.getCompanyLogoUrl());
        existingExperience.setLocation(updatedExperienceData.getLocation());
        existingExperience.setLocationType(updatedExperienceData.getLocationType());
        existingExperience.setEmploymentType(updatedExperienceData.getEmploymentType());
        existingExperience.setStartDate(updatedExperienceData.getStartDate());
        existingExperience.setEndDate(updatedExperienceData.getEndDate());
        existingExperience.setDescription(updatedExperienceData.getDescription());
        existingExperience.setAchievements(updatedExperienceData.getAchievements());
        existingExperience.setVisible(updatedExperienceData.isVisible());
        existingExperience.setDisplayOrder(updatedExperienceData.getDisplayOrder());

        // Update associated skills
        Set<Skill> skillsToAssociate = skillService.findOrCreateSkills(skillNames);
        existingExperience.setSkills(skillsToAssociate);

        return experienceRepository.save(existingExperience);
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#currentUser.slug")
    public void deleteExperience(UUID uuid, User currentUser) {
        Experience experienceToDelete = findExperienceByUuidAndUser(uuid, currentUser);
        experienceRepository.delete(experienceToDelete);
    }
}
