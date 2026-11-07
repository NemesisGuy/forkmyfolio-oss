package com.forkmyfolio.service;

import com.forkmyfolio.model.User;

import java.util.List;

/**
 * Service for handling backup-related business logic, such as creating backup
 * data structures.
 */
public interface BackupService {

    com.forkmyfolio.model.PortfolioBackup createBackupForUser(User user);

    com.forkmyfolio.model.PortfolioBackup createBackupForUser(java.util.UUID userUuid);

    /**
     * Creates a full system backup containing all users and their portfolio data.
     * This method is designed to be transactional to support lazy loading of
     * collections.
     *
     * @return A list of UserFullBackup objects representing the entire system
     *         state.
     */
    List<com.forkmyfolio.model.UserFullBackup> createFullSystemBackup();

}