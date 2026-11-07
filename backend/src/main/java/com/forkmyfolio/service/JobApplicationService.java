package com.forkmyfolio.service;

import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import com.forkmyfolio.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing job applications and their lifecycle.
 * Adheres to the DTO Boundary Rule (DBR) by operating solely on domain
 * entities.
 */
public interface JobApplicationService {

    /**
     * Retrieves all job applications for a specific user.
     *
     * @param owner The user who owns the applications.
     * @return A list of {@link JobApplication} entities.
     */
    List<JobApplication> getAllApplicationsForUser(User owner);

    /**
     * Retrieves a specific job application by its unique UUID for a given user.
     *
     * @param uuid  The UUID of the application.
     * @param owner The user who must own the application.
     * @return The found {@link JobApplication}.
     * @throws com.forkmyfolio.exception.ResourceNotFoundException       if not
     *                                                                   found.
     * @throws org.springframework.security.access.AccessDeniedException if the user
     *                                                                   doesn't own
     *                                                                   it.
     */
    JobApplication getApplicationByUuid(UUID uuid, User owner);

    /**
     * Creates a new job application for the specified user.
     *
     * @param application The transient application entity with populated details.
     * @param initialNote An optional initial note to attach to the application.
     * @param owner       The user creating the application.
     * @return The persisted {@link JobApplication}.
     */
    JobApplication createApplication(JobApplication application, String initialNote, User owner);

    /**
     * Updates an existing job application.
     *
     * @param uuid    The UUID of the application to update.
     * @param updates A transient entity containing the fields to update.
     * @param visible The requested visibility, or {@code null} when the
     *                caller omitted the field (the stored value is then
     *                preserved — Phase 8.4 visibility-preservation fix).
     * @param owner   The user performing the update.
     * @return The updated and persisted {@link JobApplication}.
     */
    JobApplication updateApplication(UUID uuid, JobApplication updates, Boolean visible, User owner);

    /**
     * Deletes a job application and all its associated notes.
     *
     * @param uuid  The UUID of the application to delete.
     * @param owner The user performing the deletion.
     */
    void deleteApplication(UUID uuid, User owner);

    /**
     * Adds a note to an existing job application.
     *
     * @param applicationUuid The UUID of the application.
     * @param content         The content of the note.
     * @param owner           The user who owns the application.
     * @return The created {@link JobApplicationNote}.
     */
    JobApplicationNote addNote(UUID applicationUuid, String content, User owner);

    /**
     * Deletes a specific note from an application.
     *
     * @param applicationUuid The UUID of the application.
     * @param noteUuid        The UUID of the note.
     * @param owner           The user who owns the application.
     */
    void deleteNote(UUID applicationUuid, UUID noteUuid, User owner);
}
