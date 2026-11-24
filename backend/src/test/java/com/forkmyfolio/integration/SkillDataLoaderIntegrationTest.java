package com.forkmyfolio.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.config.startup.SkillDataLoader;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 0.30 (TASKS C2/C3): the skill catalogue loader as an UPSERT, proven
 * against a real MySQL database with the real Flyway schema.
 *
 * <p>{@code @PostConstruct} already ran when this context booted, so the
 * database holds the full catalogue. These tests therefore pin the three
 * behaviours that matter on RE-starts (the case every deploy hits):
 * <ol>
 *   <li>the whole JSON catalogue — 583 entries incl. the owner's 12 C3
 *       business/operations skills — is present exactly once (no duplicates
 *       on re-run);</li>
 *   <li>a stale/corrupted row (wrong category, missing description/icon)
 *       is healed from the JSON on the next start;</li>
 *   <li>identity and admin state ({@code uuid}, {@code name},
 *       {@code visible}) survive the heal untouched.</li>
 * </ol>
 */
@Transactional
public class SkillDataLoaderIntegrationTest extends AbstractMySqlIntegrationTest {

    /** The owner's C3 skills, with the category each must land in. */
    private static final String[][] C3_SKILLS = {
            {"Multi-Site Operations Management", "Operations"},
            {"People Management", "Business"},
            {"Staff Recruitment & Selection", "Business"},
            {"Performance Management", "Business"},
            {"Inventory & Stock Control", "Operations"},
            {"Financial & Cash Management", "Business"},
            {"Operational Auditing", "Operations"},
            {"Workforce Planning & Scheduling", "Operations"},
            {"Sales & Target Management", "Business"},
            {"Marketing Campaign Management", "Business"},
            {"Promotional Strategy", "Business"},
            {"Business Operations", "Operations"},
    };

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillDataLoader skillDataLoader;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // 1. Fresh-boot seeding: whole catalogue present exactly once
    // ------------------------------------------------------------------

    @Test
    void catalogue_isFullySeededWithoutDuplicates() throws Exception {
        List<Skill> all = skillRepository.findAll();

        Set<String> jsonNames = new HashSet<>();
        for (Skill skill : parseClasspathCatalogue()) {
            jsonNames.add(skill.getName().toLowerCase(Locale.ROOT));
        }
        assertThat(jsonNames).hasSize(583); // 522 pre-C2 + 61 new (C2 gaps + C3)

        // Every JSON name exists in the DB...
        Set<String> dbNames = new HashSet<>();
        for (Skill skill : all) {
            dbNames.add(skill.getName().toLowerCase(Locale.ROOT));
        }
        assertThat(dbNames).containsAll(jsonNames);

        // ...and the DB has no case-insensitive duplicate names at all.
        assertThat(dbNames).hasSize(all.size());
    }

    @Test
    void c3_ownerSkills_areSeededInBusinessAndOperationsCategories() {
        for (String[] expected : C3_SKILLS) {
            Skill skill = skillRepository.findByNameIgnoreCase(expected[0])
                    .orElseThrow(() -> new AssertionError("C3 skill missing: " + expected[0]));
            assertThat(skill.getCategory()).isEqualTo(expected[1]);
            assertThat(skill.getDescription()).isNotBlank();
            assertThat(skill.getIcon()).startsWith("fa-");
        }
    }

    // ------------------------------------------------------------------
    // 2. Re-run: idempotent (no duplicates) + heals a corrupted row
    // ------------------------------------------------------------------

    @Test
    void rerun_isIdempotent_rowCountDoesNotChange() {
        int before = skillRepository.findAll().size();

        skillDataLoader.loadSkills();

        assertThat(skillRepository.findAll()).hasSize(before);
    }

    @Test
    void rerun_healsStaleRow_butPreservesUuidNameAndVisible() {
        // Shape of the ad-hoc row created before the skill existed in the
        // catalogue: category "Custom", no description, no icon.
        Skill stale = skillRepository.findByNameIgnoreCase("Multi-Site Operations Management")
                .orElseThrow();
        UUID originalUuid = stale.getUuid();
        stale.setCategory("Custom");
        stale.setDescription(null);
        stale.setIcon(null);
        stale.setVisible(false); // admin hid it — must stay hidden
        skillRepository.saveAndFlush(stale);

        skillDataLoader.loadSkills();

        Skill healed = skillRepository.findByNameIgnoreCase("Multi-Site Operations Management")
                .orElseThrow();
        assertThat(healed.getCategory()).isEqualTo("Operations");
        assertThat(healed.getDescription()).isNotBlank();
        assertThat(healed.getIcon()).startsWith("fa-");

        assertThat(healed.getUuid()).isEqualTo(originalUuid);
        assertThat(healed.getName()).isEqualTo("Multi-Site Operations Management");
        assertThat(healed.isVisible()).isFalse();
    }

    @Test
    void rerun_healsWrongCategoryWithoutDuplicating() {
        Skill stale = skillRepository.findByNameIgnoreCase("SEO").orElseThrow();
        stale.setCategory("Methodology"); // the pre-C2 miscategorisation
        skillRepository.saveAndFlush(stale);
        int before = skillRepository.findAll().size();

        skillDataLoader.loadSkills();

        assertThat(skillRepository.findAll()).hasSize(before);
        Skill healed = skillRepository.findByNameIgnoreCase("SEO").orElseThrow();
        assertThat(healed.getCategory()).isEqualTo("Web Development");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<Skill> parseClasspathCatalogue() throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("data/skills/skills.json")) {
            assertThat(in).as("classpath:data/skills/skills.json").isNotNull();
            return objectMapper.readValue(in, new TypeReference<>() {
            });
        }
    }
}
