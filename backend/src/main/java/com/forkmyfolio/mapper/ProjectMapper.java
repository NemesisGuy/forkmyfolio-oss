package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.create.CreateProjectRequest;
import com.forkmyfolio.dto.response.ProjectDto;
import com.forkmyfolio.dto.update.UpdateProjectRequest;
import com.forkmyfolio.model.KeyFeature;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting between Project domain models and
 * Project-related DTOs.
 * This keeps the conversion logic separate from the service and controller
 * layers.
 */
@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final SkillMapper skillMapper;

    /**
     * Phase 7.4 (§6): rebuilds the owned key-features collection from a plain
     * ordered string list (create/update/backup-restore). display order comes
     * from the list index; per-row uuids are generated on persist. Returns
     * {@code null} when the source list is null so callers can keep "don't
     * touch" semantics.
     */
    public static List<KeyFeature> toKeyFeatureEntities(Project owner, List<String> values) {
        if (values == null) {
            return null;
        }
        List<KeyFeature> features = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            KeyFeature feature = new KeyFeature();
            feature.setProject(owner);
            feature.setValue(values.get(i));
            feature.setDisplayOrder(i);
            features.add(feature);
        }
        return features;
    }

    /**
     * Replaces the given project's key-features with the supplied ordered
     * values (orphan-removal semantics make this a full reorder/delete).
     * Handling null as "leave unchanged" keeps patch-style callers safe.
     */
    public static void applyKeyFeatures(Project project, List<String> values) {
        List<KeyFeature> rebuilt = toKeyFeatureEntities(project, values);
        if (rebuilt == null) {
            return;
        }
        project.getKeyFeatures().clear();
        project.getKeyFeatures().addAll(rebuilt);
    }

    /**
     * Converts a Project entity to a DTO, enriching it with user-specific skill
     * data.
     * This is the correct way to build a complete Project DTO for the API.
     *
     * @param project         The Project entity to convert.
     * @param userSkillLookup A map of the user's skills to provide full context.
     * @return The corresponding ProjectDto.
     */
    public ProjectDto toDto(Project project, Map<UUID, UserSkill> userSkillLookup) {
        if (project == null) {
            return null;
        }
        ProjectDto dto = new ProjectDto();
        dto.setUuid(project.getUuid());
        dto.setTitle(project.getTitle());
        dto.setDescription(project.getDescription());
        dto.setRepoUrl(project.getRepoUrl());
        dto.setLiveUrl(project.getLiveUrl());
        dto.setImageUrl(project.getImageUrl());
        dto.setMediaUrl(project.getMediaUrl());
        dto.setVisible(project.isVisible());
        dto.setDisplayOrder(project.getDisplayOrder());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        // Phase 7.4 (§6): ordered key-features bullets (empty list stays [])
        dto.setKeyFeatures(project.getKeyFeatures() == null
                ? Collections.emptyList()
                : project.getKeyFeatures().stream().map(KeyFeature::getValue).collect(Collectors.toList()));

        if (project.getSkills() != null) {
            // FIX: Use the lookup map to call the correct skill mapper.
            // This ensures all user-specific data (ID, level, description) is included.
            dto.setSkills(project.getSkills().stream()
                    .map(skill -> {
                        UserSkill userSkill = userSkillLookup.get(skill.getUuid());
                        // If the user has this skill rated, use the detailed mapper. Otherwise, fall
                        // back to the basic one.
                        return (userSkill != null) ? skillMapper.toDetailDto(userSkill) : skillMapper.toBasicDto(skill);
                    })
                    .collect(Collectors.toSet()));
        } else {
            dto.setSkills(Collections.emptySet());
        }

        return dto;
    }

    /**
     * Converts a CreateProjectRequest DTO to a new, transient Project entity.
     * The User is not set here; the controller is responsible for associating the
     * current user.
     *
     * @param request The DTO containing the creation data.
     * @return A new Project entity, ready to be configured and persisted.
     */
    public Project toEntity(CreateProjectRequest request) {
        if (request == null) {
            return null;
        }
        Project project = new Project();
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setRepoUrl(request.getRepoUrl());
        project.setLiveUrl(request.getLiveUrl());
        project.setImageUrl(request.getImageUrl());
        project.setMediaUrl(request.getMediaUrl());
        project.setVisible(request.isVisible());
        project.setDisplayOrder(request.getDisplayOrder());
        // Phase 7.4 (§6): keyFeatures are applied in the service from the raw
        // List<String> (controller passes the list explicitly) so that a null
        // request field stays distinguishable from an empty list.
        return project;
    }

    /**
     * Converts a ProjectDto (typically from a backup) to a new Project entity.
     *
     * @param dto  The DTO containing the project data.
     * @param user The user who will own this project.
     * @return A new Project entity, ready to be persisted.
     */
    public Project toEntityFromDto(ProjectDto dto, User user) {
        if (dto == null) {
            return null;
        }
        Project project = new Project();
        // FIX: Preserve the original UUID from the backup DTO. This is crucial for
        // restores.
        project.setUuid(dto.getUuid());
        project.setUser(user);
        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setRepoUrl(dto.getRepoUrl());
        project.setLiveUrl(dto.getLiveUrl());
        project.setImageUrl(dto.getImageUrl());
        project.setMediaUrl(dto.getMediaUrl());
        project.setVisible(dto.isVisible());
        project.setDisplayOrder(dto.getDisplayOrder());
        // Phase 7.4 (§6): backup restore — list order IS the display order;
        // per-row uuids are regenerated (backup DTOs carry project uuid only)
        applyKeyFeatures(project, dto.getKeyFeatures());

        if (dto.getSkills() != null) {
            project.setSkills(dto.getSkills().stream()
                    .map(sDto -> {
                        Skill skill = new Skill();
                        skill.setUuid(sDto.getSkillId());
                        skill.setName(sDto.getName());
                        skill.setCategory(sDto.getCategory());
                        skill.setIcon(sDto.getIcon());
                        return skill;
                    })
                    .collect(Collectors.toSet()));
        }

        return project;
    }

    /**
     * Creates a transient Project entity from an UpdateProjectRequest DTO.
     *
     * @param request The DTO containing the update data.
     * @return A transient Project entity populated with data from the request.
     */
    public Project toEntity(UpdateProjectRequest request) {
        if (request == null) {
            return null;
        }
        Project project = new Project();
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setRepoUrl(request.getRepoUrl());
        project.setLiveUrl(request.getLiveUrl());
        project.setImageUrl(request.getImageUrl());
        project.setMediaUrl(request.getMediaUrl());
        project.setVisible(request.getVisible());
        project.setDisplayOrder(request.getDisplayOrder());
        // Phase 7.4 (§6): keyFeatures are applied in the service from the raw
        // List<String> (controller passes the list explicitly) so that a null
        // request field stays distinguishable from an empty list.
        return project;
    }
}