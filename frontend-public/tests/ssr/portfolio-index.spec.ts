import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mountSuspended } from '@nuxt/test-utils/runtime';
import PortfolioIndex from '../../../app/pages/[slug]/index.vue';

/**
 * NOTE (Phase 7.3): this app previously had ZERO tests. This suite renders
 * the public portfolio homepage (/[slug]) through the real Nuxt context
 * (`mountSuspended` — the same render path SSR uses) and asserts the three
 * top-level UI states the backend Phase 7.2 integration suite guarantees:
 *
 *   1. 200 public portfolio  -> hero content rendered server-side
 *   2. 403 private portfolio -> dedicated "private" UI (NOT a generic error)
 *   3. 500 server failure    -> generic error UI with a retry affordance
 *
 * fetchJson uses the global `fetch`, so we stub it at that level; the API
 * error message (`API error <status>: <body>`) matches what the real backend
 * produces via the GlobalExceptionHandler wrapping contract.
 *
 * IMPORTANT: the page derives its useAsyncData cache key from the slug
 * (`portfolio-${slug}`). useAsyncData payloads are cached in the SHARED Nuxt
 * context across tests, so every test mounts with a UNIQUE slug via the
 * `route` option — otherwise the first test's success payload leaks into the
 * 403/500 tests (this burned the first run of this suite).
 */
const portfolioFixture = (slug: string) => ({
  data: {
    user: {
      slug,
      firstName: 'Ada',
      lastName: 'Lovelace',
      email: 'ada@example.com',
    },
    profile: {
      headline: 'Analytical Engine Evangelist',
      summary: 'Pioneering programmer. Notes A fan.',
      linkedinUrl: 'https://linkedin.com/in/adalovelace',
      githubUrl: 'https://github.com/adalovelace',
      publicEmail: 'ada@example.com',
      profileImageUrl: null,
      resumeUrl: null,
      coverLetterTemplate: null,
      websiteUrl: null,
    },
    projects: [],
    skills: [],
  },
});

const stubFetch = (status: number, payload: unknown) => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () =>
      new Response(typeof payload === 'string' ? payload : JSON.stringify(payload), {
        status,
        headers: { 'content-type': 'application/json' },
      })
    )
  );
};

describe('pages/[slug]/index.vue (Nuxt SSR render)', () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
  });
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('renders the public portfolio hero server-side (200 contract)', async () => {
    stubFetch(200, portfolioFixture('alice'));
    const wrapper = await mountSuspended(PortfolioIndex, {
      route: { params: { slug: 'alice' } },
    });

    expect(wrapper.html()).toContain('Ada Lovelace');
    expect(wrapper.html()).toContain('Analytical Engine Evangelist');
    // social links resolve from the profile
    const github = wrapper.find('a[href="https://github.com/adalovelace"]');
    expect(github.exists()).toBe(true);
  });

  it('shows the dedicated unavailable UI for a concealed 404 (private or nonexistent)', async () => {
    // Phase 7.4 (403/404 policy): the backend conceals private portfolios as
    // 404 — identical to a nonexistent slug — and the page must render the
    // neutral "not available" state (no "private" phrasing that would
    // re-introduce the existence disclosure).
    stubFetch(404, 'User with slug \'private-bob\' not found.');
    const wrapper = await mountSuspended(PortfolioIndex, {
      route: { params: { slug: 'private-bob' } },
    });

    const text = wrapper.text();
    // The dedicated unavailable branch owns the render (v-else-if="isPrivate")
    expect(text).toContain('Portfolio Not Available');
    expect(text).toContain('does not exist or is not publicly accessible');
    // hero content must NOT be rendered, and the generic error branch must
    // NOT win (isPrivate takes priority over error in the template chain)
    expect(wrapper.html()).not.toContain('Ada Lovelace');
    expect(text).not.toContain('Unable to Load Profile');
  });

  it('still treats a 403 defensively as the unavailable UI (not a generic error)', async () => {
    stubFetch(403, 'This portfolio is private.');
    const wrapper = await mountSuspended(PortfolioIndex, {
      // NB: useAsyncData keys cache per slug in the shared Nuxt context —
      // every test must mount with a unique slug (suite gotcha).
      route: { params: { slug: 'private-carol' } },
    });

    const text = wrapper.text();
    expect(text).toContain('Portfolio Not Available');
    expect(text).toContain('does not exist or is not publicly accessible');
    expect(wrapper.html()).not.toContain('Ada Lovelace');
    expect(text).not.toContain('Unable to Load Profile');
  });

  it('shows the generic error UI with a retry affordance for server failures (500 contract)', async () => {
    stubFetch(500, 'Kaboom');
    const wrapper = await mountSuspended(PortfolioIndex, {
      route: { params: { slug: 'error-bob' } },
    });

    const text = wrapper.text();
    expect(text).toContain('Unable to Load Profile');
    expect(text).toContain('Try Again');
    // hero content must NOT be rendered
    expect(wrapper.html()).not.toContain('Ada Lovelace');
    expect(wrapper.html()).not.toContain('Analytical Engine Evangelist');
  });
});
