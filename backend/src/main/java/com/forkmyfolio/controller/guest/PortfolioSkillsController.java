package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.UserSkillDto;
import com.forkmyfolio.mapper.UserSkillMapper;
import com.forkmyfolio.service.PortfolioService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/portfolios/{slug}/skills")
@Tag(name = "Public Portfolios", description = "Endpoints for viewing public user portfolios.")
@RequiredArgsConstructor
public class PortfolioSkillsController {

    private final PortfolioService portfolioService;
    private final UserSkillMapper userSkillMapper;

    @GetMapping
    @Operation(summary = "Get a user's skills by their slug",
            description = "Retrieves all publicly visible skills of a user's portfolio.")
    public ResponseEntity<List<UserSkillDto>> getSkillsBySlug(
            @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe")
            @PathVariable String slug) {

        List<UserSkillDto> skillDtos = portfolioService.getVisibleUserSkillsBySlug(slug).stream()
                .map(userSkillMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(skillDtos);
    }
}
