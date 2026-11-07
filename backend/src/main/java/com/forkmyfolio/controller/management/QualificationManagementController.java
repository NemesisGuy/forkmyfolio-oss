package com.forkmyfolio.controller.management;

import com.forkmyfolio.dto.create.CreateQualificationRequest;
import com.forkmyfolio.dto.response.QualificationDto;
import com.forkmyfolio.dto.update.UpdateQualificationRequest;
import com.forkmyfolio.mapper.QualificationMapper;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.service.QualificationService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/qualifications")
@Tag(name = "Qualification Management (Me)", description = "Endpoints for the authenticated user to manage their own qualifications.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class QualificationManagementController {

    private final QualificationService qualificationService;
    private final com.forkmyfolio.service.UserService userService;
    private final QualificationMapper qualificationMapper;

    @GetMapping
    @Operation(summary = "Get all of my qualifications")
    public ResponseEntity<List<QualificationDto>> getMyQualifications() {
        List<Qualification> qualifications = qualificationService.getQualificationsForCurrentUser();
        return ResponseEntity.ok(qualificationMapper.toDtoList(qualifications));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get one of my qualifications by its UUID")
    public ResponseEntity<QualificationDto> getMyQualificationByUuid(@PathVariable UUID uuid) {
        Qualification qualification = qualificationService.getQualificationByUuidForCurrentUser(uuid);
        return ResponseEntity.ok(qualificationMapper.toDto(qualification));
    }

    @PostMapping
    @Operation(summary = "Create a new qualification for myself")
    public ResponseEntity<QualificationDto> createMyQualification(
            @Valid @RequestBody CreateQualificationRequest request) {
        com.forkmyfolio.model.User currentUser = userService.getCurrentAuthenticatedUser();
        Qualification newQualification = qualificationService.createQualification(
                request.getQualificationName(),
                request.getInstitutionName(),
                request.getInstitutionLogoUrl(),
                request.getInstitutionWebsite(),
                request.getFieldOfStudy(),
                request.getLevel(),
                request.getStartYear(),
                request.getCompletionYear(),
                request.getStillStudying(),
                request.getGrade(),
                request.getCredentialUrl(),
                request.isVisible(),
                currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(qualificationMapper.toDto(newQualification));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update one of my qualifications")
    public ResponseEntity<QualificationDto> updateMyQualification(@PathVariable UUID uuid,
            @Valid @RequestBody UpdateQualificationRequest request) {
        com.forkmyfolio.model.User currentUser = userService.getCurrentAuthenticatedUser();
        Qualification updatedQualification = qualificationService.updateQualification(
                uuid,
                request.getQualificationName(),
                request.getInstitutionName(),
                request.getInstitutionLogoUrl(),
                request.getInstitutionWebsite(),
                request.getFieldOfStudy(),
                request.getLevel(),
                request.getStartYear(),
                request.getCompletionYear(),
                request.getStillStudying(),
                request.getGrade(),
                request.getCredentialUrl(),
                request.getVisible(),
                currentUser);
        return ResponseEntity.ok(qualificationMapper.toDto(updatedQualification));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete one of my qualifications")
    public ResponseEntity<Void> deleteMyQualification(@PathVariable UUID uuid) {
        com.forkmyfolio.model.User currentUser = userService.getCurrentAuthenticatedUser();
        qualificationService.deleteQualification(uuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}
