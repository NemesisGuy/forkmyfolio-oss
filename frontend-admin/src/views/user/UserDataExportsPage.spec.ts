import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import UserDataExportsPage from './UserDataExportsPage.vue';

/**
 * Phase 0.28 (A1) — Data & Exports cover-letter tile regression tests.
 *
 * Contract under test:
 * - the Cover Letter tile exists ONLY when `export.coverletter.mode=SEPARATE`
 *   (PRV-1: the file is owner-only, so no other mode may advertise it);
 * - SEPARATE + letter text → a working Download button (the "separate
 *   documents" flow the owner reported as broken);
 * - SEPARATE + NO letter text → the tile explains itself and links to the
 *   editor instead of offering a button that can only ever 404;
 * - unreadable settings fail safe and hide the optional tile.
 */

vi.mock('@/services/api', () => ({
  settingsApi: { getAll: vi.fn(), update: vi.fn() },
  getMyPublicProfile: vi.fn(),
  downloadCoverLetterBySlug: vi.fn(),
  downloadMarkdownBySlug: vi.fn(),
  downloadPublicPortfolioBySlug: vi.fn(),
  downloadVCardBySlug: vi.fn(),
}));

vi.mock('@/services/authService', () => ({
  authService: { user: { value: { slug: 'admin-user' } } },
}));

vi.mock('@/services/notificationService', () => ({
  notificationService: { add: vi.fn() },
}));

vi.mock('@/utils/downloadUtils', () => ({
  getFilenameFromResponse: vi.fn(() => 'file.pdf'),
  triggerDownload: vi.fn(),
}));

import { settingsApi, getMyPublicProfile, downloadCoverLetterBySlug } from '@/services/api';
import { notificationService } from '@/services/notificationService';

const settingsWith = (mode: string) => ([
  { name: 'export.coverletter.mode', value: mode, uuid: 'uuid-mode', description: '' },
]);

const mountPage = async () => {
  const wrapper = mount(UserDataExportsPage, {
    global: {
      stubs: {
        LoadingModal: true,
        ErrorModal: true,
        // Render the slot: the default router-link stub renders no children,
        // which would hide the "Write my cover letter" link from assertions.
        'router-link': { template: '<a><slot /></a>' },
      },
    },
  });
  await flushPromises();
  return wrapper;
};

describe('UserDataExportsPage.vue (Phase 0.28 A1)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(downloadCoverLetterBySlug).mockResolvedValue(new Response('pdf', { status: 200 }) as never);
  });

  it('shows the Cover Letter tile with a working download when mode is SEPARATE', async () => {
    vi.mocked(settingsApi.getAll).mockResolvedValue(settingsWith('SEPARATE') as never);
    vi.mocked(getMyPublicProfile).mockResolvedValue({
      coverLetterTemplate: 'Dear Hiring Manager,',
    } as never);

    const wrapper = await mountPage();

    const titles = wrapper.findAll('h5').map((h) => h.text());
    expect(titles).toContain('Cover Letter');

    const coverBtn = wrapper
      .findAll('button')
      .find((b) => b.text().includes('Download PDF') && b.classes().includes('btn-warning'));
    expect(coverBtn).toBeDefined();
    expect(coverBtn!.attributes('disabled')).toBeUndefined();

    await coverBtn!.trigger('click');
    await flushPromises();

    expect(downloadCoverLetterBySlug).toHaveBeenCalledWith('admin-user');
    expect(notificationService.add).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'success' })
    );
  });

  it('SEPARATE with no letter text explains itself instead of offering a dead button', async () => {
    vi.mocked(settingsApi.getAll).mockResolvedValue(settingsWith('SEPARATE') as never);
    vi.mocked(getMyPublicProfile).mockResolvedValue({
      coverLetterTemplate: '   ',
    } as never);

    const wrapper = await mountPage();

    const titles = wrapper.findAll('h5').map((h) => h.text());
    expect(titles).toContain('Cover Letter');
    expect(wrapper.text()).toContain('you have not written one yet');

    const coverBtn = wrapper
      .findAll('button')
      .find((b) => b.text().includes('Download PDF') && b.classes().includes('btn-warning'));
    expect(coverBtn).toBeUndefined();

    const writeLink = wrapper.findAll('a, router-link, router-link-stub')
      .find((n) => n.text().includes('Write my cover letter'));
    expect(writeLink, 'editor link rendered').toBeDefined();
    expect(downloadCoverLetterBySlug).not.toHaveBeenCalled();
  });

  it('hides the Cover Letter tile entirely in any other mode', async () => {
    for (const mode of ['OFF', 'IN_PDF']) {
      vi.mocked(settingsApi.getAll).mockResolvedValue(settingsWith(mode) as never);
      const wrapper = await mountPage();

      const titles = wrapper.findAll('h5').map((h) => h.text());
      expect(titles).not.toContain('Cover Letter');
      // The three core tiles are always present
      expect(titles).toContain('PDF Portfolio');
      expect(titles).toContain('Markdown File');
      expect(titles).toContain('vCard Contact');
      wrapper.unmount();
    }
  });

  it('fails safe and hides the tile when settings cannot be read', async () => {
    vi.mocked(settingsApi.getAll).mockRejectedValue(new Error('offline') as never);

    const wrapper = await mountPage();

    const titles = wrapper.findAll('h5').map((h) => h.text());
    expect(titles).not.toContain('Cover Letter');
    expect(titles).toContain('PDF Portfolio');
  });

  it('keeps the download button when the profile lookup fails (fail open on the button)', async () => {
    vi.mocked(settingsApi.getAll).mockResolvedValue(settingsWith('SEPARATE') as never);
    vi.mocked(getMyPublicProfile).mockRejectedValue(new Error('boom') as never);

    const wrapper = await mountPage();

    const coverBtn = wrapper
      .findAll('button')
      .find((b) => b.text().includes('Download PDF') && b.classes().includes('btn-warning'));
    expect(coverBtn).toBeDefined();
    expect(coverBtn!.attributes('disabled')).toBeUndefined();
  });
});
