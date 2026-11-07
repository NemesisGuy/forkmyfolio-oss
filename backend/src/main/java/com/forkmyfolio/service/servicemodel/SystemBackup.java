package com.forkmyfolio.service.servicemodel;

import com.forkmyfolio.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A data transfer object that holds all data for a full system backup.
 * This class is used for serialization to and from JSON.
 */
@Data
@NoArgsConstructor
public class SystemBackup {
    private List<User> users;
    private List<PortfolioProfile> portfolioProfiles;
    private List<Project> projects;
    private List<Skill> skills;
    private List<Experience> experiences;
    private List<Testimonial> testimonials;
    private List<Qualification> qualifications;
    private List<ContactMessage> contactMessages;
    private List<Setting> settings;
    private List<UserSetting> userSettings;
}