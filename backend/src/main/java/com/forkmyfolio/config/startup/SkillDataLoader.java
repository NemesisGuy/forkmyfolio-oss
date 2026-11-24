package com.forkmyfolio.config.startup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.repository.SkillRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Responsible for seeding the global 'skills' table from all JSON files
 * found in the 'data/skills/' directory at application startup.
 * This is the single source of truth for initial skill data.
 *
 * <p>Phase 0.30 (TASKS C2): the loader is an UPSERT, not insert-only.
 * Names that are not yet in the database are inserted; names that already
 * exist get their catalogue-owned fields ({@code category},
 * {@code description}, {@code icon}, {@code level}) refreshed from the JSON,
 * so catalogue fixes (wrong category, missing description, dead icon) heal
 * existing rows — including ad-hoc "Custom" rows created before a skill
 * existed in the catalogue. Identity ({@code name}, {@code uuid}), the
 * admin {@code visible} override, and all user-owned data (per-user
 * {@code UserSkill} level/description/visible, project/experience links)
 * are never touched.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkillDataLoader {

    private static final String SKILLS_PATTERN = "classpath:data/skills/*.json";
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    @Transactional
    public void loadSkills() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SKILLS_PATTERN);

            if (resources.length == 0) {
                log.warn("Could not find any skill files matching pattern '{}'. No global skills will be seeded.",
                        SKILLS_PATTERN);
                return;
            }

            log.info("Found {} skill files to check for updates in '{}'.", resources.length, SKILLS_PATTERN);

            List<Skill> allSkillsFromJson = new ArrayList<>();
            for (Resource resource : resources) {
                log.info("Reading skills from: {}", resource.getFilename());
                try (InputStream inputStream = resource.getInputStream()) {
                    List<Skill> skillsFromFile = objectMapper.readValue(inputStream, new TypeReference<>() {
                    });
                    allSkillsFromJson.addAll(skillsFromFile);
                } catch (IOException e) {
                    log.error("Failed to read or parse skill file: {}", resource.getFilename(), e);
                }
            }

            if (allSkillsFromJson.isEmpty()) {
                log.warn("No skills were loaded from the JSON files.");
                return;
            }

            // De-duplicate skills by name (case-insensitive) across all files.
            Map<String, Skill> incomingSkillsMap = new LinkedHashMap<>();
            allSkillsFromJson.forEach(skill -> {
                if (skill != null && skill.getName() != null && !skill.getName().isBlank()) {
                    incomingSkillsMap.put(skill.getName().toLowerCase(), skill);
                }
            });

            // Phase 0.30 (TASKS C2): upsert keyed by lower-cased name.
            // - Missing name  -> insert (uuid assigned by the entity hook on save).
            // - Existing name -> refresh ONLY the catalogue-owned fields so that
            //   corrected categories/descriptions/icons heal old rows. Name,
            //   uuid, visible (admin override) and created_at stay untouched.
            Map<String, Skill> existingByName = new HashMap<>();
            for (Skill existing : skillRepository.findAll()) {
                if (existing != null && existing.getName() != null) {
                    existingByName.put(existing.getName().toLowerCase(), existing);
                }
            }

            int inserted = 0;
            int updated = 0;
            List<Skill> toInsert = new ArrayList<>();
            List<Skill> toUpdate = new ArrayList<>();
            for (Map.Entry<String, Skill> entry : incomingSkillsMap.entrySet()) {
                Skill incoming = entry.getValue();
                Skill existing = existingByName.get(entry.getKey());
                if (existing == null) {
                    if (incoming.getUuid() == null) {
                        incoming.setUuid(UUID.randomUUID());
                    }
                    toInsert.add(incoming);
                    continue;
                }

                boolean changed = false;
                changed |= applyIfPresent(incoming.getCategory(), existing.getCategory(),
                        existing::setCategory);
                changed |= applyIfPresent(incoming.getDescription(), existing.getDescription(),
                        existing::setDescription);
                changed |= applyIfPresent(incoming.getIcon(), existing.getIcon(),
                        existing::setIcon);
                changed |= applyIfPresent(incoming.getLevel(), existing.getLevel(),
                        existing::setLevel);
                if (changed) {
                    toUpdate.add(existing);
                }
            }

            if (!toInsert.isEmpty()) {
                skillRepository.saveAll(toInsert);
                inserted = toInsert.size();
            }
            if (!toUpdate.isEmpty()) {
                skillRepository.saveAll(toUpdate);
                updated = toUpdate.size();
            }

            log.info("Skill catalogue sync: {} inserted, {} refreshed, {} unchanged ({} in JSON).",
                    inserted, updated, incomingSkillsMap.size() - inserted - updated,
                    incomingSkillsMap.size());

        } catch (Exception e) {
            log.error("An unexpected error occurred during the skill loading process.", e);
        }
    }

    /**
     * Copies {@code incoming} onto the existing value only when the catalogue
     * carries a real value that differs. {@code null} and blank strings never
     * overwrite stored data (a sparse JSON file must not blank the DB).
     *
     * @return true if the existing value changed
     */
    <T> boolean applyIfPresent(T incoming, T current, java.util.function.Consumer<T> setter) {
        if (incoming == null) {
            return false;
        }
        if (incoming instanceof String s && s.isBlank()) {
            return false;
        }
        if (incoming.equals(current)) {
            return false;
        }
        setter.accept(incoming);
        return true;
    }
}