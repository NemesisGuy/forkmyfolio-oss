import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * Phase 7.4 §10 — self-XSS hardening regression pin.
 *
 * Root cause being pinned: experience description/achievements (owner-entered
 * text) used to be rendered with v-html, which executes owner-supplied HTML
 * (e.g. <img onerror>) in EVERY visitor's browser. The sinks were switched to
 * text interpolation — this spec guards against the sinks being reintroduced
 * by future edits.
 *
 * This intentionally scans SOURCE files rather than mounting components:
 * the guarantee being pinned is "renders as text", which Vue interpolation
 * provides by construction — mounting would only re-test the compiler.
 */

const vueRoot = resolve(__dirname, '..');

/** Relative paths that historically contained user-data v-html sinks. */
const guardedFiles = [
  'views/public/ExperiencePage.vue',
  'components/public/ExperienceDetailsModal.vue',
  'views/user/UserExperiencePage.vue',
];

const userSinkPattern = /v-html\s*=\s*"(exp\.|experience\.)/;

describe('self-XSS hardening (Phase 7.4 §10)', () => {
  it.each(guardedFiles)('%s renders owner text as TEXT (no user-data v-html)', (file) => {
    const source = readFileSync(resolve(vueRoot, file), 'utf-8');
    expect(userSinkPattern.test(source)).toBe(false);
  });
});
