package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.Skill;
import com.forkmyfolio.repository.SkillRepository;
import com.forkmyfolio.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Phase 0.28 (C1): skills created from a job experience must never be
 * stored name-only.
 *
 * Before this phase {@code findOrCreateSkills} persisted a new global skill
 * with a NULL icon and NULL category, so every badge that rendered it showed
 * an empty {@code <i>} (no icon at all) and the skill had no category. The
 * contract now is:
 * - an inline (invented) name is stored with a default category + icon;
 * - a name that already exists in the catalogue keeps its own metadata and
 *   is matched case-insensitively (no duplicate "java" / "Java" rows);
 * - blank/null names are dropped instead of creating junk rows.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkillServiceImplTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private SkillServiceImpl skillService;

    @BeforeEach
    void setUp() {
        // save() echoes the entity back AND assigns a uuid, mirroring the
        // entity's @PrePersist hook. Skill's equals/hashCode is uuid-based, so
        // without this two transient skills would collapse into one set entry.
        when(skillRepository.save(any(Skill.class)))
                .thenAnswer(invocation -> {
                    Skill skill = invocation.getArgument(0);
                    if (skill.getUuid() == null) {
                        skill.setUuid(UUID.randomUUID());
                    }
                    return skill;
                });
    }

    private static Skill skill(String name, String category, String icon) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory(category);
        skill.setIcon(icon);
        return skill;
    }

    @Test
    void findOrCreateSkills_storesInlineSkillWithCategoryAndIcon() {
        when(skillRepository.findByNameIgnoreCase("Multi-Site Operations Management"))
                .thenReturn(Optional.empty());

        Set<Skill> result = skillService.findOrCreateSkills(
                Set.of("Multi-Site Operations Management"));

        assertEquals(1, result.size());
        Skill created = result.iterator().next();
        assertEquals("Multi-Site Operations Management", created.getName());
        assertEquals(SkillServiceImpl.DEFAULT_INLINE_CATEGORY, created.getCategory());
        assertEquals(SkillServiceImpl.DEFAULT_INLINE_ICON, created.getIcon());

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository, times(1)).save(captor.capture());
        assertNotNull(captor.getValue().getIcon(), "inline skills must never be saved without an icon");
        assertNotNull(captor.getValue().getCategory(), "inline skills must never be saved without a category");
    }

    @Test
    void findOrCreateSkills_reusesCatalogueSkillAndKeepsItsMetadata() {
        Skill catalogueJava = skill("Java", "Programming Language", "fa-brands fa-java");
        when(skillRepository.findByNameIgnoreCase("java")).thenReturn(Optional.of(catalogueJava));

        Set<Skill> result = skillService.findOrCreateSkills(Set.of("java"));

        assertEquals(1, result.size());
        Skill resolved = result.iterator().next();
        assertEquals("Java", resolved.getName());
        assertEquals("Programming Language", resolved.getCategory());
        assertEquals("fa-brands fa-java", resolved.getIcon());
        // The catalogue row must be reused, not overwritten or re-saved.
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void findOrCreateSkills_trimsNamesAndDropsBlankOnes() {
        when(skillRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());

        java.util.Set<String> input = new java.util.HashSet<>();
        input.add("  Vue  ");
        input.add("");
        input.add("   ");
        input.add(null);

        Set<Skill> result = skillService.findOrCreateSkills(input);

        assertEquals(1, result.size());
        assertEquals("Vue", result.iterator().next().getName());
        // Only the one real skill is persisted (no junk rows for blanks).
        verify(skillRepository, times(1)).save(any(Skill.class));
    }

    @Test
    void findOrCreateSkills_returnsEmptySetForNullOrEmptyInput() {
        assertTrue(skillService.findOrCreateSkills(null).isEmpty());
        assertTrue(skillService.findOrCreateSkills(Set.of()).isEmpty());
        verify(skillRepository, never()).save(any(Skill.class));
    }

    /**
     * Phase 0.29 (TASKS §G) — ROOT-CAUSE regression test.
     *
     * <p>This method used to return the immutable {@code Set.of()} for empty
     * input. The caller assigns the result straight onto a managed entity
     * ({@code Experience.setSkills(...)}) and Hibernate's merge path replaces
     * collection contents via {@code clear()} — which threw
     * {@link UnsupportedOperationException} for an immutable set. The result
     * was HTTP 500 on {@code PUT /me/experiences/{uuid}} for ANY experience
     * without skills, the whole transaction rolled back, and EVERY field of
     * that save (including a display-order/position change) was silently
     * discarded. That is the "manual position does not persist" bug the
     * owner reported: entries WITH skills saved fine (200), entries WITHOUT
     * skills always failed (500) behind an error modal.
     */
    @Test
    void findOrCreateSkills_emptyResultMustBeMutable_forHibernateMerge() {
        Set<Skill> empty = skillService.findOrCreateSkills(Set.of());

        assertDoesNotThrow(empty::clear,
                "Hibernate merge calls clear() on this collection — it must be mutable");
        assertDoesNotThrow(() -> empty.add(new Skill()),
                "the caller assigns this set onto a managed entity — it must accept adds");
    }

    @Test
    void findOrCreateSkills_producesDistinctSkillsForDistinctNames() {
        when(skillRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());

        Set<Skill> result = skillService.findOrCreateSkills(
                Set.of("Promotional Strategy", "Inventory & Stock Control"));

        assertEquals(2, result.size());
        assertEquals(
                Set.of("Promotional Strategy", "Inventory & Stock Control"),
                result.stream().map(Skill::getName).collect(Collectors.toSet()));
        assertTrue(result.stream().allMatch(s -> s.getIcon() != null && !s.getIcon().isBlank()));
        assertTrue(result.stream().allMatch(s -> s.getCategory() != null && !s.getCategory().isBlank()));
    }
}
