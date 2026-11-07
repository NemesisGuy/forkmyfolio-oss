package com.forkmyfolio.controller.guest;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.UserSettingDto;
import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.mapper.UserSettingMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.impl.UserSettingService;
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

@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Public Portfolios", description = "Endpoints for viewing public user portfolios.")
@RequiredArgsConstructor
public class PortfolioSettingsController {

    private final UserService userService;
    private final UserSettingService userSettingService;
    private final UserSettingMapper userSettingMapper;

    @GetMapping("/{slug}/settings")
    @Operation(summary = "Get public settings for a portfolio", description = "Retrieves the list of effective settings for a user's public portfolio, identified by their unique slug.")
    public ResponseEntity<ApiResponseWrapper<List<UserSettingDto>>> getPortfolioSettings(
            @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe") @PathVariable String slug) {
        User user = userService.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio " + "slug: " + slug));

        // Aggregate at the boundary
        List<UserSettingDto> settings = userSettingMapper.toDtoList(
                userSettingService.getAllGlobalSettings(),
                userSettingService.getUserOverrides(user));

        return ResponseEntity.ok(new ApiResponseWrapper<>(settings));
    }
}