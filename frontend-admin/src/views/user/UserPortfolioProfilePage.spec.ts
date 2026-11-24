import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import UserPortfolioProfilePage from './UserPortfolioProfilePage.vue';

/**
 * Phase 0.31 (TASKS §H1 / KNOWN_ISSUES §14) — visibility contract for the
 * Manage Profile page.
 *
 * Root cause being pinned: the "Public Portfolio Visibility" switch bound
 * `formState.publiclyVisible` — a key that (a) GET /me/profile never returns
 * (it serialises as `isPublic`) and (b) the profile-save DTO ignores. The
 * switch therefore rendered unchecked even while the portfolio was published
 * and flipping it did NOTHING (the owner's "couldn't make it public" dead
 * end). The fix binds the authoritative `isPublic` flag and persists it via
 * the dedicated PUT /me/profile/visibility endpoint, while profile saves send
 * text fields ONLY — a partial profile save must never touch visibility
 * (the silent-unpublish defect).
 */

const clearPortfolio = vi.hoisted(() => vi.fn());

vi.mock('@/stores/publicPortfolioStore.js', () => ({
  usePublicPortfolioStore: () => ({ clearPortfolio }),
}));

vi.mock('@/services/api/index.js', () => ({
  getMyAccount: vi.fn(),
  getMyPublicProfile: vi.fn(),
  updateMyPublicProfile: vi.fn(),
  updateMyProfileVisibility: vi.fn(),
  ApiError: class ApiError extends Error {
    httpStatus: number | undefined;
    constructor(message?: string, httpStatus?: number) {
      super(message);
      this.httpStatus = httpStatus;
    }
  },
}));

import {
  getMyAccount,
  getMyPublicProfile,
  updateMyPublicProfile,
  updateMyProfileVisibility,
} from '@/services/api/index.js';

const mountPage = async () => {
  const wrapper = mount(UserPortfolioProfilePage, {
    global: { stubs: ['LoadingModal', 'ErrorModal', 'SuccessModal', 'router-link'] },
  });
  await flushPromises();
  return wrapper;
};

describe('UserPortfolioProfilePage.vue (Phase 0.31 H1 visibility contract)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearPortfolio.mockClear();
    vi.mocked(getMyAccount).mockResolvedValue({
      firstName: 'Admin',
      lastName: 'User',
      profileImageUrl: '',
    } as never);
    vi.mocked(getMyPublicProfile).mockResolvedValue({
      headline: 'Headline',
      summary: 'Summary',
      isPublic: true,
    } as never);
    vi.mocked(updateMyPublicProfile).mockResolvedValue({} as never);
    vi.mocked(updateMyProfileVisibility).mockResolvedValue(undefined as never);
  });

  it('renders the visibility switch CHECKED when the portfolio is actually published', async () => {
    const wrapper = await mountPage();

    // Pre-fix the switch bound a never-present key and showed unchecked even
    // while isPublic=true — the "system disregards Portfolio is Public" lie.
    expect(wrapper.find('#publiclyVisible').element.checked).toBe(true);
  });

  it('renders the visibility switch UNCHECKED when the portfolio is private', async () => {
    vi.mocked(getMyPublicProfile).mockResolvedValue({
      headline: 'Headline',
      summary: 'Summary',
      isPublic: false,
    } as never);
    const wrapper = await mountPage();

    expect(wrapper.find('#publiclyVisible').element.checked).toBe(false);
  });

  it('persists a CHANGED switch through the dedicated visibility endpoint (and clears the cache)', async () => {
    const wrapper = await mountPage();

    await wrapper.find('#publiclyVisible').setValue(false);
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(updateMyProfileVisibility).toHaveBeenCalledWith({ isPublic: false });
    expect(clearPortfolio).toHaveBeenCalledTimes(1);
    expect(updateMyPublicProfile).toHaveBeenCalled();
  });

  it('does NOT call the visibility endpoint when the switch is untouched', async () => {
    const wrapper = await mountPage();

    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(updateMyProfileVisibility).not.toHaveBeenCalled();
    expect(clearPortfolio).not.toHaveBeenCalled();
    expect(updateMyPublicProfile).toHaveBeenCalledTimes(1);
  });

  it('profile saves send TEXT FIELDS ONLY — no visibility keys can ride along (silent-unpublish pin)', async () => {
    const wrapper = await mountPage();

    await wrapper.find('form').trigger('submit');
    await flushPromises();

    const payload = vi.mocked(updateMyPublicProfile).mock.calls[0][0] as Record<string, unknown>;
    // The exact keys that used to unpublish the portfolio must be absent.
    expect(payload).not.toHaveProperty('isPublic');
    expect(payload).not.toHaveProperty('publiclyVisible');
    expect(payload).not.toHaveProperty('visible');
    // Text fields still flow.
    expect(payload).toHaveProperty('headline', 'Headline');
    expect(payload).toHaveProperty('summary', 'Summary');
  });

  it('shows the success modal after a save with a visibility change', async () => {
    const wrapper = await mountPage();

    await wrapper.find('#publiclyVisible').setValue(false);
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(wrapper.findComponent({ name: 'SuccessModal' }).props('visible')).toBe(true);
  });

  it('re-seeds the switch from the server on reload (originallyPublic tracking)', async () => {
    const wrapper = await mountPage();

    // Change the switch, save (visibility endpoint called once)...
    await wrapper.find('#publiclyVisible').setValue(false);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(updateMyProfileVisibility).toHaveBeenCalledTimes(1);

    // ...saving AGAIN without touching the switch must not re-fire it: the
    // baseline moved with the successful save.
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(updateMyProfileVisibility).toHaveBeenCalledTimes(1);
  });
});
