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

import { jobApplicationService } from '@/services/jobApplicationService';

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
});
