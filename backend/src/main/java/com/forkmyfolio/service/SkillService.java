package com.forkmyfolio.service;

import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing skills and their relationships with users.
 */
public interface SkillService {

    /**
     * Retrieves all skills associated with a specific user.
     *
     * @param user The user whose skills to retrieve.
     * @return A list of {@link UserSkill} entities.
     */
    List<UserSkill> getAllSkillsForUser(User user);

    /**
     * Retrieves a specific UserSkill relationship by its UUID, ensuring it belongs to the user.
     *
     * @param userSkillUuid The UUID of the UserSkill relationship.
     * @param user          The user who must own the skill relationship.
     * @return The found {@link UserSkill} entity.
     */
    UserSkill getSkillForUser(User user, UUID userSkillUuid);

    /**
     * Adds a new skill to a user's portfolio by finding or creating a global skill
     * and then creating the specific relationship to the user.
     *
     * @param user             The user to whom the skill will be added.
     * @param skillDetails     A transient {@link Skill} entity containing the details (name, category, icon) of the global skill.
     * @param userSkillDetails A transient {@link UserSkill} entity containing the user-specific details (level, visibility, description).
     * @return The newly created {@link UserSkill} relationship entity.
     */
    UserSkill addSkillToUser(User user, Skill skillDetails, UserSkill userSkillDetails);

    /**
     * Updates a user's relationship with a skill (e.g., level, visibility).
     *
     * @param userSkillUuid    The UUID of the UserSkill relationship to update.
     * @param userSkillUpdates A transient {@link UserSkill} entity containing the updated information.
     * @param user             The user performing the update.
     * @return The updated {@link UserSkill} entity.
     */
    UserSkill updateSkillForUser(UUID userSkillUuid, UserSkill userSkillUpdates, User user);

    /**
     * Removes a skill from a user's portfolio.
     *
     * @param userSkillUuid The UUID of the UserSkill relationship to remove.
     * @param user          The user performing the removal.
     */
    void removeSkillFromUser(UUID userSkillUuid, User user);

    /**
     * Finds a set of skills by their names, creating any that do not already exist.
     *
     * @param skillNames A set of skill names.
     * @return A set of {@link Skill} entities.
     */
    Set<Skill> findOrCreateSkills(Set<String> skillNames);

    List<Skill> getAllPlatformSkills();

    Skill findOrCreateSkill(Skill skillDetails);
}