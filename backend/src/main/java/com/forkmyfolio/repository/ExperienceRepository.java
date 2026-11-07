package com.forkmyfolio.repository;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Experience} entity.
 */
@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    /**
     * Finds an experience by its public, unique UUID, eagerly fetching associated skills.
     *
     * @param uuid The UUID of the experience.
     * @return An {@link Optional} containing the found experience, or empty if not found.
     */
    Optional<Experience> findByUuid(UUID uuid);

    /**
     * Finds all experiences associated with a specific user, ordered by the displayOrder field,
     *
     * @param user The user whose experiences to retrieve.
     * @return A list of {@link Experience} entities, sorted by displayOrder in ascending order.
     */
    List<Experience> findByUserOrderByDisplayOrderAsc(User user);

    /**
     * Finds all visible experiences for a specific user.
     *
     * @param user The user whose visible experiences to retrieve.
     * @return A list of visible {@link Experience} entities.
     */
    List<Experience> findByUserAndVisibleTrue(User user);

    /**
     * Deletes all experiences associated with a specific user.
     *
     * @param user The user whose experiences should be deleted.
     */
    @Transactional
    void deleteByUser(User user);

    List<Experience> findByUser(User user);

    long countByUser(User user);

}