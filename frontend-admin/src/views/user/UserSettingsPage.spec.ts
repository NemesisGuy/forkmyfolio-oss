import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import UserSettingsPage from './UserSettingsPage.vue';

/**
 * Phase 7.4 §5 — visibility synchronisation regression tests for the
 * Display Settings page (master visibility toggle + section saves).
 *
 * Root cause being pinned: the master toggle changed the backend but left the
 * singleton publicPortfolioStore cached; the router guard's fetchPortfolio
 * dedupe then served the STALE cached success and App.vue's global
 * PrivateLockScreen never reacted (visibility only "worked" after a hard
 * reload). The fix clears the store after every SUCCESSFUL toggle.
 */

// Controllable fake of the singleton public portfolio store (same ref-shape
// the real module exposes) so tests can seed stale cached state and assert
// the page synchronises it.
const storeState = vi.hoisted(() => {
  const s: {
    portfolio: { value: unknown },
    isLoading: { value: boolean },
    error: { value: unknown },
    isPrivate: { value: boolean },
    currentSlug: { value: string | null },
    clearPortfolio: ReturnType<typeof import('vitest').vi.fn>,
  } = {
    portfolio: { value: null as unknown },
    isLoading: { value: false },
    error: { value: null as unknown },
    isPrivate: { value: false },
    currentSlug: { value: null },
    clearPortfolio: undefined as never,
  };
  return s;
});
storeState.clearPortfolio = vi.fn(() => {
  storeState.portfolio.value = null;
  storeState.error.value = null;
  storeState.isLoading.value = false;
  storeState.currentSlug.value = null;
  storeState.isPrivate.value = false;
});

vi.mock('@/stores/publicPortfolioStore.js', () => ({
  usePublicPortfolioStore: () => storeState,
}));

vi.mock('@/services/api/user.api.js', () => ({
  getMyPublicProfile: vi.fn(),
  settingsApi: {
    getAll: vi.fn(),
    update: vi.fn(),
  },
  updateMyProfileVisibility: vi.fn(),
  updateMyProfileIndexing: vi.fn(),
}));

vi.mock('@/services/notificationService.js', () => ({
  notificationService: { add: vi.fn() },
}));

vi.mock('@/services/settingsService.js', () => ({
  settingsService: { updateSettings: vi.fn() },
}));

import {
  getMyPublicProfile,
  settingsApi,
  updateMyProfileVisibility,
  updateMyProfileIndexing,
} from '@/services/api/user.api.js';
import { notificationService } from '@/services/notificationService.js';
import { settingsService } from '@/services/settingsService.js';

const mountPage = async () => {
  const wrapper = mount(UserSettingsPage, {
    global: { stubs: ['SuccessModal', 'ErrorModal', 'LoadingModal'] },
  });
  // onMounted resolves the parallel profile+settings fetch AND waits a
  // deliberate 500ms "minDelay" before releasing the loading gate — advance
  // the fake clock through it.
  await flushPromises();
  await vi.advanceTimersByTimeAsync(600);
  await flushPromises();
  return wrapper;
};

