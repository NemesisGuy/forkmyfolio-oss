package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.BackupMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackupValidationServiceImplTest {

    private BackupValidationServiceImpl validationService;
    private BackupMetadata metadata;

    @BeforeEach
    void setUp() {
        validationService = new BackupValidationServiceImpl();
        metadata = BackupMetadata.builder()
                .type("system_backup")
                .compatibility(BackupMetadata.Compatibility.builder()
                        .minSupportedVersion("2.0.0")
                        .build())
                .build();
    }

    @Test
    void validateBackup_withValidMeta_shouldNotThrowException() {
        assertDoesNotThrow(() -> {
            validationService.validateBackup(metadata, "system_backup");
        });
    }

    @Test
    void validateBackup_withNullMeta_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateBackup(null, "system_backup");
        });
    }

    @Test
    void validateBackup_withNullCompatibility_shouldThrowIllegalArgumentException() {
        metadata.setCompatibility(null);
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateBackup(metadata, "system_backup");
        });
    }

    @Test
    void validateBackup_withMismatchedType_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateBackup(metadata, "user_backup");
        });
    }

    @Test
    void validateBackup_withIncompatibleVersion_shouldThrowIllegalArgumentException() {
        metadata.getCompatibility().setMinSupportedVersion("1.0.0");
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateBackup(metadata, "system_backup");
        });
    }
}
