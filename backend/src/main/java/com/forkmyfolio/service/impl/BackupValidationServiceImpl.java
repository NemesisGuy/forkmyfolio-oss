package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.BackupMetadata;
import com.forkmyfolio.service.BackupValidationService;
import org.springframework.stereotype.Service;

@Service
public class BackupValidationServiceImpl implements BackupValidationService {

    @Override
    public void validateBackup(BackupMetadata meta, String expectedType) {
        if (meta == null || meta.getCompatibility() == null) {
            throw new IllegalArgumentException("Invalid backup file: Missing metadata.");
        }

        if (!expectedType.equals(meta.getType())) {
            throw new IllegalArgumentException(
                    "Invalid backup file type. Expected a " + expectedType + " file, but got " + meta.getType() + ".");
        }

        // This logic can be expanded to check version ranges (e.g., using a semantic
        // versioning library)
        // For now, we'll do a simple string comparison for the minimum supported
        // version.
        if (!"2.0.0".equals(meta.getCompatibility().getMinSupportedVersion())) {
            throw new IllegalArgumentException(
                    "Backup file version is not compatible with this version of the application.");
        }
    }
}