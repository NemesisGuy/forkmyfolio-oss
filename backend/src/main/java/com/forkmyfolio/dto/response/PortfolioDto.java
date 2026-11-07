package com.forkmyfolio.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PortfolioDto", description = "A comprehensive DTO representing a user's full public portfolio.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioDto {

    @Schema(description = "Basic public information about the user.")
    private PublicUserDto user;

    @Schema(description = "The user's main profile details, if visible.")
    private PortfolioProfileDto profile;

    @Schema(description = "A list of the user's visible projects.")
    private List<ProjectDto> projects;

    @Schema(description = "A list of the user's visible skills.")
    private List<UserSkillDto> skills;

    @Schema(description = "A list of the user's visible work experiences.")
    private List<ExperienceDto> experiences;

    @Schema(description = "A list of the user's visible qualifications.")
    private List<QualificationDto> qualifications;

    @Schema(description = "A list of the user's visible testimonials.")
    private List<TestimonialDto> testimonials;
}