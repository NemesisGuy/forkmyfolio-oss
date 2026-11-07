package com.forkmyfolio.service;

import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for managing a user's portfolio projects.
 * This service operates exclusively on domain entities.
 */
public interface ProjectService {

    /**
     * Retrieves all projects for a specific user, ordered by displayOrder.
     */
    List<Project> getProjectsForUser(User user);

    /**
     * Finds a single project by its UUID, ensuring it belongs to the specified user.
     */
    Project findProjectByUuidAndUser(UUID uuid, User user);

    /**
     * Creates a new project for a user and associates it with the given skills.
     * The owner is passed explicitly so the controller never mutates the entity.
     * Key features are passed as the raw ordered string list (list order =
     * display order) — keeping DTOs at the controller boundary (NAG-01).
     */
    Project createProject(Project project, List<String> keyFeatures, User owner, Set<String> skillNames);

    /**
     * Updates an existing project. {@code keyFeatures} is the raw ordered
     * string list from the request: non-null replaces the stored list in the
     * given order (add/edit/remove/reorder); null leaves the stored list
     * untouched.
     */
    Project updateProject(UUID uuid, Project projectWithUpdates, List<String> keyFeatures, Set<String> skillNames, User user);

    /**
     * Deletes a project, verifying ownership first.
     */
    void deleteProject(UUID uuid, User user);
}