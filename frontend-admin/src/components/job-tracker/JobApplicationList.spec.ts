import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import JobApplicationList from './JobApplicationList.vue';
import type { JobApplication } from '@/types';

/**
 * Phase 8.4 — Job Application management page regression tests.
 *
 * Pins the management-page contract: real filtering/search/sort over the
 * actual API data, all 13 lifecycle statuses (+2 legacy) render, archived
 * applications stay accessible but hidden by default, and loading/empty/
 * error states behave.
 */

vi.mock('@/services/jobApplicationService', () => ({
  jobApplicationService: {
    getAll: vi.fn(),
    getByUuid: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    addNote: vi.fn(),
    deleteNote: vi.fn(),
  },
}));

vi.mock('@/utils/jobApplicationsCsv', () => ({
  buildJobApplicationsCsv: vi.fn(() => ''),
  downloadJobApplicationsCsv: vi.fn(),
}));

import { jobApplicationService } from '@/services/jobApplicationService';
import { downloadJobApplicationsCsv } from '@/utils/jobApplicationsCsv';

const app = (overrides: Partial<JobApplication> & { uuid: string }): JobApplication => ({
  jobTitle: 'Developer',
  companyName: 'Acme',
  status: 'DRAFT',
  createdAt: '2026-01-01T00:00:00',
  ...overrides,
} as JobApplication);

const richList = (): JobApplication[] => [
  app({ uuid: 'a1', jobTitle: 'Backend Engineer', companyName: 'Alpha Corp', status: 'INTERESTED', jobType: 'Contract', workArrangement: 'REMOTE', discoveredAt: '2026-01-05' }),
  app({ uuid: 'a2', jobTitle: 'Frontend Engineer', companyName: 'Beta Ltd', status: 'APPLIED', appliedAt: '2026-02-01', deadlineAt: '2026-03-01' }),
  app({ uuid: 'a3', jobTitle: 'DevOps Engineer', companyName: 'Gamma Inc', status: 'INTERVIEW_SCHEDULED', interviewAt: '2026-03-10T10:00:00', workArrangement: 'HYBRID' }),
  app({ uuid: 'a4', jobTitle: 'Archived Role', companyName: 'Delta LLC', status: 'ARCHIVED' }),
  app({ uuid: 'a5', jobTitle: 'Platform Engineer', companyName: 'Alpha Corp', status: 'OFFER', salaryRange: 'R50k', salaryCurrency: 'ZAR', salaryPeriod: 'MONTHLY' }),
];

const mountPage = async (list: JobApplication[] = richList()) => {
  vi.mocked(jobApplicationService.getAll).mockResolvedValue(list as never);
  const wrapper = mount(JobApplicationList, {
    global: { stubs: ['JobApplicationModal', 'JobApplicationDetailsModal'] },
  });
  await flushPromises();
  return wrapper;
};

