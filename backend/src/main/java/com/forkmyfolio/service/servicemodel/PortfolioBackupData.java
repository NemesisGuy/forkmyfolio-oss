package com.forkmyfolio.service.servicemodel;


import com.forkmyfolio.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A container class to hold all domain entities for a portfolio backup.
 * This is used internally by the service layer and is not exposed via the API.
 */
@Data
@NoArgsConstructor
public class PortfolioBackupData {
    private PortfolioProfile profile;
    private List<Project> projects;
    private List<Skill> skills;
    private List<Experience> experiences;
    private List<Testimonial> testimonials;
    private List<Qualification> qualifications;
}