describe('UserSettingsPage.vue (Phase 7.4 §5 regression)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    // Reset singleton-shape fake store
    storeState.portfolio.value = { stale: true };
    storeState.currentSlug.value = 'me';
    storeState.isPrivate.value = false;
    storeState.error.value = null;
    storeState.isLoading.value = false;
    storeState.clearPortfolio.mockClear();

    vi.mocked(getMyPublicProfile).mockResolvedValue({ isPublic: true } as never);
    vi.mocked(settingsApi.getAll).mockResolvedValue([
      { name: 'portfolio.projects.show', description: 'Projects', value: 'true', uuid: 'uuid-projects' },
      { name: 'portfolio.contact.show', description: 'Contact', value: 'true', uuid: 'uuid-contact' },
    ] as never);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders master visibility from the authoritative profile (isPublic contract)', async () => {
    const wrapper = await mountPage();

    const toggle = wrapper.find('#visibilityToggle');
    expect(toggle.element.checked).toBe(true);
  });

  it('clears the cached public-portfolio store after a SUCCESSFUL master toggle (sync fix)', async () => {
    const wrapper = await mountPage();

    // seed "stale cached" state, then toggle public -> private
    storeState.currentSlug.value = 'me';
    storeState.portfolio.value = { stale: true };
    await wrapper.find('#visibilityToggle').setValue(false);
    await flushPromises();

    expect(updateMyProfileVisibility).toHaveBeenCalledWith({ isPublic: false });
    // The stale cache must be invalidated so the next navigation re-fetches
    expect(storeState.portfolio.value).toBeNull();
    expect(storeState.currentSlug.value).toBeNull();
    expect(storeState.clearPortfolio).toHaveBeenCalledTimes(1);
    expect(notificationService.add).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'success', message: expect.stringContaining('private') })
    );
  });

  it('reverts the toggle and leaves the store cache untouched on FAILURE (no misleading UI)', async () => {
    vi.mocked(updateMyProfileVisibility).mockRejectedValueOnce(new Error('500 boom'));
    const wrapper = await mountPage();

    await wrapper.find('#visibilityToggle').setValue(false);
    await flushPromises();

    // optimistic value reverted...
    expect(wrapper.find('#visibilityToggle').element.checked).toBe(true);
    // ...and NO store invalidation happened (the backend still says public)
    expect(storeState.portfolio.value).not.toBeNull();
    expect(storeState.currentSlug.value).toBe('me');
    expect(notificationService.add).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'error' })
    );
  });

  it('disables the master toggle while a visibility request is in flight (no concurrent toggles)', async () => {
    let release!: (v: unknown) => void;
    vi.mocked(updateMyProfileVisibility).mockReturnValueOnce(new Promise(resolve => { release = resolve; }));
    const wrapper = await mountPage();

    await wrapper.find('#visibilityToggle').setValue(false);

    const toggle = wrapper.find('#visibilityToggle');
    expect(toggle.element.disabled).toBe(true);

    release(undefined);
    await flushPromises();
    expect(wrapper.find('#visibilityToggle').element.disabled).toBe(false);
  });

  it('saves only changed section settings and syncs settingsService from the authoritative refreshed list', async () => {
    // Backend contract: PUT /me/settings returns the FULL refreshed effective list
    const refreshed = [
      { name: 'portfolio.projects.show', value: 'false' },
      { name: 'portfolio.contact.show', value: 'true' },
      { name: 'portfolio.skills.show', value: 'true' },
    ];
    vi.mocked(settingsApi.update).mockResolvedValueOnce(refreshed as never);
    const wrapper = await mountPage();

    // Flip ONLY the projects switch (id contains dots → attribute selector)
    await wrapper.find('input[id="switch-portfolio.projects.show"]').setValue(false);
    await wrapper.find('form button[type="submit"]').trigger('submit');
    await flushPromises();

    // payload contains ONLY the changed setting
    expect(settingsApi.update).toHaveBeenCalledWith([{ uuid: 'uuid-projects', value: 'false' }]);
    // but the central settings cache receives the FULL authoritative response
    expect(settingsService.updateSettings).toHaveBeenCalledWith(refreshed);
  });

  // --- Phase 10 (Workstream 2.5): search-engine indexing preference ---

  it('renders the indexing toggle conservatively (off) when the profile payload lacks isSearchIndexable', async () => {
    const wrapper = await mountPage();

    const toggle = wrapper.find('#indexingToggle');
    expect(toggle.exists()).toBe(true);
    // Absent field (pre-V10 backend) must map to opted-out, never opt-in.
    expect(toggle.element.checked).toBe(false);
  });

  it('renders the indexing toggle from the profile payload when explicitly true', async () => {
    vi.mocked(getMyPublicProfile).mockResolvedValue({ isPublic: true, isSearchIndexable: true } as never);
    const wrapper = await mountPage();

    expect(wrapper.find('#indexingToggle').element.checked).toBe(true);
  });

  it('calls the dedicated indexing endpoint and clears the store cache on a SUCCESSFUL toggle', async () => {
    vi.mocked(getMyPublicProfile).mockResolvedValue({ isPublic: true, isSearchIndexable: true } as never);
    const wrapper = await mountPage();

    storeState.portfolio.value = { stale: true };
    await wrapper.find('#indexingToggle').setValue(false);
    await flushPromises();

    expect(updateMyProfileIndexing).toHaveBeenCalledWith({ isSearchIndexable: false });
    expect(updateMyProfileVisibility).not.toHaveBeenCalled();
    // same cache-invalidation contract as the visibility toggle
    expect(storeState.portfolio.value).toBeNull();
    expect(storeState.clearPortfolio).toHaveBeenCalledTimes(1);
    expect(notificationService.add).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'success' })
    );
  });

  it('reverts the indexing toggle on FAILURE', async () => {
    vi.mocked(getMyPublicProfile).mockResolvedValue({ isPublic: true, isSearchIndexable: true } as never);
    vi.mocked(updateMyProfileIndexing).mockRejectedValueOnce(new Error('500 boom'));
    const wrapper = await mountPage();

    await wrapper.find('#indexingToggle').setValue(false);
    await flushPromises();

    expect(wrapper.find('#indexingToggle').element.checked).toBe(true);
    expect(notificationService.add).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'error' })
    );
  });
});
