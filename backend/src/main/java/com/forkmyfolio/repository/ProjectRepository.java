package com.forkmyfolio.repository;

import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Project} entity.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Finds a project by its public, unique UUID.
     *
     * @param uuid The UUID of the project.
     * @return An {@link Optional} containing the found project, or empty if not found.
     */
    Optional<Project> findByUuid(UUID uuid);

    /**
     * Finds all projects associated with a specific user.
     *
     * @param user The user whose projects to retrieve.
     * @return A list of {@link Project} entities.
     */
    List<Project> findByUser(User user);

    /**
     * Finds all projects associated with a specific user, ordered by the displayOrder field.
     *
     * @param user The user whose projects to retrieve.
     * @return A sorted list of {@link Project} entities.
     */
    @EntityGraph(value = "Project.withSkills")
    List<Project> findByUserOrderByDisplayOrderAsc(User user);

    /**
     * Finds all visible projects for a specific user.
     *
     * @param user The user whose visible projects to retrieve.
     * @return A list of visible {@link Project} entities.
     */
    List<Project> findByUserAndVisibleTrue(User user);

    /**
     * Deletes all projects associated with a specific user.
     *
     * @param user The user whose projects should be deleted.
     */
    @Transactional
    void deleteByUser(User user);

    long countByUser(User user);

}