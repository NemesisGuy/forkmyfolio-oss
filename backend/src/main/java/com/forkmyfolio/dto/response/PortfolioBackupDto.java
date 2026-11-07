package com.forkmyfolio.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A comprehensive DTO that aggregates all of a user's portfolio data for backup and export purposes.
 */
@Data
@NoArgsConstructor
@Schema(name = "PortfolioBackupDto", description = "A complete backup of all portfolio data.")
public class PortfolioBackupDto {

    @Schema(description = "The main portfolio profile information.")
    private PortfolioProfileDto profile;

    @Schema(description = "A list of all projects in the portfolio.")
    private List<ProjectDto> projects;

    @Schema(description = "A list of all skills.")
    private List<UserSkillDto> skills;
    @Schema(description = "A list of all work experience entries.")
    private List<ExperienceDto> experiences;

    @Schema(description = "A list of all testimonials.")
    private List<TestimonialDto> testimonials;

    @Schema(description = "A list of all qualifications (education, certifications).")
    private List<QualificationDto> qualifications;

    /**
     * Phase 10 (Workstream 4): the user's per-user setting overrides
     * (section visibility etc.). Optional — older backup files predate this
     * field and restore cleanly with settings left untouched/absent.
     */
    @Schema(description = "The user's per-user setting overrides (e.g. section visibility). Absent in legacy backups.")
    private List<UserSettingDto> settings;
}