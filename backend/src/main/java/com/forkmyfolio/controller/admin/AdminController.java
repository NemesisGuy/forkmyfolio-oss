package com.forkmyfolio.controller.admin;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.create.AdminCreateUserRequest;
import com.forkmyfolio.dto.request.AdminResetPasswordRequest;
import com.forkmyfolio.dto.response.AdminStatsDto;
import com.forkmyfolio.dto.response.ContactMessageDto;
import com.forkmyfolio.dto.response.SettingDto;
import com.forkmyfolio.dto.response.UserDto;
import com.forkmyfolio.dto.update.AdminUpdateUserRequest;
import com.forkmyfolio.dto.update.UpdateSettingRequest;
import com.forkmyfolio.mapper.ContactMessageMapper;
import com.forkmyfolio.mapper.SettingMapper;
import com.forkmyfolio.mapper.StatsMapper;
import com.forkmyfolio.mapper.UserMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.ContactMessageService;
import com.forkmyfolio.service.SettingService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.impl.VisitorStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for handling all administrative tasks.
 * All endpoints in this controller require ADMIN role privileges.
 * It covers user management, application settings, statistics, and system-wide
 * message management.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Management", description = "Endpoints for administrative tasks.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final VisitorStatsService visitorStatsService;
    private final SettingService settingService;
    private final SettingMapper settingMapper;
    private final ContactMessageService contactMessageService;
    private final ContactMessageMapper contactMessageMapper;
    private final StatsMapper statsMapper;

    // --- User Management ---

    /**
     * Retrieves a paginated list of all users in the system.
     *
     * @param pageable Pagination information (page, size, sort).
     * @return A {@link ResponseEntity} containing a {@link Page} of {@link UserDto}
     *         objects.
     */
    @GetMapping("/users")
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        Page<User> userPage = userService.getAllUsers(pageable);
        Page<UserDto> userDtoPage = userPage.map(userMapper::toDto);
        return ResponseEntity.ok(userDtoPage);
    }

    /**
     * Creates a new user with specified roles and activation status.
     * 
     * @param request The request body containing the new user's details.
     * @return A {@link ResponseEntity} with the created {@link UserDto} and HTTP
     *         status 201 (Created).
     */
    @PostMapping("/users")
    @Operation(summary = "Create a new user (Admin)")
    public ResponseEntity<ApiResponseWrapper<UserDto>> createAdminUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        User newUser = userService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                null, // profileImageUrl can be null for admin creation
                request.getRoles(),
                request.getActive());
        return new ResponseEntity<>(new ApiResponseWrapper<>(userMapper.toDto(newUser)), HttpStatus.CREATED);
    }

    /**
     * Retrieves a single user by their unique identifier (UUID).
     *
     * @param userId The UUID of the user to retrieve.
     * @return A {@link ResponseEntity} containing the {@link UserDto}.
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get a single user by UUID")
    public ResponseEntity<ApiResponseWrapper<UserDto>> getUserByUuid(@PathVariable UUID userId) {
        User user = userService.getUserByUuid(userId);
        return ResponseEntity.ok(new ApiResponseWrapper<>(userMapper.toDto(user)));
    }

    /**
     * Updates an existing user's details, such as name, slug, roles, and activation
     * status.
     * 
     * @param userId  The UUID of the user to update.
     * @param request The request body containing the fields to update.
     * @return A {@link ResponseEntity} with the updated {@link UserDto}.
     */
    @PutMapping("/users/{userId}")
    @Operation(summary = "Update a user's details (Admin)")
    public ResponseEntity<ApiResponseWrapper<UserDto>> updateUserByAdmin(@PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        User updatedUser = userService.updateUserByAdmin(
                userId,
                request.getFirstName(),
                request.getLastName(),
                request.getSlug(),
                request.getRoles(),
                request.getActive());
        return ResponseEntity.ok(new ApiResponseWrapper<>(userMapper.toDto(updatedUser)));
    }

    /**
     * Resets a user's password. This is the recovery path for accounts that
     * arrive without a usable credential (e.g. legacy V2 backups).
     *
     * @param userId The UUID of the target user.
     * @param request The new password. Encoded server-side; never logged.
     * @return A {@link ResponseEntity} with a success message.
     */
    @PostMapping("/users/{userId}/reset-password")
    @Operation(summary = "Reset a user's password (Admin)")
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> resetUserPassword(
            @PathVariable UUID userId, @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.resetUserPassword(userId, request.getNewPassword());
        Map<String, String> response = Map.of("message", "Password reset successfully.");
        return ResponseEntity.ok(new ApiResponseWrapper<>(response));
    }

    /**
     * Deactivates a user, performing a soft delete. The user is marked as inactive
     * but not removed from the database.
     * 
     * @param userId The UUID of the user to deactivate.
     * @return A {@link ResponseEntity} with a success message.
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Deactivate a user (Soft Delete)")
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        Map<String, String> response = Map.of("message", "User deactivated successfully.");
        return ResponseEntity.ok(new ApiResponseWrapper<>(response));
    }

    // --- Statistics Management ---

    /**
     * Retrieves aggregated visitor and application statistics.
     *
     * @return A {@link ResponseEntity} containing the {@link AdminStatsDto}.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get all visitor statistics")
    public ResponseEntity<ApiResponseWrapper<AdminStatsDto>> getVisitorStats() {
        AdminStatsDto dto = statsMapper.toAdminStatsDto(visitorStatsService.getAllStats());
        return ResponseEntity.ok(new ApiResponseWrapper<>(dto));
    }

    // --- Application Settings Management ---

    /**
     * Retrieves a list of all global application settings.
     * 
     * @return A {@link ResponseEntity} containing a list of {@link SettingDto}
     *         objects.
     */
    @GetMapping("/settings")
    @Operation(summary = "Get all application settings")
    public ResponseEntity<ApiResponseWrapper<List<SettingDto>>> getAllSettings() {
        var settings = settingService.getAllSettings();
        return ResponseEntity.ok(new ApiResponseWrapper<>(settingMapper.toDtoList(settings)));
    }

    /**
     * Updates multiple application settings in a single batch operation.
     * 
     * @param updateRequests A list of setting update requests, each containing a
     *                       UUID and a new value.
     * @return A {@link ResponseEntity} with the list of updated {@link SettingDto}
     *         objects.
     */
    @PutMapping("/settings")
    @Operation(summary = "Update multiple application settings")
    public ResponseEntity<ApiResponseWrapper<List<SettingDto>>> updateSettings(
            @RequestBody @Valid List<UpdateSettingRequest> updateRequests) {
        Map<UUID, String> settingsToUpdate = updateRequests.stream()
                .collect(Collectors.toMap(UpdateSettingRequest::getUuid, UpdateSettingRequest::getValue));
        var updatedSettings = settingService.updateSettings(settingsToUpdate);
        return ResponseEntity.ok(new ApiResponseWrapper<>(settingMapper.toDtoList(updatedSettings)));
    }

    // --- System-wide Contact Message Management ---

    /**
     * Retrieves all contact messages received by all users across the platform.
     * 
     * @return A {@link ResponseEntity} containing a list of all
     *         {@link ContactMessageDto} objects.
     */
    @GetMapping("/contact-messages")
    @Operation(summary = "Get all contact messages from all users")
    public ResponseEntity<ApiResponseWrapper<List<ContactMessageDto>>> getAllContactMessages() {
        List<ContactMessageDto> messages = contactMessageMapper.toDtoList(contactMessageService.findAll());
        return ResponseEntity.ok(new ApiResponseWrapper<>(messages));
    }

    /**
     * Deletes any contact message from the system by its unique identifier (UUID).
     * 
     * @param uuid The UUID of the contact message to delete.
     * @return A {@link ResponseEntity} with a success message.
     */
    @DeleteMapping("/contact-messages/{uuid}")
    @Operation(summary = "Delete any contact message by its UUID")
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> deleteContactMessage(@PathVariable UUID uuid) {
        contactMessageService.deleteByUuid(uuid);
        Map<String, String> response = Map.of("message", "Message deleted successfully.");
        return ResponseEntity.ok(new ApiResponseWrapper<>(response));
    }
}