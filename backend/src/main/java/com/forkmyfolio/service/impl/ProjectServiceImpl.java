package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.ProjectRepository;
import com.forkmyfolio.repository.SkillRepository;
import com.forkmyfolio.service.ProjectService;
import com.forkmyfolio.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectsForUser(User user) {
        // The service layer's responsibility is to fetch the domain entities.
        List<Project> projects = projectRepository.findByUserOrderByDisplayOrderAsc(user);
        // We initialize the skills collection to prevent LazyInitializationException
        // later in the mapping process.
        projects.forEach(project -> {
            Hibernate.initialize(project.getSkills());
            Hibernate.initialize(project.getKeyFeatures()); // Phase 7.4 (§6)
        });
        return projects;
    }

    @Override
    @Transactional(readOnly = true)
    public Project findProjectByUuidAndUser(UUID uuid, User user) {
        // Phase 7.4 (403/404 policy): cross-tenant access returns 404
        // (ResourceNotFound) - identical to the uuid-miss branch above - so a
        // foreign uuid can never be distinguished from a nonexistent one.
        // Existence of another user's resources must not be disclosed through
        // probing.
        Project project = projectRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Project with UUID " + uuid + " not found."));
        if (!project.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Project with UUID " + uuid + " not found.");
        }
        Hibernate.initialize(project.getSkills()); // Initialize skills to prevent LazyInitializationException in the
                                                   // mapper.
        Hibernate.initialize(project.getKeyFeatures()); // Phase 7.4 (§6)
        return project;
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#owner.slug")
    public Project createProject(Project project, List<String> keyFeatures, User owner, Set<String> skillNames) {
        // The owner is assigned inside the service so the cache key above is
        // evaluated with the correct owner before the entity is persisted.
        project.setUser(owner);
        // Phase 7.4 (§6): keyFeatures cascade-persist with the project.
        ProjectMapper.applyKeyFeatures(project, keyFeatures);
        Set<Skill> skills = findAndValidateSkills(skillNames);
        project.setSkills(skills);
        Project savedProject = projectRepository.save(project);
        Hibernate.initialize(savedProject.getSkills()); // Initialize for the response DTO
        Hibernate.initialize(savedProject.getKeyFeatures());
        return savedProject;
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public Project updateProject(UUID uuid, Project projectWithUpdates, List<String> keyFeatures, Set<String> skillNames, User user) {
        Project existingProject = findProjectByUuidAndUser(uuid, user); // This also handles permission check

        existingProject.setTitle(projectWithUpdates.getTitle());
        existingProject.setDescription(projectWithUpdates.getDescription());
        existingProject.setRepoUrl(projectWithUpdates.getRepoUrl());
        existingProject.setLiveUrl(projectWithUpdates.getLiveUrl());
        existingProject.setImageUrl(projectWithUpdates.getImageUrl());
        existingProject.setVisible(projectWithUpdates.isVisible());
        existingProject.setDisplayOrder(projectWithUpdates.getDisplayOrder());

        Set<Skill> skills = findAndValidateSkills(skillNames);
        existingProject.setSkills(skills);

        // Phase 7.4 (§6): list replace = full add/edit/remove/reorder in one
        // step (orphanRemoval drops bullets the caller deleted). null stays
        // "leave unchanged" for patch-style callers.
        ProjectMapper.applyKeyFeatures(existingProject, keyFeatures);

        Project updatedProject = projectRepository.save(existingProject);
        Hibernate.initialize(updatedProject.getSkills()); // Initialize for the response DTO
        Hibernate.initialize(updatedProject.getKeyFeatures());
        return updatedProject;
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#user.slug")
    public void deleteProject(UUID uuid, User user) {
        Project project = findProjectByUuidAndUser(uuid, user); // Ensures user owns the project before deleting
        projectRepository.delete(project);
    }

    private Set<Skill> findAndValidateSkills(Set<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return new HashSet<>();
        }
        // This assumes your SkillRepository has a 'findByNameIn' method.
        return new HashSet<>(skillRepository.findByNameIn(skillNames));
    }
}
