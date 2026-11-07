package com.forkmyfolio.controller.management;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.UserSettingDto;
import com.forkmyfolio.dto.update.UpdateUserSettingRequest;
import com.forkmyfolio.mapper.UserSettingMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.impl.UserSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/me/settings")
@Tag(name = "My Settings", description = "Endpoints for managing the authenticated user's personal settings.")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;
    private final UserSettingMapper userSettingMapper;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get my effective settings", description = "Retrieves a list of all settings applicable to the authenticated user, combining global defaults with their personal overrides.")
    public ResponseEntity<ApiResponseWrapper<List<UserSettingDto>>> getMySettings() {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // Aggregate at the boundary
        List<UserSettingDto> settings = userSettingMapper.toDtoList(
                userSettingService.getAllGlobalSettings(),
                userSettingService.getUserOverrides(currentUser));

        return ResponseEntity.ok(new ApiResponseWrapper<>(settings));
    }

    @PutMapping
    @Operation(summary = "Update my settings", description = "Updates one or more settings for the authenticated user. This is a bulk update operation.")
    public ResponseEntity<ApiResponseWrapper<List<UserSettingDto>>> updateMySettings(
            @Valid @RequestBody List<UpdateUserSettingRequest> requests) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // Convert request DTOs to internal map at the boundary
        Map<UUID, String> updates = requests.stream()
                .collect(Collectors.toMap(UpdateUserSettingRequest::getUuid, UpdateUserSettingRequest::getValue));

        userSettingService.updateUserSettings(currentUser, updates);

        // Return refreshed list
        List<UserSettingDto> updatedSettings = userSettingMapper.toDtoList(
                userSettingService.getAllGlobalSettings(),
                userSettingService.getUserOverrides(currentUser));

        return ResponseEntity.ok(new ApiResponseWrapper<>(updatedSettings));
    }
}