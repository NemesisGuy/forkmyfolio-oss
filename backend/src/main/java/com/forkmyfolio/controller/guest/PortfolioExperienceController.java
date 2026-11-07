package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.ExperienceDto;
import com.forkmyfolio.mapper.ExperienceMapper;
import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.service.UserSkillService;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.ExperienceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/portfolios/{slug}/experience")
public class PortfolioExperienceController {

    private final PortfolioService portfolioService;
    private final ExperienceService experienceService;
    private final UserSkillService userSkillService;
    private final ExperienceMapper experienceMapper;

    public PortfolioExperienceController(PortfolioService portfolioService, ExperienceService experienceService, UserSkillService userSkillService, ExperienceMapper experienceMapper) {
        this.portfolioService = portfolioService;
        this.experienceService = experienceService;
        this.userSkillService = userSkillService;
        this.experienceMapper = experienceMapper;
    }

    @GetMapping
    public ResponseEntity<List<ExperienceDto>> getPortfolioExperience(@PathVariable String slug) {
        User user = portfolioService.getPublicPortfolioUserBySlug(slug);
        List<Experience> experiences = experienceService.getExperiencesForUser(user);

        // The controller gets the context map and passes it to the mapper.
        Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(user);

        // Filter for public visibility and map to DTOs.
        List<ExperienceDto> experienceDtos = experiences.stream()
                .filter(Experience::isVisible)
                .map(experience -> experienceMapper.toDto(experience, skillLookup))
                .collect(Collectors.toList());

        return ResponseEntity.ok(experienceDtos);
    }
}
