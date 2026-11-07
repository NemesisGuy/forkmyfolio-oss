package com.forkmyfolio.repository;

import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Qualification} entity.
 */
@Repository
public interface QualificationRepository extends JpaRepository<Qualification, Long> {

    /**
     * Finds a qualification by its unique UUID.
     *
     * @param uuid The UUID of the qualification.
     * @return An {@link Optional} containing the found qualification, or empty if not found.
     */
    Optional<Qualification> findByUuid(UUID uuid);

    /**
     * Finds all qualifications belonging to a specific user.
     *
     * @param user The user whose qualifications to retrieve.
     * @return A list of qualifications for the given user.
     */
    List<Qualification> findByUser(User user);

    /**
     * Finds a specific qualification by its UUID, ensuring it belongs to the specified user.
     * This is a crucial method for security to prevent users from accessing or modifying
     * qualifications that do not belong to them.
     *
     * @param uuid The UUID of the qualification.
     * @param user The user who must own the qualification.
     * @return An {@link Optional} containing the found qualification, or empty if not found or not owned by the user.
     */
    Optional<Qualification> findByUuidAndUser(UUID uuid, User user);

    /**
     * Finds all visible qualifications for a specific user.
     *
     * @param user The user whose visible qualifications to retrieve.
     * @return A list of visible {@link Qualification} entities.
     */
    List<Qualification> findByUserAndVisibleTrue(User user);

    /**
     * Deletes all qualifications associated with a specific user.
     *
     * @param user The user whose qualifications should be deleted.
     */
    @Transactional
    void deleteByUser(User user);

    List<Qualification> findByUserOrderByCompletionYearDescStartYearDesc(User user);

    long countByUser(User user);

}