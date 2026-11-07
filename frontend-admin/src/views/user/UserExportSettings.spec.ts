import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import UserExportSettings from './UserExportSettings.vue';

/**
 * Phase 8.3 — Export Settings CMS regression tests.
 *
 * Pins the owner-facing contract of the export preferences page:
 * - loads persisted values from GET /me/settings (the existing mechanism);
 * - users who never configured anything see the safe defaults
 *   (FULL mode, every section enabled);
 * - mode + section toggles are editable and gate the save button;
 * - saving sends ONLY changed uuid+value pairs to the existing bulk PUT
 *   (unrelated UserSetting values are never touched) and re-syncs from the
 *   authoritative refreshed list;
 * - API failures surface the error modal and never fake success.
 */

vi.mock('@/services/api/user.api.js', () => ({
  settingsApi: {
    getAll: vi.fn(),
    update: vi.fn(),
  },
}));

vi.mock('@/services/settingsService.js', () => ({
  settingsService: { updateSettings: vi.fn() },
}));

import { settingsApi } from '@/services/api/user.api.js';
import { settingsService } from '@/services/settingsService.js';

/** Builds a GET /me/settings payload with the seeded export.* settings. */
const serverSettings = (overrides: Record<string, string> = {}) => {
  const base: Record<string, string> = {
    'export.mode': 'FULL',
    'export.section.summary': 'true',
    'export.section.experience': 'true',
    'export.section.projects': 'true',
    'export.section.skills': 'true',
    'export.section.qualifications': 'true',
    'export.section.testimonials': 'true',
    'export.section.contact': 'true',
    ...overrides,
  };
  return Object.entries(base).map(([name, value], i) => ({
    name,
    value,
    uuid: `uuid-${i}-${name}`,
    description: `${name} description`,
  }));
};

const mountPage = async () => {
  const wrapper = mount(UserExportSettings, {
    global: { stubs: ['SuccessModal', 'ErrorModal', 'LoadingModal'] },
  });
  // onMounted waits a deliberate 500ms "minDelay" before releasing the
  // loading gate — advance the fake clock through it.
  await flushPromises();
  await vi.advanceTimersByTimeAsync(600);
  await flushPromises();
  return wrapper;
};

