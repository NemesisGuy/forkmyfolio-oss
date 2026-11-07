package com.forkmyfolio.repository;

import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link JobApplicationNote} entity.
 */
@Repository
public interface JobApplicationNoteRepository extends JpaRepository<JobApplicationNote, Long> {

    /**
     * Finds a job application note by its public, unique UUID.
     *
     * @param uuid The UUID of the job application note.
     * @return An {@link Optional} containing the found note, or empty if not found.
     */
    Optional<JobApplicationNote> findByUuid(UUID uuid);

    /**
     * Finds all notes associated with a specific job application.
     *
     * @param jobApplication The job application.
     * @return A list of {@link JobApplicationNote} entities.
     */
    List<JobApplicationNote> findByJobApplicationOrderByCreatedAtDesc(JobApplication jobApplication);
}
