package com.forkmyfolio.repository;

import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link JobApplication} entity.
 */
@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /**
     * Finds a job application by its public, unique UUID.
     *
     * @param uuid The UUID of the job application.
     * @return An {@link Optional} containing the found application, or empty if not
     *         found.
     */
    Optional<JobApplication> findByUuid(UUID uuid);

    /**
     * Finds all job applications associated with a specific user.
     *
     * @param owner The user who owns the applications.
     * @return A list of {@link JobApplication} entities.
     */
    List<JobApplication> findByOwner(User owner);

    /**
     * Finds all job applications associated with a specific user, ordered by status
     * update time.
     *
     * @param owner The user who owns the applications.
     * @return A sorted list of {@link JobApplication} entities.
     */
    List<JobApplication> findByOwnerOrderByStatusUpdatedAtDesc(User owner);

    /**
     * Deletes all job applications associated with a specific user.
     *
     * @param owner The user whose applications should be deleted.
     */
    @Transactional
    void deleteByOwner(User owner);

    /**
     * Counts the number of job applications for a specific user.
     *
     * @param owner The user.
     * @return The number of applications.
     */
    long countByOwner(User owner);
}
