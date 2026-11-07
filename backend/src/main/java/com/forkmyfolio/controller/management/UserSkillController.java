package com.forkmyfolio.controller.management;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.create.CreateSkillRequest;
import com.forkmyfolio.dto.response.UserSkillDto;
import com.forkmyfolio.dto.update.UpdateUserSkillRequest;
import com.forkmyfolio.mapper.SkillMapper;
import com.forkmyfolio.mapper.UserSkillMapper;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.UserSkillService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/skills")
@RequiredArgsConstructor
@Tag(name = "User Skills", description = "Endpoints for managing the authenticated user's skills.")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class UserSkillController {

    private final UserSkillService userSkillService;
    private final UserService userService;
    private final UserSkillMapper userSkillMapper;
    private final SkillMapper skillMapper;

    @GetMapping
    @Operation(summary = "Get all skills for the current user", description = "Retrieves a list of all skills associated with the authenticated user.")
    public ResponseEntity<ApiResponseWrapper<List<UserSkillDto>>> getAllUserSkills() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        List<UserSkill> skills = userSkillService.getAllSkillsForUser(currentUser);
        List<UserSkillDto> skillDtos = userSkillMapper.toDtoList(skills);
        return ResponseEntity.ok(new ApiResponseWrapper<>(skillDtos));
    }

    @PostMapping
    @Operation(summary = "Add a new skill to the current user's profile", description = "Creates a new skill relationship for the authenticated user. If the skill doesn't exist globally, it will be created.")
    public ResponseEntity<ApiResponseWrapper<UserSkillDto>> createUserSkill(@Valid @RequestBody CreateSkillRequest createDto) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // Map DTOs to transient entities in the controller
        Skill skillDetails = skillMapper.toSkillEntity(createDto);
        UserSkill userSkillDetails = skillMapper.toUserSkillEntity(createDto);

        // Call service with pure entities
        UserSkill newUserSkill = userSkillService.addSkillToUser(currentUser, skillDetails, userSkillDetails);

        // Map result entity back to DTO for the response
        UserSkillDto newSkillDto = userSkillMapper.toDto(newUserSkill);
        return new ResponseEntity<>(new ApiResponseWrapper<>(newSkillDto), HttpStatus.CREATED);
    }

    @PutMapping("/{userSkillId}")
    @Operation(summary = "Update a user's skill", description = "Updates details of a specific skill relationship for the authenticated user, such as proficiency level or visibility.")
    public ResponseEntity<ApiResponseWrapper<UserSkillDto>> updateUserSkill(
            @PathVariable UUID userSkillId,
            @Valid @RequestBody UpdateUserSkillRequest updateDto) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // Map DTO to transient entity
        UserSkill userSkillUpdates = skillMapper.toUserSkillEntity(updateDto);

        // Call service with pure entities
        UserSkill updatedUserSkill = userSkillService.updateSkillForUser(userSkillId, userSkillUpdates, currentUser);

        // Map result entity back to DTO
        UserSkillDto updatedSkillDto = userSkillMapper.toDto(updatedUserSkill);
        return ResponseEntity.ok(new ApiResponseWrapper<>(updatedSkillDto));
    }

    @DeleteMapping("/{userSkillId}")
    @Operation(summary = "Remove a skill from the current user's profile", description = "Deletes a skill relationship for the authenticated user.")
    public ResponseEntity<Void> deleteUserSkill(@PathVariable UUID userSkillId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        userSkillService.removeSkillFromUser(userSkillId, currentUser);
        return ResponseEntity.noContent().build();
    }
}