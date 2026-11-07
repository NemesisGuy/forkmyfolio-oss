package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.Experience;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExperienceDto", description = "Represents a work experience entry in a user's portfolio.")
public class ExperienceDto {

    @Schema(description = "The unique identifier for the experience.", example = "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6")
    private UUID uuid;

    @Schema(description = "The job title.", example = "Software Engineer")
    private String jobTitle;

    @Schema(description = "The name of the company.", example = "Tech Solutions Inc.")
    private String companyName;

    @Schema(description = "The URL of the company's website.", example = "https://techsolutions.com")
    private String companyUrl;

    @Schema(description = "A URL for the company's logo.", example = "https://cdn.logos.com/techsolutions.png")
    private String companyLogoUrl;

    @Schema(description = "The location of the job.", example = "New York, NY")
    private String location;

    @Schema(description = "The location type of the job.", example = "REMOTE")
    private Experience.LocationType locationType;

    @Schema(description = "The type of employment.", example = "FULL_TIME")
    private Experience.EmploymentType employmentType;

    @Schema(description = "The start date of the employment.", example = "2022-01-15")
    private LocalDate startDate;

    @Schema(description = "The end date of the employment. Null if it's the current job.", example = "2024-06-30")
    private LocalDate endDate;

    @Schema(description = "A general description of the role and responsibilities.")
    private String description;

    @Schema(description = "A list of key achievements.")
    private String achievements;

    @Schema(description = "Whether the experience is visible on the public portfolio.", example = "true")
    private boolean visible;

    @Schema(description = "The order in which to display this experience.", example = "1")
    private Integer displayOrder;

    @Schema(description = "A set of skills associated with this experience, including user-specific proficiency level.")
    private Set<SkillDto> skills;

    @Schema(description = "The timestamp when the experience was created.")
    private Instant createdAt;

    @Schema(description = "The timestamp when the experience was last updated.")
    private Instant updatedAt;
}