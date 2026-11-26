import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * KNOWN_ISSUES §17 — light-mode washout regression pin (cosmetic-polish
 * batch, roadmap step 2).
 *
 * Root cause being pinned: Phase 8.4 markup used dark-background-only
 * utilities (`text-white`, `text-white-50`, `text-light`, `btn-outline-light`)
 * on theme-aware light-glass surfaces. In LIGHT mode those render
 * white-on-light — the page header, the 15 status chips and the empty states
 * were nearly invisible (evidence: `screenshots/phase032-02-tracker-grid.png`).
 * Dark mode was unaffected, which is why it survived so long.
 *
 * The fix swaps them for theme-safe utilities (`btn-outline-secondary` — the
 * codebase's house pattern — `text-body-secondary`, or simply inheriting the
 * body colour). This guard scans SOURCE files so the washout classes cannot
 * creep back in; the rendered contract for the job-tracker list itself is
 * additionally pinned in `JobApplicationList.spec.ts`.
 *
 * Intentionally NOT guarded (legitimate uses):
 * - `views/public/LandingPage.vue` — scoped CSS re-defines `.btn-outline-light`
 *   per `[data-bs-theme]`, making it theme-aware by construction.
 * - `components/user/GuestPassManagement.vue` — `text-white` sits on a solid
 *   `bg-success`/`bg-danger` toast (contrast is intentional).
 * - `components/public/QualificationDetailsModal.vue` — comment text only
 *   (the actual classes were removed in an earlier fix).
 */

const vueRoot = resolve(__dirname);

/** Files fixed in the §17 polish batch — none may regain a washout class. */
const guardedFiles = [
  'components/job-tracker/JobApplicationList.vue',
  'components/job-tracker/JobApplicationDetailsModal.vue',
  'views/auth/AcceptTermsPage.vue',
  'views/admin/AdminStats.vue',
  'views/user/UserAnalytics.vue',
];

/** Dark-background-only utilities that wash out on light-glass surfaces. */
const washoutPattern = /btn-outline-light|text-white|text-light/;

describe('light-mode washout guard (KNOWN_ISSUES §17)', () => {
  it.each(guardedFiles)('%s uses only theme-safe colour utilities', (file) => {
    const source = readFileSync(resolve(vueRoot, file), 'utf-8');
    const match = source.match(washoutPattern);
    expect(match).toBeNull(
      `${file} reintroduced the washout class "${match?.[0]}" — light mode renders it invisible`,
    );
  });
});
