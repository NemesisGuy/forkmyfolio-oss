package com.forkmyfolio.util;

import com.forkmyfolio.model.Experience;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 0.29 (TASKS §G): pins the experience-ordering contract.
 *
 * <p>Owner requirements under test:
 * <ul>
 *   <li>manual positions (displayOrder != 0) persist and keep their relative
 *       order — chronological sorting must NEVER overwrite them;</li>
 *   <li>position {@code 0} (or null) activates automatic chronological
 *       placement by start/end dates, for current and historical entries;</li>
 *   <li>overlapping employment periods resolve deterministically;</li>
 *   <li>the input list is never mutated (callers hold shared lists).</li>
 * </ul>
 *
 * <p>The integration counterpart ({@code ExperienceOrderingIntegrationTest})
 * proves the same behaviours through the real HTTP API.
 */
class ExperienceOrderingTest {

    private static Experience exp(String title, Integer displayOrder, String start, String end) {
        Experience experience = new Experience();
        experience.setUuid(UUID.randomUUID());
        experience.setJobTitle(title);
        experience.setCompanyName("Ordering Corp");
        experience.setDisplayOrder(displayOrder);
        experience.setStartDate(start == null ? null : LocalDate.parse(start));
        experience.setEndDate(end == null ? null : LocalDate.parse(end));
        return experience;
    }

    private static List<String> titles(List<Experience> experiences) {
        List<String> titles = new ArrayList<>();
        experiences.forEach(e -> titles.add(e.getJobTitle()));
        return titles;
    }

    // ------------------------------------------------------------------
    // Manual ordering persists / is never overwritten (owner tests 1, 7)
    // ------------------------------------------------------------------

    @Test
    void manualPinsKeepRelativeOrder_evenWhenDatesDisagree() {
        // P is OLDER than Q but pinned FIRST — pins win over chronology.
        Experience pinnedOlderFirst = exp("P", 10, "2020-01-01", "2021-01-01");
        Experience pinnedNewerSecond = exp("Q", 20, "2024-01-01", null);

        List<Experience> sorted = ExperienceOrdering.sort(
                List.of(pinnedNewerSecond, pinnedOlderFirst));

        assertEquals(List.of("P", "Q"), titles(sorted),
                "manual slots must control the order regardless of dates");
    }

    @Test
    void manualOrderIsStable_whenAutomaticEntriesArePresent() {
        // Anti-chrono manual backbone (owner test 7: manual ordering must not
        // be unintentionally overwritten by automatic chronological sorting).
        Experience manualOld = exp("Manual-Old", 10, "2020-01-01", "2021-01-01");
        Experience manualNew = exp("Manual-New", 20, "2024-01-01", null);
        Experience automatic = exp("Auto", 0, "2022-06-01", "2022-12-31");

        List<Experience> sorted = ExperienceOrdering.sort(
                List.of(manualNew, automatic, manualOld));

        List<String> result = titles(sorted);
        // The pin invariant: Manual-Old stays ahead of Manual-New.
        assertTrue(result.indexOf("Manual-Old") < result.indexOf("Manual-New"),
                "automatic entries must never reorder pinned entries: " + result);
        // And the pinned values themselves are untouched.
        assertEquals(10, manualOld.getDisplayOrder());
        assertEquals(20, manualNew.getDisplayOrder());
        assertEquals(0, automatic.getDisplayOrder());
    }

    @Test
    void duplicateManualSlots_fallBackToChronology() {
        Experience older = exp("Older", 5, "2023-01-01", null);
        Experience newer = exp("Newer", 5, "2024-01-01", null);

        assertEquals(List.of("Newer", "Older"),
                titles(ExperienceOrdering.sort(List.of(older, newer))),
                "same slot must resolve deterministically by date (newest first)");
    }

    // ------------------------------------------------------------------
    // Position 0 = automatic chronological placement (owner test 5)
    // ------------------------------------------------------------------

