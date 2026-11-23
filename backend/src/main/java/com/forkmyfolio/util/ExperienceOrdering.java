package com.forkmyfolio.util;

import com.forkmyfolio.model.Experience;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 0.29 (TASKS §G): single source of truth for experience ordering.
 *
 * <p>Contract (owner requirement):
 * <ul>
 *   <li>{@code displayOrder != 0} — MANUAL: the entry is pinned to that exact
 *       slot. Manual entries keep their relative order no matter what the
 *       dates say; only ties (duplicate slots) fall back to dates.</li>
 *   <li>{@code displayOrder == 0} (or null) — AUTOMATIC: the entry is placed
 *       chronologically (start date, newest first) at the position where its
 *       date falls among the other entries. Current jobs (null end date) sort
 *       as newest among equal start dates.</li>
 * </ul>
 *
 * <p>Every read surface must go through this class — admin list, public
 * portfolio payload, guest experience endpoint, Markdown export and all PDF
 * templates — otherwise the same portfolio renders in different orders
 * depending on where you look.
 *
 * <p>The frontend mirrors this algorithm exactly
 * ({@code frontend-admin/src/utils/experienceOrdering.ts} and
 * {@code frontend-public/app/utils/experienceOrdering.ts}); keep them in sync.
 */
public final class ExperienceOrdering {

    private ExperienceOrdering() {
    }

    /** True when the entry should be placed by dates rather than by its pinned slot. */
    public static boolean isAutomatic(Experience experience) {
        return experience.getDisplayOrder() == null || experience.getDisplayOrder() == 0;
    }

    /**
     * Returns a NEW list with the contract applied; the input list is never
     * mutated (callers frequently hold an immutable or shared list).
     * Null input yields an empty list.
     */
    public static List<Experience> sort(List<Experience> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return new ArrayList<>();
        }

        List<Experience> manual = new ArrayList<>();
        List<Experience> automatic = new ArrayList<>();
        for (Experience experience : experiences) {
            if (isAutomatic(experience)) {
                automatic.add(experience);
            } else {
                manual.add(experience);
            }
        }

        manual.sort(byPinnedSlot());
        automatic.sort(byChronological());

        // Merge: walk the pinned backbone and drop each automatic entry at the
        // first position that is strictly OLDER than it (date-desc semantics),
        // i.e. exactly where its chronology places it. Automatic entries are
        // processed newest-first, so previously placed automatic entries are
        // always newer (or equal) and never capture an older one.
        List<Experience> result = new ArrayList<>(manual);
        for (Experience autoEntry : automatic) {
            int index = 0;
            while (index < result.size() && !isStrictlyOlder(result.get(index), autoEntry)) {
                index++;
            }
            result.add(index, autoEntry);
        }
        return result;
    }

    /** Pinned slot first, deterministic date/title tiebreaks for duplicate slots. */
    private static Comparator<Experience> byPinnedSlot() {
        return Comparator.comparing(Experience::getDisplayOrder)
                .thenComparing(byChronological());
    }

    /** Newest first: start date desc, then current job (null end) first, then stable title/company/uuid. */
    private static Comparator<Experience> byChronological() {
        return Comparator.comparing(Experience::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Experience::getEndDate,
                        Comparator.nullsFirst(Comparator.reverseOrder()))
                .thenComparing(Experience::getJobTitle,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Experience::getCompanyName,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Experience::getUuid,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * True when {@code candidate} has a strictly older start date than
     * {@code reference}. Null start date means "unknown/oldest" and therefore
     * sorts after every dated entry; two nulls are not strictly older.
     */
    private static boolean isStrictlyOlder(Experience candidate, Experience reference) {
        LocalDate candidateStart = candidate.getStartDate();
        LocalDate referenceStart = reference.getStartDate();
        if (referenceStart == null) {
            return false; // reference is the oldest possible — append at the end
        }
        if (candidateStart == null) {
            return true; // candidate is older than any dated reference
        }
        return candidateStart.isBefore(referenceStart);
    }
}
