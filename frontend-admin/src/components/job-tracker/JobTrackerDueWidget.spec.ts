import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import JobTrackerDueWidget from './JobTrackerDueWidget.vue';
import type { JobApplication, ApplicationStatus } from '@/types';

/**
 * Phase 0.32 (TASKS §I2): "Needs attention" dashboard widget contract —
 * surfaces deadlines/follow-ups/interviews/assessments that are due TODAY
 * or overdue, applies the noise rules (terminal statuses silent, past
 * interview dates only while still awaited, future dates silent), hides
 * itself when there is nothing to act on, and never breaks the dashboard
 * if the API fails.
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

vi.mock('@/services/authService', () => ({
  authService: { user: { value: { slug: 'admin-user' } } },
}));

import { jobApplicationService } from '@/services/jobApplicationService';

/** Local-timezone ISO date n days from today (avoids UTC slice off-by-one). */
const dayOffset = (n: number): string => {
  const d = new Date();
  d.setDate(d.getDate() + n);
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
};

const app = (overrides: Partial<JobApplication> & { uuid: string; status: ApplicationStatus }): JobApplication => ({
  jobTitle: 'Developer',
  companyName: 'Acme',
  createdAt: '2026-01-01T00:00:00',
  ...overrides,
} as JobApplication);

const mountWidget = async (list: JobApplication[]) => {
  vi.mocked(jobApplicationService.getAll).mockResolvedValue(list as never);
  const wrapper = mount(JobTrackerDueWidget, {
    global: { stubs: { 'router-link': true } },
  });
  await flushPromises();
  return wrapper;
};

describe('JobTrackerDueWidget.vue (Phase 0.32)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows an overdue deadline and a follow-up due today', async () => {
    const wrapper = await mountWidget([
      app({ uuid: 'a1', status: 'APPLIED', deadlineAt: dayOffset(-2) }),
      app({ uuid: 'a2', status: 'APPLIED', followUpAt: dayOffset(0) }),
    ]);

    const widget = wrapper.find('[data-testid="due-widget"]');
    expect(widget.exists()).toBe(true);
    expect(wrapper.find('[data-testid="due-count"]').text()).toBe('2');
    expect(wrapper.text()).toContain('Deadline 2d overdue');
    expect(wrapper.text()).toContain('Follow-up today');
  });

  it('stays silent for future dates and terminal statuses', async () => {
    const wrapper = await mountWidget([
      app({ uuid: 'a1', status: 'APPLIED', deadlineAt: dayOffset(5) }), // future
      app({ uuid: 'a2', status: 'REJECTED', deadlineAt: dayOffset(-9) }), // terminal
      app({ uuid: 'a3', status: 'WITHDRAWN', followUpAt: dayOffset(-1) }), // terminal
      app({ uuid: 'a4', status: 'ARCHIVED', deadlineAt: dayOffset(0) }), // terminal
    ]);

    expect(wrapper.find('[data-testid="due-widget"]').exists()).toBe(false);
  });

  it('alerts on a past interview date only while the interview is still awaited', async () => {
    const wrapper = await mountWidget([
      app({ uuid: 'a1', status: 'INTERVIEW_SCHEDULED', interviewAt: `${dayOffset(-1)}T10:00:00` }),
      app({ uuid: 'a2', status: 'INTERVIEW_COMPLETED', interviewAt: `${dayOffset(-3)}T10:00:00` }),
    ]);

    expect(wrapper.find('[data-testid="due-widget"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="due-count"]').text()).toBe('1');
    expect(wrapper.text()).toContain('Interview 1d overdue');
    expect(wrapper.text()).not.toContain('Interview Completed'); // only one alert row
  });

  it('sorts alerts soonest-first (most overdue at the top)', async () => {
    const wrapper = await mountWidget([
      app({ uuid: 'a1', status: 'APPLIED', deadlineAt: dayOffset(-1) }),
      app({ uuid: 'a2', status: 'APPLIED', deadlineAt: dayOffset(-5) }),
      app({ uuid: 'a3', status: 'APPLIED', deadlineAt: dayOffset(0) }),
    ]);

    const rows = wrapper.findAll('[data-testid^="due-item-"]').map(r => r.text());
    expect(rows).toHaveLength(3);
    expect(rows[0]).toContain('5d overdue'); // most overdue first
    expect(rows[1]).toContain('1d overdue');
    expect(rows[2]).toContain('today'); // due today last
  });

  it('hides itself entirely when nothing is due', async () => {
    const wrapper = await mountWidget([
      app({ uuid: 'a1', status: 'APPLIED' }), // no dates at all
      app({ uuid: 'a2', status: 'APPLIED', deadlineAt: dayOffset(10) }),
    ]);

    expect(wrapper.find('[data-testid="due-widget"]').exists()).toBe(false);
    expect(wrapper.html()).not.toContain('due-widget');
  });

  it('stays silent if the API fails (the dashboard must never break)', async () => {
    vi.mocked(jobApplicationService.getAll).mockRejectedValueOnce(new Error('offline') as never);
    const wrapper = mount(JobTrackerDueWidget, {
      global: { stubs: { 'router-link': true } },
    });
    await flushPromises();

    expect(wrapper.find('[data-testid="due-widget"]').exists()).toBe(false);
    expect(wrapper.find('.alert-danger').exists()).toBe(false);
  });

  it('links to the job-tracker route', async () => {
    // Real router (not a stub) so the resolved href is observable.
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/:slug/manage/job-tracker', name: 'job-tracker', component: { template: '<div />' } },
      ],
    });
    vi.mocked(jobApplicationService.getAll).mockResolvedValue([
      app({ uuid: 'a1', status: 'APPLIED', deadlineAt: dayOffset(0) }),
    ] as never);
    const wrapper = mount(JobTrackerDueWidget, { global: { plugins: [router] } });
    await flushPromises();
    await router.push('/admin-user/manage/job-tracker');
    await router.isReady();

    const link = wrapper.find('[data-testid="due-open-tracker"]');
    expect(link.exists()).toBe(true);
    expect(link.attributes('href')).toBe('/admin-user/manage/job-tracker');
  });
});