    @Test
    void positionZero_insertsChronologicallyBetweenManualPins() {
        // Manual backbone: pinned 10 (2024, current) then pinned 20 (2020).
        Experience manualCurrent = exp("Manual-Current", 10, "2024-01-01", null);
        Experience manualHistorical = exp("Manual-Historical", 20, "2020-01-01", "2021-01-31");
        // Automatic entries: one brand-new (current), one that belongs between.
        Experience autoNewest = exp("Auto-Newest", 0, "2025-03-01", null);
        Experience autoMiddle = exp("Auto-Middle", 0, "2022-06-01", "2022-12-31");

        List<String> result = titles(ExperienceOrdering.sort(
                List.of(manualHistorical, autoMiddle, manualCurrent, autoNewest)));

        assertEquals(List.of("Auto-Newest", "Manual-Current", "Auto-Middle", "Manual-Historical"),
                result,
                "position-0 entries must slot in by date while pins keep their order");
    }

    @Test
    void positionZero_historicalEntrySortsByDate_notToTheTop() {
        Experience manual = exp("Manual", 10, "2024-01-01", null);
        Experience autoHistorical = exp("Auto-Historical", 0, "2019-06-01", "2021-08-31");

        assertEquals(List.of("Manual", "Auto-Historical"),
                titles(ExperienceOrdering.sort(List.of(manual, autoHistorical))),
                "an old automatic entry belongs at the BOTTOM, not the top");
    }

    @Test
    void nullDisplayOrder_isTreatedAsAutomatic() {
        Experience manual = exp("Manual", 10, "2024-01-01", null);
        Experience nullOrder = exp("NullOrder", null, "2021-01-01", "2022-01-01");

        assertTrue(ExperienceOrdering.isAutomatic(nullOrder));
        assertFalse(ExperienceOrdering.isAutomatic(manual));
        assertEquals(List.of("Manual", "NullOrder"),
                titles(ExperienceOrdering.sort(List.of(nullOrder, manual))));
    }

    // ------------------------------------------------------------------
    // Current vs historical + overlapping periods (owner tests 5, 6)
    // ------------------------------------------------------------------

    @Test
    void currentJob_sortsAheadOfEndedJob_withEqualStartDate() {
        Experience current = exp("Current", 0, "2022-01-01", null);
        Experience ended = exp("Ended", 0, "2022-01-01", "2023-06-30");

        assertEquals(List.of("Current", "Ended"),
                titles(ExperienceOrdering.sort(List.of(ended, current))),
                "a current job (null end date) is the newest state of its start date");
    }

    @Test
    void overlappingPeriods_resolveDeterministicallyByStartDescending() {
        Experience longStretch = exp("Long", 0, "2022-01-01", "2023-06-30");
        Experience overlap = exp("Overlap", 0, "2022-05-01", "2022-12-31");
        Experience current = exp("Current", 0, "2025-01-01", null);

        List<String> first = titles(ExperienceOrdering.sort(
                List.of(longStretch, overlap, current)));
        List<String> second = titles(ExperienceOrdering.sort(
                List.of(current, longStretch, overlap)));

        assertEquals(List.of("Current", "Overlap", "Long"), first);
        assertEquals(first, second, "overlapping periods must order identically on every read");
    }

    // ------------------------------------------------------------------
    // Robustness of the sort itself
    // ------------------------------------------------------------------

    @Test
    void inputListIsNeverMutated() {
        Experience a = exp("A", 3, "2023-01-01", null);
        Experience b = exp("B", 1, "2024-01-01", null);
        Experience c = exp("C", 2, "2022-01-01", null);
        List<Experience> input = new ArrayList<>(List.of(a, b, c));

        List<Experience> sorted = ExperienceOrdering.sort(input);

        assertEquals(List.of("A", "B", "C"), titles(input),
                "the caller's list must keep its original order");
        assertNotSame(input, sorted, "sort must return a NEW list");
        assertEquals(List.of("B", "C", "A"), titles(sorted));
    }

    @Test
    void nullAndEmptyInput_returnEmptyList() {
        assertTrue(ExperienceOrdering.sort(null).isEmpty());
        assertTrue(ExperienceOrdering.sort(List.of()).isEmpty());
    }

    @Test
    void sort_isIdempotent_secondPassChangesNothing() {
        Experience manualOld = exp("Manual-Old", 10, "2020-01-01", null);
        Experience manualNew = exp("Manual-New", 20, "2024-01-01", null);
        Experience automatic = exp("Auto", 0, "2022-06-01", null);

        List<Experience> once = ExperienceOrdering.sort(
                List.of(manualNew, automatic, manualOld));
        List<Experience> twice = ExperienceOrdering.sort(once);

        assertEquals(titles(once), titles(twice),
                "re-sorting an already-sorted list must be a no-op (reload safety)");
    }
}
