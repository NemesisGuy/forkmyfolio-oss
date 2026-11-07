package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.QualificationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a single qualification (e.g., degree, certificate) in the API response.")
public class QualificationDto {

    @Schema(description = "The unique identifier (UUID) of the qualification.")
    private UUID uuid;

    @Schema(description = "The name of the qualification.", example = "Bachelor of Science in Computer Science")
    private String qualificationName;

    @Schema(description = "The name of the institution that awarded the qualification.", example = "University of California, Berkeley")
    private String institutionName;

    @Schema(description = "URL for the institution's logo.", example = "https://cdn.university.edu/logos/berkeley.png")
    private String institutionLogoUrl;

    @Schema(description = "URL for the institution's public website.", example = "https://www.berkeley.edu/")
    private String institutionWebsite;

    @Schema(description = "The specific field of study.", example = "Computer Science")
    private String fieldOfStudy;

    @Schema(description = "The academic level of the qualification.", example = "BACHELORS")
    private QualificationLevel level;

    @Schema(description = "The year the qualification study began.", example = "2018")
    private Integer startYear;

    @Schema(description = "The year the qualification was completed. Null if ongoing.", example = "2022")
    private Integer completionYear;

    @Schema(description = "Flag indicating if the user is currently pursuing this qualification.", example = "false")
    private Boolean stillStudying;

    @Schema(description = "The grade or result achieved.", example = "GPA: 3.9/4.0")
    private String grade;

    @Schema(description = "A URL to a verifiable credential or digital badge.", example = "https://www.credly.com/badges/...")
    private String credentialUrl;

    @Schema(description = "Flag indicating if the qualification is visible on the public portfolio.", example = "true")
    private boolean visible;

    @Schema(description = "Timestamp of when the qualification was created.")
    private Instant createdAt;

    @Schema(description = "Timestamp of the last update to the qualification.")
    private Instant updatedAt;
}