import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import JobApplicationModal from './JobApplicationModal.vue';
import type { JobApplication } from '@/types';

/**
 * Phase 8.4 — JobApplicationModal regression tests.
 *
 * Pins the backend payload contract:
 * - Create sends the TRK-400 keys (jobTitle/jobLocation) and NO status field
 *   (the backend defaults new applications to DRAFT).
 * - Edit sends status + an explicit `visible` boolean, and omits blank
 *   optional fields (undefined, not "") so the backend's "null = no change"
 *   contract is respected.
 * - All 15 statuses (13 lifecycle + 2 legacy) are offered in the edit select.
 */

const baseApp = (overrides: Partial<JobApplication> = {}): JobApplication => ({
  uuid: 'app-1',
  jobTitle: 'Developer',
  companyName: 'Acme',
  status: 'DRAFT',
  visible: true,
  createdAt: '2026-01-01T00:00:00',
  ...overrides,
} as JobApplication);

const mountModal = async (application: JobApplication | null = null) => {
  const wrapper = mount(JobApplicationModal, {
    props: { modelValue: true, application },
  });
  await flushPromises();
  return wrapper;
};

describe('JobApplicationModal.vue (Phase 8.4)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('create mode: emits the TRK-400 contract keys with no status field and omits blanks', async () => {
    const wrapper = await mountModal(null);

    await wrapper.find('#title').setValue('Senior Dev');
    await wrapper.find('#company').setValue('NemesisNet');
    await wrapper.find('#location').setValue('Johannesburg');
    // Leave every other optional field blank
    await wrapper.find('button.btn-primary').trigger('click');

    const emitted = wrapper.emitted('save');
    expect(emitted).toHaveLength(1);
    const payload = emitted![0][0] as Record<string, unknown>;

    // TRK-400 contract keys
    expect(payload.jobTitle).toBe('Senior Dev');
    expect(payload.companyName).toBe('NemesisNet');
    expect(payload.jobLocation).toBe('Johannesburg');
    // Create contract has NO status field — the backend defaults to DRAFT
    expect(payload).not.toHaveProperty('status');
    expect(payload).not.toHaveProperty('visible');
    // Blank optional fields are omitted (undefined — dropped by JSON
    // serialization), never sent as empty strings
    expect(payload.jobType).toBeUndefined();
    expect(payload.applicationUrl).toBeUndefined();
    expect(payload.deadlineAt).toBeUndefined();
    expect(payload.salaryRange).toBeUndefined();
    expect(payload.initialNote).toBeUndefined();
  });

  it('create mode: includes filled optional fields with correct backend names', async () => {
    const wrapper = await mountModal(null);

    await wrapper.find('#title').setValue('Dev');
    await wrapper.find('#company').setValue('Acme');
    await wrapper.find('#job-type').setValue('Contract');
    await wrapper.find('#application-url').setValue('https://jobs.example.com/1');
    await wrapper.find('#deadline-at').setValue('2026-12-01');
    await wrapper.find('#recruiter-email').setValue('jane@acme.com');
    await wrapper.find('#initialNote').setValue('Found via referral');

    await wrapper.find('button.btn-primary').trigger('click');

    const payload = wrapper.emitted('save')![0][0] as Record<string, unknown>;
    expect(payload.jobType).toBe('Contract');
    expect(payload.applicationUrl).toBe('https://jobs.example.com/1');
    expect(payload.deadlineAt).toBe('2026-12-01');
    expect(payload.recruiterEmail).toBe('jane@acme.com');
    expect(payload.initialNote).toBe('Found via referral');
  });

  it('edit mode: emits status, explicit visible, and omits blank fields', async () => {
    const wrapper = await mountModal(baseApp({
      status: 'APPLIED',
      visible: false,
      jobLocation: 'Remote',
      appliedAt: '2026-02-01T09:00:00',
    }));

    await wrapper.find('#status').setValue('OFFER');
    await wrapper.find('#visible-switch').setChecked(true);
    await wrapper.find('#title').setValue('Updated Title');
    // Leave jobType blank — it was never set, so it must be omitted
    await wrapper.find('button.btn-primary').trigger('click');

    const payload = wrapper.emitted('save')![0][0] as Record<string, unknown>;
    expect(payload.jobTitle).toBe('Updated Title');
    expect(payload.status).toBe('OFFER');
    expect(payload.visible).toBe(true);
    expect(payload.jobLocation).toBe('Remote');
    expect(payload.jobType).toBeUndefined();
    expect(payload.initialNote).toBeUndefined();
  });

  it('edit mode: offers all 13 lifecycle statuses plus the 2 legacy values', async () => {
    const wrapper = await mountModal(baseApp());

    const options = wrapper.findAll('#status option').map(o => o.element.value);
    for (const status of [
      'INTERESTED', 'PREPARING', 'DRAFT', 'APPLIED', 'ACKNOWLEDGED',
      'INTERVIEW_SCHEDULED', 'INTERVIEW_COMPLETED', 'TECHNICAL_ASSESSMENT',
      'OFFER', 'REJECTED', 'WITHDRAWN', 'NO_RESPONSE', 'ARCHIVED',
    ]) {
      expect(options).toContain(status);
    }
    // Legacy values remain selectable so existing records keep working
    expect(options).toContain('INTERVIEWING');
    expect(options).toContain('ACCEPTED');
    expect(options).toHaveLength(15);
  });

  it('create mode: does not render the status or visibility controls', async () => {
    const wrapper = await mountModal(null);

    expect(wrapper.find('#status').exists()).toBe(false);
    expect(wrapper.find('#visible-switch').exists()).toBe(false);
  });

  it('does not emit save when required fields are missing', async () => {
    const wrapper = await mountModal(null);

    await wrapper.find('#company').setValue('Acme only');
    await wrapper.find('button.btn-primary').trigger('click');

    expect(wrapper.emitted('save')).toBeUndefined();
  });

  it('edit mode: populates form fields from the application prop', async () => {
    const wrapper = await mountModal(baseApp({
      jobTitle: 'Existing Role',
      companyName: 'Existing Co',
      jobLocation: 'Cape Town',
      salaryRange: 'R45k',
      status: 'INTERVIEW_SCHEDULED',
    }));

    expect((wrapper.find('#title').element as HTMLInputElement).value).toBe('Existing Role');
    expect((wrapper.find('#company').element as HTMLInputElement).value).toBe('Existing Co');
    expect((wrapper.find('#location').element as HTMLInputElement).value).toBe('Cape Town');
    expect((wrapper.find('#salary-range').element as HTMLInputElement).value).toBe('R45k');
    expect((wrapper.find('#status').element as HTMLSelectElement).value).toBe('INTERVIEW_SCHEDULED');
  });
});
