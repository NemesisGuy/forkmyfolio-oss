package com.forkmyfolio.controller.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.advice.SkipApiResponseWrapper;
import com.forkmyfolio.dto.backup.BackupFileDto;
import com.forkmyfolio.dto.backup.BackupMetaDto;
import com.forkmyfolio.dto.response.PortfolioBackupDto;
import com.forkmyfolio.dto.response.UserFullBackupDto;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.BackupService;
import com.forkmyfolio.service.BackupValidationService;
import com.forkmyfolio.service.RestoreService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.model.UserFullBackup;
import com.forkmyfolio.model.PortfolioBackup;
import com.forkmyfolio.mapper.BackupMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/backup")
@Tag(name = "Admin: System Backup & Restore", description = "Endpoints for administrators to perform system-wide backups and restores.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminBackupController {

    // Services for core logic
    private final BackupValidationService backupValidationService;
    private final RestoreService restoreService;
    private final UserService userService;
    private final BackupService backupService;
    private final BackupMapper backupMapper;

    // Mappers for DTO conversion
    private final ObjectMapper objectMapper;
    private final com.forkmyfolio.security.JwtTokenProvider jwtTokenProvider;

    /**
     * Enforces password re-verification for sensitive system backup operations.
     * Requires a valid, unexpired backup-scoped token bound to the caller.
     */
    private void requireBackupReVerification(com.forkmyfolio.model.User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new com.forkmyfolio.exception.PermissionDeniedException(
                    "Password re-verification required for backup operations.");
        }
        jakarta.servlet.http.HttpServletRequest currentRequest = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes() == null ? null
                : ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                        .getRequestAttributes()).getRequest();
        String token = currentRequest == null ? null : currentRequest.getHeader("X-Backup-Token");
        if (token == null || !jwtTokenProvider.isBackupTokenValid(token, currentUser.getId())) {
            throw new com.forkmyfolio.exception.PermissionDeniedException(
                    "Password re-verification required for backup operations.");
        }
    }

    @Value("${app.version:2.0.0}")
    private String appVersion;

    @GetMapping
    @Operation(summary = "Create a full system backup", description = "Downloads a versioned JSON file containing all data for all users in the system.")
    @SkipApiResponseWrapper
    public ResponseEntity<byte[]> downloadSystemBackup() throws IOException {
        log.info("Admin request for system-wide backup initiated.");

        // Use the service to create a transactional backup of all users (domain
        // objects).
        requireBackupReVerification(userService.getCurrentAuthenticatedUser());
        List<UserFullBackup> systemBackupData = backupService.createFullSystemBackup();

        // Map to DTOs at the boundary
        List<UserFullBackupDto> systemBackupDtos = backupMapper.toDtoList(systemBackupData);

        log.info("Successfully generated backup data for {} users.", systemBackupDtos.size());

        BackupMetaDto meta = BackupMetaDto.builder()
                .version(appVersion)
                .exportedAt(ZonedDateTime.now())
                .type("system_backup")
                .compatibility(BackupMetaDto.Compatibility.builder()
                        .minSupportedVersion("2.0.0")
                        .maxSupportedVersion("2.x")
                        .build())
                .system("ForkMyFolio")
                .build();

        BackupFileDto<List<UserFullBackupDto>> backupFile = new BackupFileDto<>(meta, systemBackupDtos);

        String filename = String.format("forkmyfolio-system-backup-%s.json",
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        byte[] jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(backupFile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok().headers(headers).body(jsonContent);
    }

    @PostMapping("/restore/system")
    @Operation(summary = "Restore the entire system from a backup", description = "Upload a system backup file. THIS IS A DESTRUCTIVE OPERATION and will wipe all existing data before restoring.")
    public ResponseEntity<Void> restoreSystemFromBackup(@RequestParam("file") MultipartFile file) throws IOException {
        log.warn("Admin request for SYSTEM-WIDE restore initiated. THIS IS A DESTRUCTIVE OPERATION.");
        if (file.isEmpty()
                || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(file.getContentType()))) {
            throw new IllegalArgumentException("Invalid file. Please upload a valid JSON backup file.");
        }

        BackupFileDto<List<UserFullBackupDto>> backupFile = objectMapper.readValue(file.getInputStream(),
                new TypeReference<>() {
                });

        backupValidationService.validateBackup(backupMapper.toDomain(backupFile.getMeta()), "system_backup");

        // Map to domain at the boundary
        requireBackupReVerification(userService.getCurrentAuthenticatedUser());
        List<UserFullBackup> domainBackup = backupMapper.toDomainList(backupFile.getData());

        restoreService.restoreSystemFromBackup(domainBackup);

        log.warn("System restore completed successfully. {} users restored.", backupFile.getData().size());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore/user/{userUuid}")
    @Operation(summary = "Restore a single user's portfolio from a backup", description = "Upload a standard user backup file to restore a specific user's portfolio. This is a destructive action for the target user only.")
    public ResponseEntity<Void> restoreSingleUser(
            @Parameter(description = "The UUID of the user to restore.") @PathVariable UUID userUuid,
            @RequestParam("file") MultipartFile file) throws IOException {

        log.warn("Admin request to restore a single user's portfolio for UUID: {}", userUuid);
        if (file.isEmpty()
                || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(file.getContentType()))) {
            throw new IllegalArgumentException("Invalid file. Please upload a valid JSON backup file.");
        }

        User targetUser = userService.getUserByUuid(userUuid);
        BackupFileDto<PortfolioBackupDto> backupFile = objectMapper.readValue(file.getInputStream(),
                new TypeReference<>() {
                });

        backupValidationService.validateBackup(backupMapper.toDomain(backupFile.getMeta()), "user_backup");

        // Map to domain at the boundary
        PortfolioBackup domainBackup = backupMapper.toDomain(backupFile.getData(), targetUser);

        restoreService.restoreUserFromBackup(targetUser, domainBackup);

        log.info("Successfully restored portfolio for user: {}", targetUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/wipe")
    @Operation(summary = "Wipe all data from the system", description = "THIS IS A HIGHLY DESTRUCTIVE OPERATION. It deletes all users, portfolios, skills, and other associated data from the database. Use with extreme caution.")
    public ResponseEntity<Void> wipeSystemData() {
        log.warn("ADMIN-INITIATED DATA WIPE. ALL DATA WILL BE DELETED.");
        restoreService.wipeAllData();
        log.warn("System data wipe completed successfully.");
        return ResponseEntity.noContent().build();
    }
}