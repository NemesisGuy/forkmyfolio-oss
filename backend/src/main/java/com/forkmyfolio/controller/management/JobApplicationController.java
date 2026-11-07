package com.forkmyfolio.controller.management;

import com.forkmyfolio.dto.create.CreateJobApplicationRequest;
import com.forkmyfolio.dto.response.JobApplicationDto;
import com.forkmyfolio.dto.response.JobApplicationNoteDto;
import com.forkmyfolio.dto.update.UpdateJobApplicationRequest;
import com.forkmyfolio.mapper.JobApplicationMapper;
import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.JobApplicationService;
import com.forkmyfolio.service.UserService;
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
@RequestMapping("/api/v1/me/job-applications")
@Tag(name = "Job Application Tracker", description = "Endpoints for managing your personal job hunting operations.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final UserService userService;
    private final JobApplicationMapper jobApplicationMapper;

    @GetMapping
    @Operation(summary = "Get all my job applications", description = "Retrieves a list of all job applications you've logged, sorted by status update date.")
    public ResponseEntity<List<JobApplicationDto>> getAllApplications() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        List<JobApplication> applications = jobApplicationService.getAllApplicationsForUser(currentUser);
        return ResponseEntity.ok(jobApplicationMapper.toDtoList(applications));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get a specific job application", description = "Retrieves details of a single job application by its UUID.")
    public ResponseEntity<JobApplicationDto> getApplication(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        JobApplication application = jobApplicationService.getApplicationByUuid(uuid, currentUser);
        return ResponseEntity.ok(jobApplicationMapper.toDto(application));
    }

    @PostMapping
    @Operation(summary = "Log a new job application", description = "Creates a new job application tracking entry. An optional initial note can be provided.")
    public ResponseEntity<JobApplicationDto> createApplication(
            @Valid @RequestBody CreateJobApplicationRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // Map DTO to transient entity (Rule 2: DTOs converted at boundary)
        JobApplication application = jobApplicationMapper.toEntity(request, currentUser);

        // Call DBR-compliant service
        JobApplication saved = jobApplicationService.createApplication(application, request.getInitialNote(),
                currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationMapper.toDto(saved));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update an existing job application", description = "Updates details or the status of a job application.")
    public ResponseEntity<JobApplicationDto> updateApplication(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateJobApplicationRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        // Map updates to transient entity
        JobApplication updates = jobApplicationMapper.toEntity(request);

        // Call DBR-compliant service; the nullable Boolean preserves the
        // omitted-vs-false distinction the transient entity cannot carry
        // (Phase 8.4 visibility-preservation fix).
        JobApplication updated = jobApplicationService.updateApplication(uuid, updates, request.getVisible(), currentUser);

        return ResponseEntity.ok(jobApplicationMapper.toDto(updated));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a job application", description = "Permanently removes a job application and all associated notes.")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID uuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        jobApplicationService.deleteApplication(uuid, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uuid}/notes")
    @Operation(summary = "Add a note to an application", description = "Creates a new private note for a specific job application.")
    public ResponseEntity<JobApplicationNoteDto> addNote(
            @PathVariable UUID uuid,
            @RequestBody String content) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        JobApplicationNote note = jobApplicationService.addNote(uuid, content, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationMapper.toNoteDto(note));
    }

    @DeleteMapping("/{uuid}/notes/{noteUuid}")
    @Operation(summary = "Delete a note from an application", description = "Permanently removes a specific note.")
    public ResponseEntity<Void> deleteNote(
            @PathVariable UUID uuid,
            @PathVariable UUID noteUuid) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        jobApplicationService.deleteNote(uuid, noteUuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}
