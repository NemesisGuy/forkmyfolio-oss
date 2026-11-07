package com.forkmyfolio.dto.create;

import com.forkmyfolio.model.enums.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateSkillRequest", description = "Data required to add a skill to the current user's portfolio. If the skill doesn't exist globally, it will be created.")
public class CreateSkillRequest {

    @NotBlank(message = "Skill name cannot be blank.")
    @Size(min = 1, max = 100, message = "Skill name must be between 1 and 100 characters.")
    @Schema(description = "The name of the skill to add. This is case-insensitive.", example = "React", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "Skill level cannot be null.")
    @Schema(description = "The user's proficiency level with this skill.", example = "ADVANCED", requiredMode = Schema.RequiredMode.REQUIRED)
    private SkillLevel level;

    @NotNull(message = "Visibility must be specified.")
    @Schema(description = "Whether this skill should be visible on the user's public portfolio.", example = "true", defaultValue = "true")
    private boolean visible = true;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters.")
    @Schema(description = "A personal description or notes about this skill.", example = "Used for building interactive UIs.")
    private String description;

    // --- ADDED FIELDS ---
    @Size(max = 100, message = "Category cannot exceed 100 characters.")
    @Schema(description = "(Optional) A suggested category for the skill if it's new.", example = "Frontend Framework")
    private String category;

    @Size(max = 100, message = "Icon cannot exceed 100 characters.")
    @Schema(description = "(Optional) A suggested icon class for the skill if it's new.", example = "devicon-react-original")
    private String icon;
}