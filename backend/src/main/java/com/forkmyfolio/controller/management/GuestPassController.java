package com.forkmyfolio.controller.management;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.GuestPassDto;
import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;
import com.forkmyfolio.mapper.GuestPassMapper;
import com.forkmyfolio.service.GuestPassService;
import com.forkmyfolio.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/management/guest-passes")
@RequiredArgsConstructor
@Tag(name = "Guest Pass Management", description = "Endpoints for managing temporary portfolio access tokens")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@Slf4j
public class GuestPassController {

    private final GuestPassService guestPassService;
    private final UserService userService;
    private final GuestPassMapper guestPassMapper;

    @PostMapping
    @Operation(summary = "Create a new guest pass")
    public ResponseEntity<ApiResponseWrapper<GuestPassDto>> createGuestPass(
            @RequestParam String label,
            @RequestParam long durationInHours) {

        User currentUser = userService.getCurrentAuthenticatedUser();
        Instant expiresAt = Instant.now().plusSeconds(durationInHours * 3600);

        GuestPass created = guestPassService.createGuestPass(currentUser, label, expiresAt);
        return ResponseEntity.ok(new ApiResponseWrapper<>(guestPassMapper.toDto(created)));
    }

    @GetMapping
    @Operation(summary = "List my guest passes")
    public ResponseEntity<ApiResponseWrapper<List<GuestPassDto>>> getMyGuestPasses() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        List<GuestPass> passes = guestPassService.getMyGuestPasses(currentUser);
        List<GuestPassDto> dtos = passes.stream()
                .map(guestPassMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponseWrapper<>(dtos));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Revoke a guest pass")
    public ResponseEntity<Void> revokeGuestPass(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        log.info("Received DELETE request to revoke guest pass {} from user {}", uuid, currentUser.getEmail());
        guestPassService.revokeGuestPass(currentUser, uuid);
        return ResponseEntity.noContent().build();
    }
}