describe('JobApplicationList.vue (Phase 8.4)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all cards with STATUS_META labels for every lifecycle status', async () => {
    const wrapper = await mountPage();

    const cards = wrapper.findAll('[data-testid="card-status"]');
    expect(cards).toHaveLength(4); // ARCHIVED hidden by default
    const labels = cards.map(c => c.text());
    expect(labels).toContain('Interested');
    expect(labels).toContain('Applied');
    expect(labels).toContain('Interview Scheduled');
    expect(labels).toContain('Offer');
  });

  it('hides archived applications by default and reveals them via the toggle', async () => {
    const wrapper = await mountPage();

    expect(wrapper.text()).not.toContain('Archived Role');

    await wrapper.find('#show-archived').setValue(true);
    expect(wrapper.text()).toContain('Archived Role');
  });

  it('search filters by company and job title', async () => {
    const wrapper = await mountPage();

    await wrapper.find('#tracker-search').setValue('alpha');
    let titles = wrapper.findAll('.card-title').map(c => c.text());
    expect(titles).toEqual(['Backend Engineer', 'Platform Engineer']);

    await wrapper.find('#tracker-search').setValue('frontend');
    titles = wrapper.findAll('.card-title').map(c => c.text());
    expect(titles).toEqual(['Frontend Engineer']);
  });

  it('status filter shows only matching applications', async () => {
    const wrapper = await mountPage();

    await wrapper.find('#filter-status').setValue('OFFER');
    const titles = wrapper.findAll('.card-title').map(c => c.text());
    expect(titles).toEqual(['Platform Engineer']);
  });

  it('employment-type and work-arrangement filters work', async () => {
    const wrapper = await mountPage();

    await wrapper.find('#filter-type').setValue('Contract');
    expect(wrapper.findAll('.card-title').map(c => c.text())).toEqual(['Backend Engineer']);

    await wrapper.find('#filter-type').setValue('ALL');
    await wrapper.find('#filter-arrangement').setValue('HYBRID');
    expect(wrapper.findAll('.card-title').map(c => c.text())).toEqual(['DevOps Engineer']);
  });

  it('sorts by deadline with soonest first and missing values last', async () => {
    const wrapper = await mountPage([
      app({ uuid: 's1', jobTitle: 'No Deadline', companyName: 'A', status: 'APPLIED' }),
      app({ uuid: 's2', jobTitle: 'Late Deadline', companyName: 'B', status: 'APPLIED', deadlineAt: '2026-12-01' }),
      app({ uuid: 's3', jobTitle: 'Soon Deadline', companyName: 'C', status: 'APPLIED', deadlineAt: '2026-01-10' }),
    ]);

    await wrapper.find('#sort-by').setValue('deadlineAt');
    const titles = wrapper.findAll('.card-title').map(c => c.text());
    expect(titles).toEqual(['Soon Deadline', 'Late Deadline', 'No Deadline']);
  });

  it('shows deadline and follow-up indicators on cards', async () => {
    const soon = new Date();
    soon.setDate(soon.getDate() + 3);
    const wrapper = await mountPage([
      app({ uuid: 'd1', jobTitle: 'Urgent', companyName: 'X', status: 'APPLIED', deadlineAt: soon.toISOString().slice(0, 10) }),
    ]);

    const indicator = wrapper.find('[data-testid="deadline-indicator"]');
    expect(indicator.exists()).toBe(true);
    expect(indicator.text()).toContain('Deadline in 3d');
  });

  it('distinguishes "nothing tracked yet" from "no matches" empty states', async () => {
    const emptyWrapper = await mountPage([]);
    expect(emptyWrapper.text()).toContain('No applications tracked yet');

    const noMatchWrapper = await mountPage();
    await noMatchWrapper.find('#tracker-search').setValue('zzz-no-match');
    expect(noMatchWrapper.text()).toContain('No applications match your search or filters.');
  });

  it('surfaces API failures as an error state instead of a blank page', async () => {
    vi.mocked(jobApplicationService.getAll).mockRejectedValueOnce(new Error('offline') as never);
    const wrapper = mount(JobApplicationList, {
      global: { stubs: ['JobApplicationModal', 'JobApplicationDetailsModal'] },
    });
    await flushPromises();

    expect(wrapper.find('.alert-danger').exists()).toBe(true);
    expect(wrapper.text()).toContain('Could not load your job applications');
  });

  it('status summary chips show per-status counts and toggle the filter', async () => {
    const wrapper = await mountPage();

    const summary = wrapper.find('[data-testid="status-summary"]');
    expect(summary.exists()).toBe(true);
    expect(summary.text()).toContain('Interested');

    // Clicking the INTERESTED chip filters the list to that status
    const chip = summary.findAll('button').find(b => b.text().includes('Interested'))!;
    await chip.trigger('click');
    expect(wrapper.findAll('.card-title').map(c => c.text())).toEqual(['Backend Engineer']);
  });

  // --- Phase 0.32 (TASKS §I2): view toggle, board DnD, CSV export ---

  it('view toggle switches between grid and board', async () => {
    const wrapper = await mountPage();

    expect(wrapper.find('[data-testid="job-board"]').exists()).toBe(false);
    expect(wrapper.findAll('[data-testid="card-status"]')).toHaveLength(4); // archived hidden

    await wrapper.find('[data-testid="view-board"]').trigger('click');
    expect(wrapper.find('[data-testid="job-board"]').exists()).toBe(true);
    expect(wrapper.findAll('[data-testid="board-card"]')).toHaveLength(4);
    expect(wrapper.findAll('[data-testid="card-status"]')).toHaveLength(0); // grid gone

    await wrapper.find('[data-testid="view-grid"]').trigger('click');
    expect(wrapper.find('[data-testid="job-board"]').exists()).toBe(false);
    expect(wrapper.findAll('[data-testid="card-status"]')).toHaveLength(4);
  });

  it('board drag-and-drop updates status optimistically and calls the API', async () => {
    vi.mocked(jobApplicationService.update).mockResolvedValue({} as never);
    const wrapper = await mountPage();
    await wrapper.find('[data-testid="view-board"]').trigger('click');

    // a1 (INTERESTED) is the only Wishlist card in richList
    const card = wrapper.find('[data-testid="column-body-wishlist"]')
      .findAll('[data-testid="board-card"]')[0];
    await card.trigger('dragstart', { dataTransfer: { setData: vi.fn(), effectAllowed: 'move' } });
    await wrapper.find('[data-testid="board-column-offer"]').trigger('drop');
    await flushPromises();

    expect(jobApplicationService.update).toHaveBeenCalledWith('a1', { status: 'OFFER' });
    // Optimistic move: a1 now sits beside a5 in the Offer column
    const offerCards = wrapper.find('[data-testid="column-body-offer"]')
      .findAll('[data-testid="board-card"]');
    expect(offerCards).toHaveLength(2);
    expect(wrapper.find('[data-testid="board-error"]').exists()).toBe(false);
  });

  it('board drop failure reverts the card and shows a non-destructive banner', async () => {
    vi.mocked(jobApplicationService.update).mockRejectedValueOnce(new Error('offline') as never);
    const wrapper = await mountPage();
    await wrapper.find('[data-testid="view-board"]').trigger('click');

    const card = wrapper.find('[data-testid="column-body-wishlist"]')
      .findAll('[data-testid="board-card"]')[0];
    await card.trigger('dragstart', { dataTransfer: { setData: vi.fn(), effectAllowed: 'move' } });
    await wrapper.find('[data-testid="board-column-offer"]').trigger('drop');
    await flushPromises();

    expect(wrapper.find('[data-testid="board-error"]').exists()).toBe(true);
    // Reverted: back in Wishlist, Offer column untouched
    expect(wrapper.find('[data-testid="column-body-wishlist"]')
      .findAll('[data-testid="board-card"]')).toHaveLength(1);
    expect(wrapper.find('[data-testid="column-body-offer"]')
      .findAll('[data-testid="board-card"]')).toHaveLength(1);
    // The page itself never blanks out
    expect(wrapper.find('.alert-danger').exists()).toBe(false);
  });

  it('Export CSV hands the CURRENT filtered selection to the CSV builder', async () => {
    const wrapper = await mountPage();
    await wrapper.find('#tracker-search').setValue('alpha');

    await wrapper.find('[data-testid="export-csv"]').trigger('click');

    expect(downloadJobApplicationsCsv).toHaveBeenCalledTimes(1);
    const passed = vi.mocked(downloadJobApplicationsCsv).mock.calls[0][0];
    expect(passed.map(a => a.uuid)).toEqual(['a1', 'a5']);
  });

  it('Export CSV is disabled when the selection is empty', async () => {
    const wrapper = await mountPage();
    await wrapper.find('#tracker-search').setValue('zzz-no-match');

    const btn = wrapper.find('[data-testid="export-csv"]');
    expect(btn.attributes('disabled')).toBeDefined();
    await btn.trigger('click');
    expect(downloadJobApplicationsCsv).not.toHaveBeenCalled();
  });

  // --- KNOWN_ISSUES §17: light-mode washout (cosmetic-polish batch) ---

  it('header, chips, toggle and empty states render theme-safe (KNOWN_ISSUES §17)', async () => {
    const wrapper = await mountPage();

    // No dark-background-only utilities anywhere in the rendered page — in
    // LIGHT mode they render white-on-light and vanish (phase032-02 evidence).
    const html = wrapper.html();
    expect(html).not.toContain('btn-outline-light');
    expect(html).not.toMatch(/text-white/);
    expect(html).not.toContain('text-light');

    // The page heading inherits the theme-aware body colour instead
    expect(wrapper.find('h2').classes()).not.toContain('text-white');

    // Inactive status chips + the board toggle use the house outline
    const summary = wrapper.find('[data-testid="status-summary"]');
    for (const chip of summary.findAll('button')) {
      expect(chip.classes()).toContain('btn-outline-secondary');
    }
    expect(wrapper.find('[data-testid="view-board"]').classes())
      .toContain('btn-outline-secondary');
    // Active states keep the readable solid style
    expect(wrapper.find('[data-testid="view-grid"]').classes()).toContain('btn-light');
  });

  it('both empty states use the semantic secondary colour', async () => {
    const emptyWrapper = await mountPage([]);
    expect(emptyWrapper.find('.text-body-secondary').exists()).toBe(true);
    expect(emptyWrapper.html()).not.toMatch(/text-white/);

    const noMatchWrapper = await mountPage();
    await noMatchWrapper.find('#tracker-search').setValue('zzz-no-match');
    expect(noMatchWrapper.find('.text-body-secondary').exists()).toBe(true);
    expect(noMatchWrapper.html()).not.toMatch(/text-white/);
  });

  it('loading spinner is theme-visible (inherits body colour, no text-light)', async () => {
    vi.mocked(jobApplicationService.getAll)
      .mockReturnValue(new Promise(() => {}) as never); // stays pending → loading state
    const wrapper = mount(JobApplicationList, {
      global: { stubs: ['JobApplicationModal', 'JobApplicationDetailsModal'] },
    });
    await flushPromises(); // flush the re-render; the fetch itself never settles

    const spinner = wrapper.find('.spinner-border');
    expect(spinner.exists()).toBe(true);
    expect(spinner.classes()).not.toContain('text-light');
  });
});
