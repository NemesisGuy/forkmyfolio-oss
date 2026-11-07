package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.create.CreateExperienceRequest;
import com.forkmyfolio.dto.response.ExperienceDto;
import com.forkmyfolio.dto.update.UpdateExperienceRequest;
import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper for converting Experience entities to DTOs and vice-versa.
 */
@Component
@RequiredArgsConstructor
public class ExperienceMapper {

    private final SkillMapper skillMapper;

    /**
     * Converts an Experience entity to a DTO, enriching it with user-specific skill
     * data.
     * This is the correct way to build a complete Experience DTO for the API.
     *
     * @param experience      The Experience entity to convert.
     * @param userSkillLookup A map of the user's skills to provide full context.
     * @return A detailed ExperienceDto with user-specific skill information.
     */
    public ExperienceDto toDto(Experience experience, Map<UUID, UserSkill> userSkillLookup) {
        if (experience == null) {
            return null;
        }

        ExperienceDto dto = new ExperienceDto();
        dto.setUuid(experience.getUuid());
        dto.setJobTitle(experience.getJobTitle());
        dto.setCompanyName(experience.getCompanyName());
        dto.setCompanyUrl(experience.getCompanyUrl());
        dto.setCompanyLogoUrl(experience.getCompanyLogoUrl());
        dto.setLocation(experience.getLocation());
        dto.setLocationType(experience.getLocationType());
        dto.setEmploymentType(experience.getEmploymentType());
        dto.setStartDate(experience.getStartDate());
        dto.setEndDate(experience.getEndDate());
        dto.setDescription(experience.getDescription());
        dto.setAchievements(experience.getAchievements());
        dto.setVisible(experience.isVisible());
        dto.setDisplayOrder(experience.getDisplayOrder());
        dto.setCreatedAt(experience.getCreatedAt());
        dto.setUpdatedAt(experience.getUpdatedAt());

        if (experience.getSkills() != null) {
            // FIX: Use the lookup map to call the correct skill mapper.
            // This ensures all user-specific data (ID, level, description) is included.
            dto.setSkills(experience.getSkills().stream()
                    .map(skill -> {
                        UserSkill userSkill = userSkillLookup.get(skill.getUuid());
                        // If the user has this skill rated, use the detailed mapper.
                        // Otherwise, fall back to the basic one.
                        return (userSkill != null) ? skillMapper.toDetailDto(userSkill) : skillMapper.toBasicDto(skill);
                    })
                    .collect(Collectors.toSet()));
        } else {
            dto.setSkills(Collections.emptySet());
        }

        return dto;
    }

    /**
     * Converts a CreateExperienceRequest DTO to a new, transient Experience entity.
     *
     * @param request The DTO with creation data.
     * @return A new Experience entity.
     */
    public Experience toEntity(CreateExperienceRequest request) {
        if (request == null) {
            return null;
        }
        Experience experience = new Experience();
        experience.setJobTitle(request.getJobTitle());
        experience.setCompanyName(request.getCompanyName());
        experience.setCompanyUrl(request.getCompanyUrl());
        experience.setCompanyLogoUrl(request.getCompanyLogoUrl());
        experience.setLocation(request.getLocation());
        experience.setLocationType(request.getLocationType());
        experience.setEmploymentType(request.getEmploymentType());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setDescription(request.getDescription());
        experience.setAchievements(request.getAchievements());
        experience.setVisible(request.isVisible());
        experience.setDisplayOrder(request.getDisplayOrder());
        return experience;
    }

    /**
     * Converts an ExperienceDto (from a backup) to a new Experience entity.
     *
     * @param dto  The DTO with experience data.
     * @param user The user who will own this experience.
     * @return A new Experience entity.
     */
    public Experience toEntityFromDto(ExperienceDto dto, User user) {
        if (dto == null) {
            return null;
        }
        Experience experience = new Experience();
        experience.setUuid(dto.getUuid());
        experience.setUser(user);
        experience.setJobTitle(dto.getJobTitle());
        experience.setCompanyName(dto.getCompanyName());
        experience.setCompanyUrl(dto.getCompanyUrl());
        experience.setCompanyLogoUrl(dto.getCompanyLogoUrl());
        experience.setLocation(dto.getLocation());
        experience.setLocationType(dto.getLocationType());
        experience.setEmploymentType(dto.getEmploymentType());
        experience.setStartDate(dto.getStartDate());
        experience.setEndDate(dto.getEndDate());
        experience.setDescription(dto.getDescription());
        experience.setAchievements(dto.getAchievements());
        experience.setVisible(dto.isVisible());
        experience.setDisplayOrder(dto.getDisplayOrder());

        if (dto.getSkills() != null) {
            experience.setSkills(dto.getSkills().stream()
                    .map(sDto -> {
                        Skill skill = new Skill();
                        skill.setUuid(sDto.getSkillId());
                        skill.setName(sDto.getName());
                        skill.setCategory(sDto.getCategory());
                        skill.setIcon(sDto.getIcon());
                        return skill;
                    })
                    .collect(Collectors.toSet()));
        }

        return experience;
    }

    /**
     * Creates a transient Experience entity from an UpdateExperienceRequest DTO.
     *
     * @param request The DTO with update data.
     * @return A transient Experience entity.
     */
    public Experience toEntity(UpdateExperienceRequest request) {
        if (request == null) {
            return null;
        }
        Experience experience = new Experience();
        experience.setJobTitle(request.getJobTitle());
        experience.setCompanyName(request.getCompanyName());
        experience.setCompanyUrl(request.getCompanyUrl());
        experience.setCompanyLogoUrl(request.getCompanyLogoUrl());
        experience.setLocation(request.getLocation());
        experience.setLocationType(request.getLocationType());
        experience.setEmploymentType(request.getEmploymentType());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setDescription(request.getDescription());
        experience.setAchievements(request.getAchievements());
        experience.setVisible(request.getVisible());
        experience.setDisplayOrder(request.getDisplayOrder());
        return experience;
    }
}