package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.PortfolioDto;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Coordinating Mapper responsible for building the aggregate PortfolioDto.
 * 
 * Adheres to NAG-01 (DBR): This mapper performs the complex transformation
 * from the rich domain model (User entity) to the public API contract
 * (PortfolioDto).
 */
@Component
@RequiredArgsConstructor
public class PortfolioMapper {

    private final PublicUserMapper publicUserMapper;
    private final PortfolioProfileMapper portfolioProfileMapper;
    private final ProjectMapper projectMapper;
    private final UserSkillMapper userSkillMapper;
    private final ExperienceMapper experienceMapper;
    private final QualificationMapper qualificationMapper;
    private final TestimonialMapper testimonialMapper;

    /**
     * Aggregates various portfolio sections into a single PortfolioDto.
     *
     * @param user        The User entity containing all domain data.
     * @param skillLookup A map for enriching projects and experiences with skill
     *                    context.
     * @return A fully populated PortfolioDto.
     */
    public PortfolioDto toDto(User user, Map<UUID, UserSkill> skillLookup) {
        if (user == null) {
            return null;
        }

        PortfolioDto dto = new PortfolioDto();

        // 1. Core Profile Information
        dto.setUser(publicUserMapper.toDto(user));
        dto.setProfile(portfolioProfileMapper.toDto(user.getPortfolioProfile()));

        // 2. Aggregate Sub-sections (Filters for visibility at the mapping stage)

        // Skills
        dto.setSkills(user.getUserSkills().stream()
                .filter(com.forkmyfolio.model.UserSkill::isVisible)
                .map(userSkillMapper::toDto)
                .collect(Collectors.toList()));

        // Projects
        dto.setProjects(user.getProjects().stream()
                .filter(com.forkmyfolio.model.Project::isVisible)
                .map(project -> projectMapper.toDto(project, skillLookup))
                .collect(Collectors.toList()));

        // Experiences
        dto.setExperiences(user.getExperiences().stream()
                .filter(com.forkmyfolio.model.Experience::isVisible)
                .map(experience -> experienceMapper.toDto(experience, skillLookup))
                .collect(Collectors.toList()));

        // Qualifications
        dto.setQualifications(user.getQualifications().stream()
                .filter(com.forkmyfolio.model.Qualification::isVisible)
                .map(qualificationMapper::toDto)
                .collect(Collectors.toList()));

        // Testimonials
        dto.setTestimonials(user.getTestimonials().stream()
                .filter(com.forkmyfolio.model.Testimonial::isVisible)
                .map(testimonialMapper::toDto)
                .collect(Collectors.toList()));

        return dto;
    }
}
