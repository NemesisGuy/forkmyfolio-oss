package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
// REFACTOR: Renaming the schema for clarity. This DTO shows the details of a
// user's skill.
@Schema(name = "UserSkillDetailDto", description = "Represents a user's detailed relationship with a global skill, including personal proficiency and visibility.")
public class SkillDto {

    // REFACTOR: Added to explicitly identify the User-Skill relationship entity.
    @Schema(description = "The unique identifier for the user's relationship with this skill.", example = "c4a5b6d7-e8f9-1234-a5b6-c7d8e9f0a1b2")
    private UUID userSkillId;

    // REFACTOR: Renamed from 'uuid' to 'skillId' for clarity. This is the ID of the
    // global skill.
    @Schema(description = "The unique identifier for the global skill.", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID skillId;

    @Schema(description = "The name of the skill.", example = "Spring Boot")
    private String name;

    // REFACTOR: Changed from nested class 'Skill.SkillLevel' to a dedicated
    // top-level enum 'SkillLevel'.
    // This is a standard best practice for maintainability.
    @Schema(description = "The user's proficiency level with the skill.", example = "ADVANCED")
    private SkillLevel level;

    @Schema(description = "Whether the user wants this skill visible on their public portfolio.", example = "true")
    private boolean visible;

    @Schema(description = "The global category the skill belongs to.", example = "Backend Frameworks")
    private String category;

    @Schema(description = "An icon representing the skill (e.g., a CSS class like 'devicon-spring-plain').", example = "devicon-spring-plain")
    private String icon;

    @Schema(description = "The user's personal description or notes about the skill.", example = "Used this for building microservices in my last three projects.")
    private String description;

    @Schema(description = "The timestamp when the user added this skill to their portfolio.")
    private Instant createdAt;

    @Schema(description = "The timestamp when the user last updated their relationship with this skill.")
    private Instant updatedAt;
}