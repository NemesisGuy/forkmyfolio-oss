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
     * Restores the entire system from a full backup, including all users and
     * their data, plus the global skills catalogue the backup carries
     * (KNOWN_ISSUES &#167;10). (Admin only)
     *
     * @param systemBackupData A list containing the full backup data for all users.
     * @param catalogSkills    The global skills catalogue carried by the backup;
     *                         {@code null} or empty for legacy backup files that
     *                         predate the catalogue (skills are then recreated on
     *                         demand from user references, as before).
     */
    void restoreSystemFromBackup(List<com.forkmyfolio.model.UserFullBackup> systemBackupData,
                                 List<com.forkmyfolio.model.Skill> catalogSkills);

    /**
     * Wipes all user and portfolio data from the database. This is a destructive
     * operation. (Admin only)
     */
    void wipeAllData();
}