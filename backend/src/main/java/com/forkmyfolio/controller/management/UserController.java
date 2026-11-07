package com.forkmyfolio.controller.management;

import com.forkmyfolio.dto.request.UpdatePasswordRequest;
import com.forkmyfolio.dto.response.UserDto;
import com.forkmyfolio.dto.update.UpdateUserAccountRequest;
import com.forkmyfolio.mapper.UserMapper;
import com.forkmyfolio.model.User;
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
@RequestMapping("/api/v1/me")
@Tag(name = "User (Me)", description = "Endpoints for the authenticated user to manage their own profile.")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'GUEST')")
    @Operation(summary = "Get current user's profile", description = "Retrieves the detailed profile information for the currently authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDto> getMyProfile() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        UserDto userDto = userMapper.toDto(currentUser);
        // The ApiResponseWrapper will be applied automatically
        return ResponseEntity.ok(userDto);
    }

    @PutMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update current user's profile", description = "Updates the first name, last name, and profile image URL for the currently authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDto> updateMyProfile(@Valid @RequestBody UpdateUserAccountRequest request) {
        // The service now gets the user from the security context, so we don't need to
        // pass an ID.
        User updatedUser = userService.updateUserProfile(
                request.getFirstName(),
                request.getLastName(),
                request.getProfileImageUrl());

        UserDto updatedDto = userMapper.toDto(updatedUser);
        return ResponseEntity.ok(updatedDto);
    }

    @PostMapping("/password")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Change current user's password", description = "Allows the authenticated user to change their own password. This is required for admins on first login.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.changeCurrentUserPassword(request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept-terms")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Accept Terms and Conditions", description = "Records that the currently authenticated user has accepted the latest Terms and Conditions. This is required on first login or when terms have been updated.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> acceptTerms() {
        userService.acceptTermsForCurrentUser();
        return ResponseEntity.ok().build();
    }
}