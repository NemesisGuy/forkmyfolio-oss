package com.forkmyfolio.controller.management;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.create.CreateProjectRequest;
import com.forkmyfolio.dto.response.ProjectDto;
import com.forkmyfolio.dto.update.UpdateProjectRequest;
import com.forkmyfolio.mapper.ProjectMapper;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.service.ProjectService;
import com.forkmyfolio.service.UserSkillService;
import com.forkmyfolio.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/me/projects")
@Tag(name = "Project Management (Me)", description = "Endpoints for the authenticated user to manage their own portfolio projects.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class ProjectManagementController {

    private final ProjectService projectService;
    private final UserService userService;
    private final UserSkillService userSkillService;
    private final ProjectMapper projectMapper;

    @GetMapping
    @Operation(summary = "Get all of my projects")
    public ResponseEntity<ApiResponseWrapper<List<ProjectDto>>> getMyProjects() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        List<Project> projects = projectService.getProjectsForUser(currentUser);

        Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(currentUser);

        List<ProjectDto> projectDtos = projects.stream()
                .map(project -> projectMapper.toDto(project, skillLookup))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponseWrapper<>(projectDtos));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get one of my projects by its UUID")
    public ResponseEntity<ApiResponseWrapper<ProjectDto>> getMyProjectByUuid(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Project project = projectService.findProjectByUuidAndUser(uuid, currentUser);

        Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(currentUser);
        ProjectDto projectDto = projectMapper.toDto(project, skillLookup);

        return ResponseEntity.ok(new ApiResponseWrapper<>(projectDto));
    }

    @PostMapping
    @Operation(summary = "Create a new project for myself")
    public ResponseEntity<ApiResponseWrapper<ProjectDto>> createMyProject(@Valid @RequestBody CreateProjectRequest createRequest) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Project newProjectDetails = projectMapper.toEntity(createRequest);

        Project createdProject = projectService.createProject(newProjectDetails, createRequest.getKeyFeatures(), currentUser,
                createRequest.getSkills());

        Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(currentUser);
        ProjectDto createdDto = projectMapper.toDto(createdProject, skillLookup);
        return new ResponseEntity<>(new ApiResponseWrapper<>(createdDto), HttpStatus.CREATED);
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update one of my projects")
    public ResponseEntity<ApiResponseWrapper<ProjectDto>> updateMyProject(@PathVariable UUID uuid, @Valid @RequestBody UpdateProjectRequest updateRequest) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Project updatedProjectData = projectMapper.toEntity(updateRequest);

        Project updatedProject = projectService.updateProject(uuid, updatedProjectData, updateRequest.getKeyFeatures(),
                updateRequest.getSkills(), currentUser);

        Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(currentUser);
        ProjectDto updatedDto = projectMapper.toDto(updatedProject, skillLookup);
        return ResponseEntity.ok(new ApiResponseWrapper<>(updatedDto));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete one of my projects")
    public ResponseEntity<Void> deleteMyProject(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        projectService.deleteProject(uuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}