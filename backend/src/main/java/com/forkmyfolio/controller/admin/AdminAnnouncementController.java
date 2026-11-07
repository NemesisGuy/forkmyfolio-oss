package com.forkmyfolio.controller.admin;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.create.CreateAnnouncementRequest;
import com.forkmyfolio.dto.response.AnnouncementDto;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.AnnouncementService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@Tag(name = "Admin Announcements", description = "Endpoints for admins to broadcast system-wide messages.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;
    private final com.forkmyfolio.service.UserService userService;
    private final com.forkmyfolio.mapper.AnnouncementMapper announcementMapper;

    @PostMapping
    @Operation(summary = "Create and broadcast a new announcement")
    public ResponseEntity<ApiResponseWrapper<AnnouncementDto>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {
        User admin = userService.getCurrentAuthenticatedUser();

        com.forkmyfolio.model.Announcement announcement = announcementMapper.toEntity(request);
        com.forkmyfolio.model.Announcement saved = announcementService.createAnnouncement(announcement,
                admin.getUuid());

        return new ResponseEntity<>(new ApiResponseWrapper<>(announcementMapper.toDto(saved)), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all announcements (Admin View)")
    public ResponseEntity<ApiResponseWrapper<List<AnnouncementDto>>> getAllAnnouncements() {
        List<AnnouncementDto> announcements = announcementService.getAllAnnouncements().stream()
                .map(announcementMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(new ApiResponseWrapper<>(announcements));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete an announcement")
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> deleteAnnouncement(@PathVariable UUID uuid) {
        announcementService.deleteAnnouncement(uuid);
        return ResponseEntity.ok(new ApiResponseWrapper<>(Map.of("message", "Announcement deleted successfully.")));
    }
}
