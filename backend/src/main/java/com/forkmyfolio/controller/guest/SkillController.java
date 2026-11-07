package com.forkmyfolio.controller.guest;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.SkillDto;
import com.forkmyfolio.mapper.SkillMapper;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/skills")
@Tag(name = "Skills (Platform)", description = "Endpoints for accessing the global pool of skills.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class SkillController {

    private final SkillService skillService;
    private final SkillMapper skillMapper;

    @GetMapping
    @Operation(summary = "Get all available platform skills", description = "Retrieves a list of all skills available on the platform for users to add to their portfolios.")
    public ResponseEntity<ApiResponseWrapper<List<SkillDto>>> getAllPlatformSkills() {
        List<Skill> skills = skillService.getAllPlatformSkills();
        List<SkillDto> skillDtos = skills.stream()
                // FIX: Use the correct mapper for global skills that have no user-specific context.
                .map(skillMapper::toBasicDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponseWrapper<>(skillDtos));
    }
}