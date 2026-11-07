package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.advice.SkipApiResponseWrapper;
import com.forkmyfolio.dto.backup.BackupFileDto;
import com.forkmyfolio.dto.backup.BackupMetaDto;
import com.forkmyfolio.dto.response.PortfolioBackupDto;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.BackupService;
import com.forkmyfolio.service.BackupValidationService;
import com.forkmyfolio.service.RestoreService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.model.PortfolioBackup;
import com.forkmyfolio.mapper.BackupMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/me/backup")
@Tag(name = "Backup & Restore (Me)", description = "Endpoints for the authenticated user to backup and restore their portfolio data.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class BackupController {

        private final BackupService backupService;
        private final UserService userService;
        private final RestoreService restoreService;
        private final BackupValidationService backupValidationService;
        private final BackupMapper backupMapper;
        private final ObjectMapper objectMapper;
        private final com.forkmyfolio.security.JwtTokenProvider jwtTokenProvider;

        /**
         * Enforces password re-verification for sensitive backup operations.
         * Requires a valid, unexpired backup-scoped token bound to the caller.
         */
        private void requireBackupReVerification(User currentUser) {
                if (currentUser == null || currentUser.getId() == null) {
                        throw new com.forkmyfolio.exception.PermissionDeniedException(
                                        "Password re-verification required for backup operations.");
                }
                jakarta.servlet.http.HttpServletRequest currentRequest = org.springframework.web.context.request.RequestContextHolder
                                .getRequestAttributes() == null ? null
                                : ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                                                .getRequestAttributes()).getRequest();
                String token = currentRequest == null ? null
                                : currentRequest.getHeader("X-Backup-Token");
                if (token == null || !jwtTokenProvider.isBackupTokenValid(token, currentUser.getId())) {
                        throw new com.forkmyfolio.exception.PermissionDeniedException(
                                        "Password re-verification required for backup operations.");
                }
        }

        @Value("${app.version:2.0.0}")
        private String appVersion;

        @GetMapping
        @Operation(summary = "Backup my entire portfolio", description = "Downloads a versioned JSON file containing all of the authenticated user's portfolio data.")
        @SkipApiResponseWrapper
        public ResponseEntity<byte[]> downloadBackup() throws IOException {
                // Fetch the current user
                User currentUser = userService.getCurrentAuthenticatedUser();

                // Use the transactional service to create the backup domain object.
                requireBackupReVerification(currentUser);
                PortfolioBackup backupData = backupService.createBackupForUser(currentUser.getUuid());

                // Map to DTO at the boundary
                PortfolioBackupDto backupDto = backupMapper.toDto(backupData);

                BackupMetaDto meta = BackupMetaDto.builder()
                                .version(appVersion)
                                .exportedAt(ZonedDateTime.now())
                                .type("user_backup")
                                .compatibility(BackupMetaDto.Compatibility.builder()
                                                .minSupportedVersion("2.0.0")
                                                .maxSupportedVersion("2.x")
                                                .build())
                                .exportedBy(currentUser.getSlug())
                                .system("ForkMyFolio")
                                .build();

                BackupFileDto<PortfolioBackupDto> backupFile = new BackupFileDto<>(meta, backupDto);

                String filename = String.format("forkmyfolio-backup-%s-%s.json", currentUser.getSlug(),
                                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                byte[] jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(backupFile);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setContentDispositionFormData("attachment", filename);

                return ResponseEntity.ok()
                                .headers(headers)
                                .body(jsonContent);
        }

        @PostMapping("/restore")
        @Operation(summary = "Restore my portfolio from a backup", description = "Upload a versioned JSON backup file to restore portfolio data. This is a destructive action and will replace existing data.")
        public ResponseEntity<Void> restoreFromBackup(@RequestParam("file") MultipartFile file) throws IOException {
                if (file.isEmpty() || !MediaType.APPLICATION_JSON
                                .isCompatibleWith(MediaType.parseMediaType(file.getContentType()))) {
                        throw new IllegalArgumentException("Invalid file. Please upload a valid JSON backup file.");
                }

                BackupFileDto<PortfolioBackupDto> backupFile = objectMapper.readValue(file.getInputStream(),
                                new TypeReference<>() {
                                });

                backupValidationService.validateBackup(backupMapper.toDomain(backupFile.getMeta()), "user_backup");

                // Map to domain at the boundary
                User currentUser = userService.getCurrentAuthenticatedUser();
                requireBackupReVerification(currentUser);
                PortfolioBackup domainBackup = backupMapper.toDomain(backupFile.getData(), currentUser);

                restoreService.restoreFromBackup(domainBackup);

                return ResponseEntity.noContent().build();
        }

}
