package com.forkmyfolio.controller.management;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.create.CreateSkillRequest;
import com.forkmyfolio.dto.response.SkillDto;
import com.forkmyfolio.dto.update.UpdateUserSkillRequest;
import com.forkmyfolio.mapper.SkillMapper;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.service.UserSkillService;
import com.forkmyfolio.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/me/skills")
@Tag(name = "Skill Management (Me)", description = "Endpoints for the authenticated user to manage their skill portfolio.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class SkillManagementController {

    private final UserSkillService userSkillService;
    private final UserService userService;
    private final SkillMapper skillMapper;

    @GetMapping
    @Operation(summary = "Get all of my skills")
    public ResponseEntity<ApiResponseWrapper<List<SkillDto>>> getMySkills() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        List<UserSkill> userSkills = userSkillService.getAllSkillsForUser(currentUser);

        log.info("Found {} skills for user {}. Mapping to detailed DTOs.", userSkills.size(), currentUser.getUuid());

        List<SkillDto> skillDtos = userSkills.stream()
                .map(skillMapper::toDetailDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponseWrapper<>(skillDtos));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get one of my skills by its UserSkill UUID")
    public ResponseEntity<ApiResponseWrapper<SkillDto>> getMySkillByUuid(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        UserSkill userSkill = userSkillService.getSkillForUser(currentUser, uuid);
        SkillDto skillDto = skillMapper.toDetailDto(userSkill);
        return ResponseEntity.ok(new ApiResponseWrapper<>(skillDto));
    }

    @PostMapping
    @Operation(summary = "Add a new skill to my portfolio")
    public ResponseEntity<ApiResponseWrapper<SkillDto>> addSkillToMyPortfolio(
            @Valid @RequestBody CreateSkillRequest createRequest) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // 1. Controller maps the DTO to transient entities.
        Skill skillDetails = skillMapper.toSkillEntity(createRequest);
        UserSkill userSkillDetails = skillMapper.toUserSkillEntity(createRequest);

        // 2. Service layer receives only pure entities.
        UserSkill newUserSkill = userSkillService.addSkillToUser(currentUser, skillDetails, userSkillDetails);

        // 3. Controller maps the resulting entity back to a DTO for the response.
        SkillDto skillDto = skillMapper.toDetailDto(newUserSkill);
        return new ResponseEntity<>(new ApiResponseWrapper<>(skillDto), HttpStatus.CREATED);
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update my relationship with a skill (e.g., proficiency level)")
    public ResponseEntity<ApiResponseWrapper<SkillDto>> updateMySkill(@PathVariable UUID uuid,
            @Valid @RequestBody UpdateUserSkillRequest updateRequest) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // 1. Controller maps the DTO to a transient entity.
        UserSkill userSkillUpdates = skillMapper.toUserSkillEntity(updateRequest);

        // 2. Service layer receives only the pure entity.
        UserSkill updatedUserSkill = userSkillService.updateSkillForUser(uuid, userSkillUpdates, currentUser);

        // 3. Controller maps the resulting entity back to a DTO.
        SkillDto skillDto = skillMapper.toDetailDto(updatedUserSkill);
        return ResponseEntity.ok(new ApiResponseWrapper<>(skillDto));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Remove a skill from my portfolio")
    public ResponseEntity<Void> removeSkillFromMyPortfolio(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        userSkillService.removeSkillFromUser(uuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}