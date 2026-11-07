package com.forkmyfolio.dto.update;

import com.forkmyfolio.model.enums.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UpdateUserSkillRequest", description = "Data required to update the current user's relationship with a skill.")
public class UpdateUserSkillRequest {

    @NotNull(message = "Skill level cannot be null.")
    @Schema(description = "The user's updated proficiency level with this skill.", example = "EXPERT", requiredMode = Schema.RequiredMode.REQUIRED)
    private SkillLevel level;

    @NotNull(message = "Visibility must be specified.")
    @Schema(description = "Updated visibility for this skill on the user's public portfolio.", example = "false")
    private boolean visible;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters.")
    @Schema(description = "Updated personal description or notes about this skill.", example = "Now using it for state management with Redux.")
    private String description;
}