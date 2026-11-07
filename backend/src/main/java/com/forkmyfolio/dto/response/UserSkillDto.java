package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a skill associated with a user, including the user-specific
 * skill level.
 * This is the lean version used for backups.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserSkillBackupDto", description = "Represents a skill linked to a user for backup purposes.")
public class UserSkillDto {

    @Schema(description = "The unique identifier for the user's relationship with this skill.", example = "c4a5b6d7-e8f9-1234-a5b6-c7d8e9f0a1b2")
    private UUID userSkillId;

    @Schema(description = "The unique identifier for the global skill entity.", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private UUID skillId;

    @Schema(description = "The name of the skill.", example = "Java")
    private String name;

    @Schema(description = "The user's self-assessed proficiency level for this skill.", example = "ADVANCED")
    private SkillLevel level;
    @Schema(description = "An icon representing the skill (e.g., a CSS class like 'devicon-java-plain').", example = "devicon-java-plain")
    private String icon;

    @Schema(description = "The global category the skill belongs to.", example = "Backend Frameworks")
    private String category;
    @Schema(description = "Whether the skill is visible on the user's public portfolio.", example = "true")
    private boolean visible;

    @Schema(description = "The user's personal notes or description for this skill.", example = "Used in several microservice projects.")
    private String description;

    private Instant createdAt;
    private Instant updatedAt;
}