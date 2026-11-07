package com.forkmyfolio.repository;

import com.forkmyfolio.model.Skill;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for the global Skill entity.
 * This repository handles operations on the master list of all skills in the system.
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Finds a skill by its unique UUID.
     *
     * @param uuid The UUID of the skill.
     * @return An Optional containing the skill if found.
     */
    Optional<Skill> findByUuid(UUID uuid);

    /**
     * Finds a skill by its unique name (case-insensitive).
     *
     * @param name The name of the skill.
     * @return An Optional containing the skill if found.
     */
    Optional<Skill> findByNameIgnoreCase(String name);

    /**
     * Finds a set of skills from a given collection of names.
     * This is useful for batch lookups.
     *
     * @param names A set of skill names to find.
     * @return A set of matching Skill entities.
     */
    Set<Skill> findByNameIn(Set<String> names);

    /**
     * FIX: Add the missing method to find all skills by a set of UUIDs.
     * This is a highly efficient bulk operation used by the restore service.
     * Spring Data JPA will automatically generate the implementation.
     *
     * @param uuids The set of skill UUIDs to find.
     * @return A set of matching Skill entities.
     */
    Set<Skill> findAllByUuidIn(Set<UUID> uuids);

    boolean existsByName(String name);

    Optional<Skill> findByName(@NotBlank(message = "Skill name cannot be blank.") String name);
}