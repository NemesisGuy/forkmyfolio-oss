package com.forkmyfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A domain-level container that aggregates all of a user's portfolio data for
 * backup and restore purposes.
 * This is used to decouple the service layer from DTOs (Rule 2: DBR).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioBackup {
    private PortfolioProfile profile;
    private List<Project> projects;
    private List<UserSkill> skills;
    private List<Experience> experiences;
    private List<Testimonial> testimonials;
    private List<Qualification> qualifications;
    /**
     * Phase 10 (Workstream 4): per-user setting overrides, so section
     * visibility preferences survive a backup/restore round trip.
     */
    private List<UserSetting> settings;
}