describe('UserExportSettings.vue (Phase 8.3)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    vi.mocked(settingsApi.getAll).mockResolvedValue(serverSettings() as never);
    vi.mocked(settingsApi.update).mockResolvedValue(serverSettings() as never);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('loads persisted values from the server (mode + toggles)', async () => {
    vi.mocked(settingsApi.getAll).mockResolvedValue(serverSettings({
      'export.mode': 'BRIEF',
      'export.section.projects': 'false',
    }) as never);
    const wrapper = await mountPage();

    expect(wrapper.find('#mode-brief').element.checked).toBe(true);
    expect(wrapper.find('#mode-full').element.checked).toBe(false);
    expect(wrapper.find('#export-switch-projects').element.checked).toBe(false);
    expect(wrapper.find('#export-switch-experience').element.checked).toBe(true);
  });

  it('shows the safe defaults for a user with no export preferences', async () => {
    // A brand-new user: the backend seeds the globals, so GET returns the
    // default values (FULL + all true) even with no UserSetting overrides.
    const wrapper = await mountPage();

    expect(wrapper.find('#mode-full').element.checked).toBe(true);
    for (const key of ['summary', 'experience', 'projects', 'skills', 'qualifications', 'testimonials', 'contact']) {
      expect(wrapper.find(`#export-switch-${key}`).element.checked,
        `section ${key} defaults to enabled`).toBe(true);
    }
    // Nothing changed yet → save disabled
    expect(wrapper.find('form button[type="submit"]').element.disabled).toBe(true);
  });

  it('selecting a mode enables saving and sends only the mode change', async () => {
    const refreshed = serverSettings({ 'export.mode': 'BRIEF' });
    vi.mocked(settingsApi.update).mockResolvedValue(refreshed as never);
    const wrapper = await mountPage();

    await wrapper.find('#mode-brief').setValue();
    expect(wrapper.find('form button[type="submit"]').element.disabled).toBe(false);

    await wrapper.find('form button[type="submit"]').trigger('submit');
    await flushPromises();

    expect(settingsApi.update).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(settingsApi.update).mock.calls[0][0] as { uuid: string; value: string }[];
    expect(payload).toEqual([{ uuid: 'uuid-0-export.mode', value: 'BRIEF' }]);
    // The central settings cache receives the FULL authoritative response
    expect(settingsService.updateSettings).toHaveBeenCalledWith(refreshed);
  });

  it('sends only the toggled sections and keeps unrelated settings untouched', async () => {
    const refreshed = serverSettings({ 'export.section.projects': 'false', 'export.section.contact': 'false' });
    vi.mocked(settingsApi.update).mockResolvedValue(refreshed as never);
    const wrapper = await mountPage();

    await wrapper.find('#export-switch-projects').setValue(false);
    await wrapper.find('#export-switch-contact').setValue(false);
    await wrapper.find('form button[type="submit"]').trigger('submit');
    await flushPromises();

    const payload = vi.mocked(settingsApi.update).mock.calls[0][0] as { uuid: string; value: string }[];
    // ONLY the two changed sections — never a bulk overwrite of everything
    expect(payload).toHaveLength(2);
    expect(payload).toContainEqual({ uuid: 'uuid-3-export.section.projects', value: 'false' });
    expect(payload).toContainEqual({ uuid: 'uuid-7-export.section.contact', value: 'false' });
  });

  it('reset restores the loaded values and re-disables saving', async () => {
    const wrapper = await mountPage();

    await wrapper.find('#mode-brief').setValue();
    await wrapper.find('#export-switch-projects').setValue(false);
    expect(wrapper.find('form button[type="submit"]').element.disabled).toBe(false);

    await wrapper.find('button[type="button"].btn-secondary').trigger('click');

    expect(wrapper.find('#mode-full').element.checked).toBe(true);
    expect(wrapper.find('#export-switch-projects').element.checked).toBe(true);
    expect(wrapper.find('form button[type="submit"]').element.disabled).toBe(true);
    expect(settingsApi.update).not.toHaveBeenCalled();
  });

  it('surfaces API failures in the error modal and never fakes success', async () => {
    vi.mocked(settingsApi.update).mockRejectedValueOnce(new Error('500 boom') as never);
    const wrapper = await mountPage();

    await wrapper.find('#mode-brief').setValue();
    await wrapper.find('form button[type="submit"]').trigger('submit');
    await flushPromises();

    // ErrorModal stub receives truthy visible + message
    const errorModal = wrapper.findComponent({ name: 'ErrorModal' });
    expect(errorModal.props('visible')).toBe(true);
    expect(String(errorModal.props('message'))).toContain('500 boom');
    // SuccessModal never opened
    const successModal = wrapper.findComponent({ name: 'SuccessModal' });
    expect(successModal.props('visible')).toBe(false);
  });

  it('load failure renders the error alert instead of the form', async () => {
    vi.mocked(settingsApi.getAll).mockRejectedValueOnce(new Error('offline') as never);
    const wrapper = await mountPage();

    expect(wrapper.find('.alert-danger').exists()).toBe(true);
    expect(wrapper.find('form').exists()).toBe(false);
  });

  it('blocks saving when the backend has no export.mode setting (defensive)', async () => {
    // Backend without seeded export settings: the page still renders defaults
    // but must refuse to save rather than send a payload with no uuid.
    vi.mocked(settingsApi.getAll).mockResolvedValue([
      { name: 'portfolio.pdf.template', value: 'modern', uuid: 'uuid-pdf', description: '' },
    ] as never);
    const wrapper = await mountPage();

    expect(wrapper.find('#mode-full').element.checked).toBe(true); // defaults shown

    await wrapper.find('#mode-brief').setValue();
    await wrapper.find('form button[type="submit"]').trigger('submit');
    await flushPromises();

    expect(settingsApi.update).not.toHaveBeenCalled();
    const errorModal = wrapper.findComponent({ name: 'ErrorModal' });
    expect(errorModal.props('visible')).toBe(true);
  });
});
