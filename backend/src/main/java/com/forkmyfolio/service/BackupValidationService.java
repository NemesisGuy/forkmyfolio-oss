package com.forkmyfolio.service;

import com.forkmyfolio.model.BackupMetadata;

/**
 * Service responsible for validating backup files before a restore operation.
 */
public interface BackupValidationService {

    /**
     * Validates the metadata of a backup file to ensure it's compatible with the
     * current application version.
     *
     * @param meta         The metadata block from the backup file.
     * @param expectedType The expected type of backup (e.g., "user_backup",
     *                     "system_backup").
     * @throws IllegalArgumentException if the backup file is invalid or
     *                                  incompatible.
     */
    void validateBackup(BackupMetadata meta, String expectedType);
}