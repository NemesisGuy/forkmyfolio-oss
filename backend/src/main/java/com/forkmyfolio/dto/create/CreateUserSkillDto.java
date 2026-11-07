package com.forkmyfolio.dto.create;

import com.forkmyfolio.model.enums.SkillLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a new UserSkill relationship.
 * This is received by the controller from the frontend.
 */
@Data
public class CreateUserSkillDto {

    @NotBlank(message = "Skill name cannot be blank.")
    private String name;

    private String category;

    @NotNull(message = "Proficiency level is required.")
    private SkillLevel level;

    private boolean visible = true;

    // This can be null or empty, the service will handle the fallback.
    private String description;

    private String icon;
}