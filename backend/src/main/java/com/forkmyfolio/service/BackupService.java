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

    /**
     * Returns the complete global skills catalogue for embedding in a system
     * backup (KNOWN_ISSUES &#167;10). Without it a restore into an empty
     * database only rebuilds the skills user data happens to reference.
     *
     * @return All catalogue skills in the system.
     */
    List<com.forkmyfolio.model.Skill> getSkillCatalog();

}