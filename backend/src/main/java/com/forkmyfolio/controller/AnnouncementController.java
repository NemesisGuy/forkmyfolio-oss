package com.forkmyfolio.controller;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.AnnouncementDto;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@Tag(name = "Announcements", description = "Endpoints for users to view system-wide announcements.")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final com.forkmyfolio.service.UserService userService;
    private final com.forkmyfolio.mapper.AnnouncementMapper announcementMapper;

    @GetMapping
    @Operation(summary = "Get all relevant announcements for the current user's role")
    public ResponseEntity<ApiResponseWrapper<List<AnnouncementDto>>> getMyAnnouncements() {
        User user = userService.getCurrentAuthenticatedUser();

        Set<UUID> readUuids = announcementService.getReadAnnouncementUuids(user);
        List<AnnouncementDto> announcements = announcementMapper.toDtoList(
                announcementService.getAnnouncementsForRole(userService.getEffectiveRoleOfCurrentUser()),
                readUuids);

        return ResponseEntity.ok(new ApiResponseWrapper<>(announcements));
    }

    @PostMapping("/read/{uuid}")
    @Operation(summary = "Mark an announcement as read")
    public ResponseEntity<ApiResponseWrapper<Void>> markRead(@PathVariable UUID uuid) {
        User user = userService.getCurrentAuthenticatedUser();

        announcementService.markAsRead(uuid, user);
        return ResponseEntity.ok(new ApiResponseWrapper<>(null, "Announcement marked as read."));
    }

    @PostMapping("/read/all")
    @Operation(summary = "Mark all announcements as read")
    public ResponseEntity<ApiResponseWrapper<Void>> markAllRead() {
        User user = userService.getCurrentAuthenticatedUser();

        announcementService.markAllAsRead(user);
        return ResponseEntity.ok(new ApiResponseWrapper<>(null, "All announcements marked as read."));
    }
}
