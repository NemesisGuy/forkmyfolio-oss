/**
 * @file app/utils/experienceOrdering.ts
 * @description Phase 0.29 (TASKS §G) — client mirror of the backend
 * `com.forkmyfolio.util.ExperienceOrdering` contract.
 *
 * Nuxt auto-imports functions from `app/utils`, no manual import needed.
 *
 * The backend already serves experiences in the correct order; re-applying
 * the SAME contract here means this page can never disagree with the admin
 * list, the guest API or the exports — even against a stale cached payload.
 *
 * Contract (MUST stay in sync with ExperienceOrdering.java and
 * frontend-admin/src/utils/experienceOrdering.ts):
 *  - displayOrder != 0 (and not null) → MANUAL: pinned to that exact slot,
 *    relative order preserved no matter what the dates say;
 *  - displayOrder === 0 or null      → AUTOMATIC: placed chronologically
 *    (start date, newest first) at the first position whose entry is older.
 */

export interface OrderableExperience {
  displayOrder?: number | null;
  startDate?: string | null;
  endDate?: string | null;
  jobTitle?: string | null;
  companyName?: string | null;
  uuid?: string | null;
}

/** True when the entry should be placed by dates rather than by its pinned slot. */
export const isAutomaticExperience = (experience: OrderableExperience): boolean =>
  experience.displayOrder == null || experience.displayOrder === 0;

/** Epoch ms for an ISO date; null/invalid → null (treated as "oldest"). */
const timeOf = (value?: string | null): number | null => {
  if (!value) return null;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? null : time;
};

/**
 * Newest first: start date desc → current job (null end) first → stable
 * title/company/uuid tiebreaks. Mirrors ExperienceOrdering.byChronological().
 */
const byChronological = (a: OrderableExperience, b: OrderableExperience): number => {
  const aStart = timeOf(a.startDate);
  const bStart = timeOf(b.startDate);
  if (aStart === null && bStart !== null) return 1; // null start sorts last
  if (bStart === null && aStart !== null) return -1;
  if (aStart !== null && bStart !== null && aStart !== bStart) return bStart - aStart;

  const aEnd = timeOf(a.endDate);
  const bEnd = timeOf(b.endDate);
  if (aEnd === null && bEnd !== null) return -1; // current job first
  if (bEnd === null && aEnd !== null) return 1;
  if (aEnd !== null && bEnd !== null && aEnd !== bEnd) return bEnd - aEnd;

  const aTitle = a.jobTitle ?? '';
  const bTitle = b.jobTitle ?? '';
  if (aTitle !== bTitle) return aTitle < bTitle ? -1 : 1;

  const aCompany = a.companyName ?? '';
  const bCompany = b.companyName ?? '';
  if (aCompany !== bCompany) return aCompany < bCompany ? -1 : 1;

  const aUuid = a.uuid ?? '';
  const bUuid = b.uuid ?? '';
  if (aUuid !== bUuid) return aUuid < bUuid ? -1 : 1;
  return 0;
};

/** True when `candidate` has a strictly older start date than `reference`. */
const isStrictlyOlder = (candidate: OrderableExperience, reference: OrderableExperience): boolean => {
  const referenceStart = timeOf(reference.startDate);
  if (referenceStart === null) return false; // reference is the oldest possible
  const candidateStart = timeOf(candidate.startDate);
  if (candidateStart === null) return true; // candidate is older than any dated entry
  return candidateStart < referenceStart;
};

/**
 * Returns a NEW array with the ordering contract applied; the input array is
 * never mutated. Null/empty input → empty array.
 */
export const sortExperiences = <T extends OrderableExperience>(experiences: T[] | null | undefined): T[] => {
  if (!experiences || experiences.length === 0) return [];

  const manual: T[] = [];
  const automatic: T[] = [];
  for (const experience of experiences) {
    if (isAutomaticExperience(experience)) automatic.push(experience);
    else manual.push(experience);
  }

  manual.sort((a, b) => {
    const orderA = a.displayOrder ?? 0;
    const orderB = b.displayOrder ?? 0;
    if (orderA !== orderB) return orderA - orderB;
    return byChronological(a, b);
  });
  automatic.sort(byChronological);

  // Merge: walk the pinned backbone and drop each automatic entry at the
  // first position that is strictly OLDER than it (date-desc semantics).
  // Automatic entries are processed newest-first, so this mirrors
  // ExperienceOrdering.sort() exactly.
  const result: T[] = [...manual];
  for (const autoEntry of automatic) {
    let index = 0;
    while (index < result.length && !isStrictlyOlder(result[index], autoEntry)) {
      index++;
    }
    result.splice(index, 0, autoEntry);
  }
  return result;
};
