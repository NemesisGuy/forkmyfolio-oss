package com.forkmyfolio.dto.update;

import com.forkmyfolio.model.Experience;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
@Schema(name = "UpdateExperienceRequest", description = "Request body for updating an existing work experience.")
public class UpdateExperienceRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "The job title.", example = "Senior Software Engineer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobTitle;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "The name of the company.", example = "Tech Solutions Inc.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @Size(max = 255)
    @Schema(description = "The URL of the company's website.", example = "https://techsolutions.com")
    private String companyUrl;

    @Size(max = 255)
    @Schema(description = "A URL for the company's logo.", example = "https://cdn.logos.com/techsolutions.png")
    private String companyLogoUrl;

    @Size(max = 100)
    @Schema(description = "The location of the job.", example = "Remote")
    private String location;

    @Schema(description = "The location type of the job.", example = "REMOTE")
    private Experience.LocationType locationType;

    @Schema(description = "The type of employment.", example = "FULL_TIME")
    private Experience.EmploymentType employmentType;

    @NotNull
    @Schema(description = "The start date of the employment.", example = "2022-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @Schema(description = "The end date of the employment. Null if it's the current job.", example = "null")
    private LocalDate endDate;

    @Schema(description = "A general description of the role and responsibilities.", example = "TEXT")
    private String description;

    @Schema(description = "A list of key achievements, separate from the main description.", example = "TEXT")
    private String achievements;

    @NotNull
    @Schema(description = "Whether the experience is visible on the public portfolio.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean visible;

    @NotNull
    @Schema(description = "The order in which to display this experience.", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer displayOrder;

    @Schema(description = "A set of NAMES for the skills used in this experience.")
    private Set<String> skills;
}