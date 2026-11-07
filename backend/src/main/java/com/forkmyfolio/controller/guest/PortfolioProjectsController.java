package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.ProjectDto;
import com.forkmyfolio.mapper.ProjectMapper;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.UserSkillService;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/portfolios/{slug}/projects")
@Tag(name = "Public Portfolios", description = "Endpoints for viewing public user portfolios.")
@RequiredArgsConstructor
public class PortfolioProjectsController {

    private final PortfolioService portfolioService;
    private final ProjectService projectService;
    private final UserSkillService userSkillService;
    private final ProjectMapper projectMapper;

    @GetMapping
    @Operation(summary = "Get a user's projects by their slug",
            description = "Retrieves all publicly visible projects of a user's portfolio.")
    public ResponseEntity<List<ProjectDto>> getProjectsBySlug(
            @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe")
            @PathVariable String slug) {

        User user = portfolioService.getPublicPortfolioUserBySlug(slug);
        List<Project> projects = projectService.getProjectsForUser(user);

        // The controller gets the context map and passes it to the mapper.
        Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(user);

        // Filter for public visibility and map to DTOs.
        List<ProjectDto> projectDtos = projects.stream()
                .filter(Project::isVisible)
                .map(project -> projectMapper.toDto(project, skillLookup))
                .collect(Collectors.toList());

        return ResponseEntity.ok(projectDtos);
    }
}
