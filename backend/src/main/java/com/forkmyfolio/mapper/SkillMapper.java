package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.create.CreateSkillRequest;
import com.forkmyfolio.dto.response.SkillDto;
import com.forkmyfolio.dto.update.UpdateUserSkillRequest;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.UserSkill;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting between Skill/UserSkill domain models
 * and Skill-related DTOs.
 * This centralizes the conversion logic, keeping it out of the service and
 * controller layers.
 */
@Component
public class SkillMapper {

    /**
     * Converts a global Skill entity to a basic SkillDto. This should only be used
     * when no user-specific context is available. It represents a skill without
     * proficiency.
     *
     * @param skill The Skill entity to convert.
     * @return The corresponding basic SkillDto.
     */
    public SkillDto toBasicDto(Skill skill) {
        if (skill == null) {
            return null;
        }
        SkillDto dto = new SkillDto();
        // No userSkillId because there is no user context
        dto.setSkillId(skill.getUuid());
        dto.setName(skill.getName());
        dto.setCategory(skill.getCategory());
        dto.setIcon(skill.getIcon());
        // Level is null because it's user-specific
        // Description is the global one
        dto.setDescription(skill.getDescription());
        return dto;
    }

    /**
     * Converts a UserSkill entity to a detailed SkillDto.
     * This version includes all user-specific details like proficiency level and
     * visibility.
     * It's suitable for managing a user's own skill list.
     *
     * @param userSkill The UserSkill entity to convert.
     * @return The corresponding detailed SkillDto.
     */
    public SkillDto toDetailDto(UserSkill userSkill) {
        if (userSkill == null || userSkill.getSkill() == null) {
            return null;
        }
        Skill skill = userSkill.getSkill();
        SkillDto dto = new SkillDto();

        // User-specific fields from UserSkill
        dto.setUserSkillId(userSkill.getUuid());
        dto.setLevel(userSkill.getLevel());
        dto.setVisible(userSkill.isVisible());
        dto.setDescription(userSkill.getDescription());

        // Global fields from Skill
        dto.setSkillId(skill.getUuid());
        dto.setName(skill.getName());
        dto.setCategory(skill.getCategory());
        dto.setIcon(skill.getIcon());
        dto.setCreatedAt(userSkill.getCreatedAt());
        dto.setUpdatedAt(userSkill.getUpdatedAt());

        return dto;
    }

    /**
     * Converts a CreateSkillRequest DTO to a new, transient Skill entity.
     * This captures the details of the global skill to be found or created.
     *
     * @param request The DTO with creation data.
     * @return A new, transient Skill entity.
     */
    public Skill toSkillEntity(CreateSkillRequest request) {
        if (request == null) {
            return null;
        }
        Skill skill = new Skill();
        skill.setName(request.getName());
        skill.setCategory(request.getCategory());
        skill.setIcon(request.getIcon());
        return skill;
    }

    /**
     * Converts a CreateSkillRequest DTO to a new, transient UserSkill entity.
     * This captures the user-specific details of the skill relationship.
     *
     * @param request The DTO with creation data.
     * @return A new, transient UserSkill entity.
     */
    public UserSkill toUserSkillEntity(CreateSkillRequest request) {
        if (request == null) {
            return null;
        }
        UserSkill userSkill = new UserSkill();
        userSkill.setLevel(request.getLevel());
        userSkill.setVisible(request.isVisible());
        userSkill.setDescription(request.getDescription());
        return userSkill;
    }

    /**
     * Converts an UpdateSkillRequest DTO to a transient UserSkill entity.
     * This captures the user-specific fields that are being updated.
     *
     * @param request The DTO with update data.
     * @return A transient UserSkill entity with the updated values.
     */

    /**
     * FIX: Added the missing overloaded method to handle the UpdateUserSkillRequest
     * DTO.
     * Converts an UpdateUserSkillRequest DTO to a transient UserSkill entity.
     *
     * @param request The DTO with update data.
     * @return A transient UserSkill entity with the updated values.
     */
    public UserSkill toUserSkillEntity(UpdateUserSkillRequest request) {
        if (request == null) {
            return null;
        }
        UserSkill userSkill = new UserSkill();
        userSkill.setLevel(request.getLevel());
        userSkill.setVisible(request.isVisible());
        userSkill.setDescription(request.getDescription());
        return userSkill;
    }

    /**
     * Converts a list of UserSkill entities to a list of detailed SkillDtos for
     * backup purposes.
     *
     * @param userSkills The list of UserSkill entities to convert.
     * @return A list of detailed SkillDtos.
     */
    public List<SkillDto> toBackupDtoList(List<UserSkill> userSkills) {
        if (userSkills == null) {
            return Collections.emptyList();
        }
        return userSkills.stream()
                .map(this::toDetailDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}