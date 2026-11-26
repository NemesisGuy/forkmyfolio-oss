import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mountSuspended } from '@nuxt/test-utils/runtime';
import AppNavbar from '~/components/AppNavbar.vue';

/**
 * Guest-pass menu fix (v1.2.11) — Nuxt (public) navbar twin of
 * frontend-admin/src/components/common/AppNavbar.spec.ts.
 *
 * <p>This is the navbar from the owner's bug report (full page load of a
 * portfolio as a guest): the session legitimately carries the OWNER's
 * identity (documented design — a scoped ROLE_GUEST JWT is what lets guests
 * read private portfolios), but the dropdown leaked owner-only items
 * ("Display Settings" and "My Public Page" were missing the
 * `v-if="!isGuest"` gate every sibling item carries) and the toggle showed
 * the owner's first name as if the guest were logged in as them.
 *
 * <p>The component restores its session client-side in onMounted()
 * (localStorage token -> GET /me), so these tests seed a token and stub the
 * global fetch, then wait for the account section to appear.
 */

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
];

const stubFetchFor = (user: typeof OWNER) => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      const url = String(input);
      // useApi().fetchJson unwraps the { data } envelope itself.
      const body = url.endsWith('/me')
        ? { data: user }
        : { data: { unreadCount: 0 } };
      return {
        ok: true,
        status: 200,
        json: async () => body,
        text: async () => JSON.stringify(body),
      };
    }),
  );
};

const mountNavbar = async (user: typeof OWNER) => {
  localStorage.setItem('accessToken', 'e2e-guest-token');
  stubFetchFor(user);

  const wrapper = await mountSuspended(AppNavbar);

  // The account section lives inside <ClientOnly> and only renders after
  // restoreSession() resolves GET /me — wait for the toggle to land.
  await vi.waitFor(
    () => {
      expect(wrapper.find('#navbarUserDropdown').exists()).toBe(true);
    },
    { timeout: 10_000 },
  );
  return wrapper;
};

const userMenu = (wrapper: Awaited<ReturnType<typeof mountNavbar>>) =>
  wrapper.find('ul[aria-labelledby="navbarUserDropdown"]');

const dropdownItems = (wrapper: Awaited<ReturnType<typeof mountNavbar>>) =>
  userMenu(wrapper)
    .findAll('.dropdown-item')
    .map((node) => node.text().trim());

describe('AppNavbar.vue (Nuxt) guest menu gating (v1.2.11 guest-pass fix)', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('hides every owner-only menu item from a guest session', async () => {
    const wrapper = await mountNavbar(GUEST);
    const items = dropdownItems(wrapper);

    expect(items).toContain('Logout');
    for (const ownerItem of OWNER_ONLY_ITEMS) {
      expect(items).not.toContain(ownerItem);
    }
    // Grouping dividers collapse with their groups: exactly one divider
    // (above Logout) — not the double-hr a naive per-item gate would leave.
    expect(userMenu(wrapper).findAll('hr.dropdown-divider')).toHaveLength(1);

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
    expect(userMenu(wrapper).findAll('hr.dropdown-divider')).toHaveLength(2);

    const toggle = wrapper.find('#navbarUserDropdown').text();
    expect(toggle).toContain('Mike');
    expect(toggle).not.toContain('Guest');

    wrapper.unmount();
  });
});
