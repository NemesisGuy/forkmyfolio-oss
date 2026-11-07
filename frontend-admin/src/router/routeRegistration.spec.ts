/**
 * @file src/router/routeRegistration.spec.ts
 * @description Phase 7 UAT-fix regression tests for ROUTE-NAME REGISTRATION
 *              integrity (UAT-5 bug found during the 2026-11-03 UAT round).
 *
 * Root cause: RouterLinks whose `to` object referenced route names that were
 * never registered ('job-tracker' on the dashboard, 'settings' on HomePage).
 * vue-router's matcher threw a NO_MATCH RouterError (an EMPTY-message Error)
 * inside the RouterLink resolve computed on every render — visible as bare
 * "Error at os / Object.u [as resolve]" console entries during every auth
 * transition into the dashboard. The dashboard card's button also routed
 * nowhere when clicked.
 *
 * These tests pin: (a) the curse list of names actually used in templates
 * resolves successfully, and (b) the previously-dead names stay dead.
 */
import { describe, it, expect } from 'vitest';
import { createRouter, createMemoryHistory } from 'vue-router';

import routerProduction from '@/router';

const DEMO_SLUG_PARAM = { slug: 'demo-user' };

/** Records needing params BEYOND the standard slug (path shape in index.js). */
const EXTRA_PARAMS: Record<string, Record<string, string>> = {
  'project-details': { uuid: '00000000000000000000000000000000' },
  'admin-user-edit': { id: '1' },
};

/**
 * Every route name referenced by a <router-link :to="{ name: ... }"> in the
 * production templates (navbar, dashboard cards, HomePage, public pages).
 * If a new named link is added to a template, add it here — mounting the page
 * with a bad name throws inside vue-router's resolve computed.
 */
const LINK_NAMES_HISTORY = [
  // Global chrome
  'home', 'login', 'register', 'not-found', 'guest-access', 'accept-terms',
  'force-password-change',
  // Public portfolio pages
  'portfolio-home', 'projects-public', 'skills-public', 'experience-public',
  'testimonials-public', 'qualifications-public', 'contact', 'project-details',
  // Dashboard cards / management pages
  'dashboard', 'profile', 'edit-account', 'edit-public-profile',
  'my-projects', 'my-skills', 'my-experience', 'my-testimonials',
  'my-qualifications', 'my-backup', 'my-data-exports', 'my-guest-passes',
  'user-messages', 'display-settings', 'pdf-settings', 'export-settings',
  // UAT-5 fix: previously referenced but never registered
  'job-tracker',
  // Admin surfaces
  'admin', 'admin-messages', 'admin-users', 'admin-user-create',
  'admin-user-edit', 'admin-settings', 'admin-pdf-settings', 'admin-stats',
  'admin-system-backup',
];

const BAD_NAMES_ONCE_REFERENCED = [
  { name: 'settings', usedAt: 'src/views/public/HomePage.vue (admin tip) — replaced with display-settings' },
];

describe('router route-name registration (UAT-5 regression)', () => {
  it('every production template link name is a registered route and resolves', async () => {
    // NOTE: do NOT await routerProduction.isReady() here — the guard's initial
    // navigation calls settingsService/authService which hit the network in the
    // happy-dom env and would hang. resolve() is synchronous and independent of
    // the initial navigation state.
    for (const name of LINK_NAMES_HISTORY) {
      const params = { ...DEMO_SLUG_PARAM, ...(EXTRA_PARAMS[name] ?? {}) };
      let resolved;
      try {
        resolved = routerProduction.resolve({ name, params });
      } catch (e) {
        throw new Error(`route name "${name}" does NOT resolve (NO_MATCH risk): ${e}`);
      }
      expect(resolved.name, `resolved record name for "${name}"`).toBe(name);
    }
  });

  it('the dashboard job-tracker record exists at the documented manage path', () => {
    expect(routerProduction.hasRoute('job-tracker')).toBe(true);
    const resolved = routerProduction.resolve({ name: 'job-tracker', params: DEMO_SLUG_PARAM });
    expect(resolved.path).toBe('/demo-user/manage/job-tracker');
  });

  it('previously-referenced dead names are NOT re-registrable typos', () => {
    for (const bad of BAD_NAMES_ONCE_REFERENCED) {
      const msg = `dead name "${bad.name}" (${bad.usedAt}) must not exist`;
      expect(routerProduction.hasRoute(bad.name), msg).toBe(false);
    }
  });

  it('the public portfolio route-name export matches the guard source of truth', async () => {
    const { PUBLIC_PORTFOLIO_ROUTE_NAMES } = await import('@/router');
    for (const name of ['portfolio-home', 'projects-public', 'skills-public',
      'experience-public', 'testimonials-public', 'qualifications-public',
      'contact', 'project-details']) {
      expect(PUBLIC_PORTFOLIO_ROUTE_NAMES.has(name)).toBe(true);
    }
    // Names that must NEVER be treated as portfolio routes (UAT-1 regression)
    for (const name of ['login', 'register', 'home', 'dashboard', 'not-found']) {
      expect(PUBLIC_PORTFOLIO_ROUTE_NAMES.has(name)).toBe(false);
    }
  });

  it('a sidebar-style in-memory router still refuses unknown names (sanity that the throw is matcher-level)', async () => {
    const localRouter = createRouter({ history: createMemoryHistory(), routes: [{ path: '/x', name: 'x', component: {} }] });
    expect(localRouter.hasRoute('job-tracker')).toBe(false);
    expect(() => localRouter.resolve({ name: 'job-tracker', params: DEMO_SLUG_PARAM })).toThrow();
    expect(() => localRouter.resolve({ name: 'settings' })).toThrow();
    void LINK_NAMES_HISTORY; // keep scope tidy for tree-shaken builds
  });
});
