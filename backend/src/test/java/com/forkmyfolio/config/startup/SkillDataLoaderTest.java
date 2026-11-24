package com.forkmyfolio.config.startup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 0.30 (TASKS C2): {@link SkillDataLoader} must UPSERT the catalogue.
 *
 * <p>These tests run the loader against the REAL classpath catalogue
 * ({@code data/skills/*.json}) so they fail whenever the JSON and the
 * upsert contract drift apart — not against a hand-written fixture.
 */
@ExtendWith(MockitoExtension.class)
class SkillDataLoaderTest {

    private static final String C3_SKILL = "Multi-Site Operations Management";

    @Mock
    private SkillRepository skillRepository;

    private SkillDataLoader loader;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        loader = new SkillDataLoader(skillRepository, objectMapper);
    }

    // ------------------------------------------------------------------
    // Fresh database: pure insert path
    // ------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void loadSkills_freshDatabase_insertsWholeCatalogueWithUuids() {
        when(skillRepository.findAll()).thenReturn(Collections.emptyList());

        loader.loadSkills();

        ArgumentCaptor<List<Skill>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillRepository).saveAll(captor.capture());
        List<Skill> saved = captor.getValue();

        assertEquals(catalogueSize(), saved.size(), "every JSON entry must be inserted");
        Set<String> names = new HashSet<>();
        for (Skill skill : saved) {
            assertNotNull(skill.getUuid(), "insert must carry a uuid: " + skill.getName());
            assertNotNull(skill.getCategory(), "insert must carry a category: " + skill.getName());
            assertTrue(names.add(skill.getName().toLowerCase(Locale.ROOT)),
                    "duplicate name in catalogue: " + skill.getName());
        }
    }

    // ------------------------------------------------------------------
    // Re-run: idempotency (no writes when the DB already matches the JSON)
    // ------------------------------------------------------------------

    @Test
    void loadSkills_catalogueAlreadyInSync_writesNothing() {
        when(skillRepository.findAll()).thenReturn(parseCatalogueAsExistingRows());

        loader.loadSkills();

        verify(skillRepository, never()).saveAll(anyList());
    }

    // ------------------------------------------------------------------
    // Healing: stale / ad-hoc rows get the catalogue-owned fields refreshed
    // ------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void loadSkills_staleRow_healsCatalogueFieldsButPreservesIdentityAndVisible() {
        Skill stale = new Skill();
        stale.setUuid(UUID.randomUUID());
        stale.setName(C3_SKILL);
        // The pre-C2 ad-hoc shape: created from an inline entry before the
        // skill existed in the catalogue.
        stale.setCategory("Custom");
        stale.setDescription(null);
        stale.setIcon(null);
        stale.setLevel(null);
        // Admin explicitly hid this skill; the loader must not unhide it.
        stale.setVisible(false);
        UUID originalUuid = stale.getUuid();

        when(skillRepository.findAll()).thenReturn(List.of(stale));

        loader.loadSkills();

        ArgumentCaptor<List<Skill>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillRepository, atLeastOnce()).saveAll(captor.capture());
        List<Skill> healed = captor.getValue();

        assertEquals(1, healed.size(), "only the stale row is written");
        Skill row = healed.get(0);
        assertEquals("Operations", row.getCategory(), "wrong category must be healed");
        assertNotNull(row.getDescription(), "missing description must be healed");
        assertNotNull(row.getIcon(), "missing icon must be healed");
        assertNotNull(row.getLevel(), "missing level must be healed");

        assertEquals(originalUuid, row.getUuid(), "identity must never change");
        assertEquals(C3_SKILL, row.getName(), "name must never change");
        assertFalse(row.isVisible(), "admin visible override must be preserved");
    }

    @Test
    @SuppressWarnings("unchecked")
    void loadSkills_caseInsensitiveNameMatch_doesNotDuplicateTheRow() {
        Skill lowerCaseTwin = new Skill();
        lowerCaseTwin.setUuid(UUID.randomUUID());
        lowerCaseTwin.setName(C3_SKILL.toLowerCase(Locale.ROOT));
        // Stale category forces an in-place refresh of THIS row.
        lowerCaseTwin.setCategory("Custom");
        lowerCaseTwin.setDescription("Existing description that must not be blanked.");
        lowerCaseTwin.setIcon("fa-solid fa-building");
        lowerCaseTwin.setLevel(SkillLevel.BEGINNER);

        when(skillRepository.findAll()).thenReturn(List.of(lowerCaseTwin));

        loader.loadSkills();

        // The whole catalogue (minus the matched name) is inserted, and the
        // matched name is refreshed in place — never re-inserted under the
        // JSON's casing.
        ArgumentCaptor<List<Skill>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillRepository, atLeastOnce()).saveAll(captor.capture());
        List<Skill> allSaved = new ArrayList<>();
        captor.getAllValues().forEach(allSaved::addAll);

        List<Skill> twins = allSaved.stream()
                .filter(s -> C3_SKILL.equalsIgnoreCase(s.getName()))
                .toList();
        assertEquals(1, twins.size(), "the existing name must be saved exactly once");
        assertSame(lowerCaseTwin, twins.get(0), "the EXISTING row is updated, not a new one");
        assertEquals("Operations", lowerCaseTwin.getCategory(), "stale category healed in place");
        assertEquals(C3_SKILL.toLowerCase(Locale.ROOT), lowerCaseTwin.getName(),
                "name must never be rewritten to the JSON casing");
        // description IS catalogue-owned: the JSON value wins over the stale one.
        assertNotNull(lowerCaseTwin.getDescription(), "description refreshed from JSON");
        assertFalse(lowerCaseTwin.getDescription().isBlank());
    }

    // ------------------------------------------------------------------
    // applyIfPresent: sparse JSON must never blank stored data
    // ------------------------------------------------------------------

    @Test
    void applyIfPresent_nullIncoming_keepsCurrent() {
        String[] sink = {"kept"};
        assertFalse(loader.applyIfPresent(null, sink[0], v -> sink[0] = v));
        assertEquals("kept", sink[0]);
    }

    @Test
    void applyIfPresent_blankIncoming_keepsCurrent() {
        String[] sink = {"kept"};
        assertFalse(loader.applyIfPresent("   ", sink[0], v -> sink[0] = v));
        assertEquals("kept", sink[0]);
    }

    @Test
    void applyIfPresent_identicalIncoming_isNotAChange() {
        assertFalse(loader.applyIfPresent("Operations", "Operations", v -> {
            throw new AssertionError("identical value must not be written");
        }));
    }

    @Test
    void applyIfPresent_differentIncoming_writesAndReportsChange() {
        String[] sink = {"Custom"};
        assertTrue(loader.applyIfPresent("Operations", sink[0], v -> sink[0] = v));
        assertEquals("Operations", sink[0]);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Number of entries in the real classpath catalogue. */
    private int catalogueSize() {
        return parseCatalogueAsExistingRows().size();
    }

    /**
     * Parses the real {@code data/skills/*.json} into rows shaped like a DB
     * that is already perfectly in sync (uuid assigned, all fields equal to
     * the JSON).
     */
    private List<Skill> parseCatalogueAsExistingRows() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:data/skills/*.json");
            List<Skill> rows = new ArrayList<>();
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    List<Skill> parsed = objectMapper.readValue(in, new TypeReference<>() {
                    });
                    for (Skill skill : parsed) {
                        skill.setUuid(UUID.randomUUID());
                        rows.add(skill);
                    }
                }
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalStateException("classpath catalogue unreadable", e);
        }
    }
}
