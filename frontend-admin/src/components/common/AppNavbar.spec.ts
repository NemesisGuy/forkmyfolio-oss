import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import type { VueWrapper } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import AppNavbar from './AppNavbar.vue';

/**
 * Guest-pass menu fix (v1.2.11): the user dropdown must not offer owner-only
 * items to a GUEST session, and the toggle must not present the owner's name
 * as if the guest were logged in as them.
 *
 * <p>Root cause pinned: every sibling item carries `v-if="!isGuest"`, but
 * "Display Settings" (and the owner-scoped "My Public Page") were added later
 * without the gate — a guest could open the settings page (reads are allowed)
 * and hit 403s on every save. The backend enforcement was already correct
 * ({@code PublicPortfolioVisibilityIntegrationTest}: guest write -> 403);
 * this spec pins the UI half in BOTH navbars (the Nuxt twin lives in
 * frontend-public/tests/unit/AppNavbar.spec.ts).
 */

// Controllable fake of the authService singleton (same ref-shape the real
// module exposes) so each test can seed a guest vs. owner session.
const authState = vi.hoisted(() => ({
  user: { value: null as unknown },
  isAuthenticated: { value: true },
}));

vi.mock('@/services/authService.js', () => ({
  authService: {
    user: authState.user,
    isAuthenticated: authState.isAuthenticated,
    logout: vi.fn(),
  },
}));

vi.mock('@/stores/publicPortfolioStore.js', () => ({
  usePublicPortfolioStore: () => ({
    currentSlug: { value: null },
    isPrivate: { value: false },
    fetchPortfolio: vi.fn(),
    clearPortfolio: vi.fn(),
  }),
}));

vi.mock('@/services/settingsService.js', () => ({
  settingsService: {
    initialize: vi.fn(),
    // Template calls settingsService.isEnabled.value('portfolio.*.show').
    isEnabled: { value: () => true },
  },
}));

vi.mock('@/services/themeService.js', () => ({
  useTheme: () => ({ currentTheme: 'light' }),
}));

vi.mock('@/services/messageNotificationService.js', () => ({
  messageNotificationService: { unreadCount: 0 },
}));

// Minimal guard-free router carrying every route name the navbar links to:
// RouterLink.resolve() must not warn/throw, and RouterLink must render real
// anchors so dropdown item text stays assertable.
const ROUTE_NAMES = [
  'home', 'dashboard', 'profile', 'my-guest-passes', 'portfolio-home',
  'display-settings', 'pdf-settings', 'export-settings', 'job-tracker',
  'analytics', 'user-messages', 'contact', 'projects-public', 'skills-public',
  'experience-public', 'testimonials-public', 'qualifications-public',
];

const makeRouter = () =>
  createRouter({
    history: createMemoryHistory(),
    routes: ROUTE_NAMES.map((name) => ({
      path: name === 'home' ? '/' : `/stub/${name}`,
      name,
      component: { render: () => null },
    })),
  });

const OWNER = {
  slug: 'mike',
  firstName: 'Mike',
  roles: ['USER'],
  profileImageUrl: null,
};

const GUEST = {
  slug: 'mike',
  firstName: 'Mike',
  roles: ['GUEST'],
  profileImageUrl: null,
};

const OWNER_ONLY_ITEMS = [
  'My Dashboard',
  'My Profile',
  'Guest Passes',
  'My Public Page',
  'Display Settings',
  'PDF Settings',
  'Export Settings',
  'Job Applications',
  'Analytics',
];

const mountNavbar = async (user: typeof OWNER) => {
  authState.user.value = user;
  const wrapper = mount(AppNavbar, {
    global: {
      plugins: [makeRouter()],
      stubs: ['AnnouncementDropdown', 'ThemeToggle', 'ConfirmModal'],
    },
  });
  await flushPromises();
  // The context watcher debounces 300ms before touching the (mocked) store —
  // advance the fake clock so it runs inside the test, deterministically.
  await vi.advanceTimersByTimeAsync(400);
  await flushPromises();
  return wrapper;
};

const dropdownItems = (wrapper: VueWrapper) =>
  wrapper
    .find('ul[aria-labelledby="navbarUserDropdown"]')
    .findAll('.dropdown-item')
    .map((node) => node.text().trim());

const userMenuDividers = (wrapper: VueWrapper) =>
  wrapper.find('ul[aria-labelledby="navbarUserDropdown"]').findAll('hr.dropdown-divider');

describe('AppNavbar.vue guest menu gating (v1.2.11 guest-pass fix)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    authState.isAuthenticated.value = true;
    authState.user.value = null;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('hides every owner-only menu item from a guest session', async () => {
    const wrapper = await mountNavbar(GUEST);
    const items = dropdownItems(wrapper);

    // The guest keeps the one item that works for them: leaving the session.
    expect(items).toContain('Logout');
    for (const ownerItem of OWNER_ONLY_ITEMS) {
      expect(items).not.toContain(ownerItem);
    }
    // Grouping dividers collapse with their groups: guest sees exactly one
    // divider (the one above Logout) — not the double-hr the naive gate left.
    expect(userMenuDividers(wrapper)).toHaveLength(1);

    wrapper.unmount();
  });

  it('shows a Guest badge instead of the owner name in the toggle', async () => {
    const wrapper = await mountNavbar(GUEST);

    const toggle = wrapper.find('#navbarUserDropdown').text();
    expect(toggle).toContain('Guest');
    expect(toggle).not.toContain('Mike');

    wrapper.unmount();
  });

  it('still offers every owner-only item to a real owner (positive control)', async () => {
    const wrapper = await mountNavbar(OWNER);
    const items = dropdownItems(wrapper);

    for (const ownerItem of OWNER_ONLY_ITEMS) {
      expect(items).toContain(ownerItem);
    }
    expect(items).toContain('Logout');
    expect(userMenuDividers(wrapper)).toHaveLength(2);

    const toggle = wrapper.find('#navbarUserDropdown').text();
    expect(toggle).toContain('Mike');
    expect(toggle).not.toContain('Guest');

    wrapper.unmount();
  });
});
