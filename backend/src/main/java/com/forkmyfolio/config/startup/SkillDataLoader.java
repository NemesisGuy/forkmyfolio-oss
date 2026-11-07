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

            // Get existing skills from DB to avoid duplicates
            List<Skill> existingSkills = skillRepository.findAll();
            Set<String> existingNames = new HashSet<>();
            existingSkills.forEach(s -> existingNames.add(s.getName().toLowerCase()));

            List<Skill> skillsToSave = new ArrayList<>();
            for (Map.Entry<String, Skill> entry : incomingSkillsMap.entrySet()) {
                if (!existingNames.contains(entry.getKey())) {
                    Skill newSkill = entry.getValue();
                    if (newSkill.getUuid() == null) {
                        newSkill.setUuid(UUID.randomUUID());
                    }
                    skillsToSave.add(newSkill);
                }
            }

            if (!skillsToSave.isEmpty()) {
                skillRepository.saveAll(skillsToSave);
                log.info("Successfully added {} new global skills to the database.", skillsToSave.size());
            } else {
                log.info("No new global skills found to update.");
            }

        } catch (Exception e) {
            log.error("An unexpected error occurred during the skill loading process.", e);
        }
    }
}