package com.forkmyfolio.service;

import java.util.List;

/**
 * Service for handling the business logic of restoring portfolio data from
 * backups.
 */
public interface RestoreService {

    /**
     * Restores the portfolio for the currently authenticated user.
     *
     * @param backupData The portfolio data to restore.
     */
    void restoreFromBackup(com.forkmyfolio.model.PortfolioBackup backupData);

    /**
     * Restores the portfolio for a specific target user. (Admin only)
     *
     * @param targetUser The user whose portfolio is to be restored.
     * @param backupData The portfolio data to restore.
     */
    void restoreUserFromBackup(com.forkmyfolio.model.User targetUser, com.forkmyfolio.model.PortfolioBackup backupData);

    /**
     * Restores the entire system from a full backup, including all users and their
     * data. (Admin only)
     *
     * @param systemBackupData A list containing the full backup data for all users.
     */
    void restoreSystemFromBackup(List<com.forkmyfolio.model.UserFullBackup> systemBackupData);

    /**
     * Wipes all user and portfolio data from the database. This is a destructive
     * operation. (Admin only)
     */
    void wipeAllData();
}