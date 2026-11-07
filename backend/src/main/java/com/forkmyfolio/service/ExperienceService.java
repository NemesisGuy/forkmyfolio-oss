package com.forkmyfolio.service;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for managing user work experiences.
 */
public interface ExperienceService {

    /**
     * Retrieves all experiences for a specific user.
     *
     * @param user The user whose experiences to retrieve.
     * @return A list of Experience entities.
     */
    List<Experience> getExperiencesForUser(User user);

    /**
     * Finds a single experience by its UUID, ensuring it belongs to the specified user.
     *
     * @param uuid The UUID of the experience.
     * @param user The user for ownership verification.
     * @return The found Experience entity.
     */
    Experience findExperienceByUuidAndUser(UUID uuid, User user);

    /**
     * Creates a new experience and associates it with the given user and skills.
     *
     * @param experienceDetails A transient Experience entity with the new data.
     * @param skillNames        A set of names for the skills to associate.
     * @param owner             The user who will own this experience.
     * @return The persisted Experience entity.
     */
    Experience createExperience(Experience experienceDetails, Set<String> skillNames, User owner);

    /**
     * Updates an existing experience.
     *
     * @param uuid                  The UUID of the experience to update.
     * @param updatedExperienceData A transient entity with the updated data.
     * @param skillNames            The new set of skill names to associate.
     * @param currentUser           The user performing the update, for ownership verification.
     * @return The updated and persisted Experience entity.
     */
    Experience updateExperience(UUID uuid, Experience updatedExperienceData, Set<String> skillNames, User currentUser);

    /**
     * Deletes an experience.
     *
     * @param uuid        The UUID of the experience to delete.
     * @param currentUser The user performing the deletion, for ownership verification.
     */
    void deleteExperience(UUID uuid, User currentUser);
}