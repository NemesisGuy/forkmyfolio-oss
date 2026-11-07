package com.forkmyfolio.controller.management;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.PortfolioProfileDto;
import com.forkmyfolio.dto.update.UpdatePortfolioProfileRequest;
import com.forkmyfolio.dto.update.UpdateProfileIndexingRequest;
import com.forkmyfolio.dto.update.UpdateProfileVisibilityRequest;
import com.forkmyfolio.mapper.PortfolioProfileMapper;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.PortfolioProfileService;
import com.forkmyfolio.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/profile")
@Tag(name = "Profile Management (Me)", description = "Endpoints for the authenticated user to manage their portfolio profile.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioProfileManagementController {

    private final PortfolioProfileService portfolioProfileService;
    private final UserService userService;
    private final PortfolioProfileMapper portfolioProfileMapper;

    @GetMapping
    @Operation(summary = "Get my portfolio profile")
    public ResponseEntity<ApiResponseWrapper<PortfolioProfileDto>> getMyProfile() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        // The service now safely returns an existing profile or a new, empty one.
        PortfolioProfile profile = portfolioProfileService.getProfileByUser(currentUser);
        PortfolioProfileDto dto = portfolioProfileMapper.toDto(profile);
        return ResponseEntity.ok(new ApiResponseWrapper<>(dto));
    }

    @PutMapping
    @Operation(summary = "Create or update my portfolio profile")
    public ResponseEntity<ApiResponseWrapper<PortfolioProfileDto>> createOrUpdateMyProfile(
            @Valid @RequestBody UpdatePortfolioProfileRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        // The mapper converts the request DTO to a transient entity.
        PortfolioProfile profileUpdates = portfolioProfileMapper.toEntity(request);
        // The service handles the create-or-update logic.
        PortfolioProfile updatedProfile = portfolioProfileService.createOrUpdateProfile(profileUpdates, currentUser);
        PortfolioProfileDto dto = portfolioProfileMapper.toDto(updatedProfile);
        return ResponseEntity.ok(new ApiResponseWrapper<>(dto));
    }

    @PutMapping("/visibility")
    @Operation(summary = "Update my portfolio's public visibility")
    public ResponseEntity<ApiResponseWrapper<PortfolioProfileDto>> updateMyProfileVisibility(
            @Valid @RequestBody UpdateProfileVisibilityRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        // The service handles the update logic.
        PortfolioProfile updatedProfile = portfolioProfileService.updateProfileVisibility(currentUser,
                request.isPubliclyVisible());
        // The mapper converts the result to a full DTO for the response.
        PortfolioProfileDto dto = portfolioProfileMapper.toDto(updatedProfile);
        return ResponseEntity.ok(new ApiResponseWrapper<>(dto));
    }

    /**
     * Phase 10 (Workstream 2): the owner's search-engine indexing preference.
     * Deliberately separate from the visibility endpoint and from the general
     * profile form — the two concerns are independent (a public portfolio can
     * opt out of search indexing while remaining reachable via its URL), and
     * the general profile form does not carry this field so it can never
     * reset it as a side effect.
     */
    @PutMapping("/indexing")
    @Operation(summary = "Update my portfolio's search-engine indexing preference",
            description = "Controls whether search engines may index the public portfolio. "
                    + "Independent of visibility: a publicly accessible portfolio can opt out of search indexing "
                    + "while remaining reachable through its direct URL. The platform-wide indexable switch "
                    + "always takes precedence over this per-user preference.")
    public ResponseEntity<ApiResponseWrapper<PortfolioProfileDto>> updateMyProfileIndexing(
            @Valid @RequestBody UpdateProfileIndexingRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        PortfolioProfile updatedProfile = portfolioProfileService.updateProfileIndexing(currentUser,
                request.isSearchIndexable());
        PortfolioProfileDto dto = portfolioProfileMapper.toDto(updatedProfile);
        return ResponseEntity.ok(new ApiResponseWrapper<>(dto));
    }
}