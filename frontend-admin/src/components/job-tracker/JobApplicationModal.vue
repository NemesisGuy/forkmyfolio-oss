<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { JobApplication, ApplicationStatus, CreateJobApplicationRequest, UpdateJobApplicationRequest } from '@/types';
import { APPLICATION_STATUS_ORDER, STATUS_META } from '@/types';

const props = defineProps<{
  modelValue: boolean; // Controls visibility
  application?: JobApplication | null; // If present, we are editing
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'save', data: CreateJobApplicationRequest | UpdateJobApplicationRequest): void;
}>();

// TRK-400 regression (2026-11-04): the form/payload keys are the BACKEND's
// contract (jobTitle/jobLocation). The previous `title`/`location` keys made
// the backend's `@NotBlank` on jobTitle fail with `POST /me/job-applications
// 400 "Job title cannot be blank"` even when the user had filled the field.
const form = ref({
  jobTitle: '',
  companyName: '',
  jobLocation: '',
  jobType: '',
  applicationUrl: '',
  resumeVersion: '',
  jobDescription: '',
  workArrangement: '',
  discoveredAt: '',
  deadlineAt: '',
  followUpAt: '',
  interviewAt: '',
  assessmentAt: '',
  recruiterName: '',
  recruiterEmail: '',
  contactDetails: '',
  salaryRange: '',
  salaryCurrency: '',
  salaryPeriod: '',
  status: 'DRAFT' as ApplicationStatus,
  visible: false,
  initialNote: '' // Only for creation
});

const isEditing = computed(() => !!props.application);

const statusOptions = APPLICATION_STATUS_ORDER.map(status => ({
  value: status,
  // STATUS_META labels already carry the "(legacy)" suffix for the two
  // legacy values — don't append it twice.
  label: STATUS_META[status].label
}));

const employmentTypes = ['Full-Time', 'Part-Time', 'Contract', 'Temporary', 'Internship', 'Freelance'];
const workArrangements = [
  { value: 'REMOTE', label: 'Remote' },
  { value: 'HYBRID', label: 'Hybrid' },
  { value: 'ON_SITE', label: 'On-site' }
];
const salaryPeriods = ['HOURLY', 'MONTHLY', 'ANNUALLY'];

// `immediate: true` so a modal mounted with modelValue already true (e.g.
// remount after an error) still populates; the guard skips the false case.
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    if (props.application) {
      // Edit mode: populate form (dates as yyyy-mm-dd / datetime-local)
      const app = props.application;
      form.value = {
        jobTitle: app.jobTitle,
        companyName: app.companyName,
        jobLocation: app.jobLocation || '',
        jobType: app.jobType || '',
        applicationUrl: app.applicationUrl || '',
        resumeVersion: app.resumeVersion || '',
        jobDescription: app.jobDescription || '',
        workArrangement: app.workArrangement || '',
        discoveredAt: (app.discoveredAt || '').slice(0, 10),
        deadlineAt: (app.deadlineAt || '').slice(0, 10),
        followUpAt: (app.followUpAt || '').slice(0, 10),
        interviewAt: (app.interviewAt || '').slice(0, 16),
        assessmentAt: (app.assessmentAt || '').slice(0, 10),
        recruiterName: app.recruiterName || '',
        recruiterEmail: app.recruiterEmail || '',
        contactDetails: app.contactDetails || '',
        salaryRange: app.salaryRange || '',
        salaryCurrency: app.salaryCurrency || '',
        salaryPeriod: app.salaryPeriod || '',
        status: app.status,
        visible: app.visible ?? false,
        initialNote: '' // Notes are managed in the details view
      };
    } else {
      // Create mode: reset form (backend defaults status to DRAFT)
      form.value = {
        jobTitle: '',
        companyName: '',
        jobLocation: '',
        jobType: '',
        applicationUrl: '',
        resumeVersion: '',
        jobDescription: '',
        workArrangement: '',
        discoveredAt: '',
        deadlineAt: '',
        followUpAt: '',
        interviewAt: '',
        assessmentAt: '',
        recruiterName: '',
        recruiterEmail: '',
        contactDetails: '',
        salaryRange: '',
        salaryCurrency: '',
        salaryPeriod: '',
        status: 'DRAFT',
        visible: false,
        initialNote: ''
      };
    }
  }
}, { immediate: true });

const closeModal = () => {
  emit('update:modelValue', false);
};

/**
 * Phase 8.4: omitted-vs-empty handling. Blank optional fields are OMITTED
 * from the payload (undefined) rather than sent as empty strings, so the
 * backend's "null = no change" contract is respected on edits and no
 * spurious values are stored on create.
 */
const omitBlank = (value: string) => (value.trim() === '' ? undefined : value.trim());

const save = () => {
  if (!form.value.jobTitle || !form.value.companyName) {
    return; // required-field validation
  }

  if (isEditing.value) {
    const payload = {
      jobTitle: form.value.jobTitle,
      companyName: form.value.companyName,
      jobLocation: omitBlank(form.value.jobLocation),
      jobType: omitBlank(form.value.jobType),
      applicationUrl: omitBlank(form.value.applicationUrl),
      resumeVersion: omitBlank(form.value.resumeVersion),
      jobDescription: omitBlank(form.value.jobDescription),
      workArrangement: omitBlank(form.value.workArrangement),
      discoveredAt: omitBlank(form.value.discoveredAt),
      deadlineAt: omitBlank(form.value.deadlineAt),
      followUpAt: omitBlank(form.value.followUpAt),
      interviewAt: omitBlank(form.value.interviewAt),
      assessmentAt: omitBlank(form.value.assessmentAt),
      recruiterName: omitBlank(form.value.recruiterName),
      recruiterEmail: omitBlank(form.value.recruiterEmail),
      contactDetails: omitBlank(form.value.contactDetails),
      salaryRange: omitBlank(form.value.salaryRange),
      salaryCurrency: omitBlank(form.value.salaryCurrency),
      salaryPeriod: omitBlank(form.value.salaryPeriod),
      status: form.value.status,
      // Explicit user choice — the backend preserves visibility when this is
      // omitted, and applies it when present.
      visible: form.value.visible
    } as UpdateJobApplicationRequest;
    emit('save', payload);
  } else {
    // Backend create contract has NO status field (the service defaults to
    // DRAFT); sending one would be silently dropped, so don't pretend.
    const payload = {
      jobTitle: form.value.jobTitle,
      companyName: form.value.companyName,
      jobLocation: omitBlank(form.value.jobLocation),
      jobType: omitBlank(form.value.jobType),
      applicationUrl: omitBlank(form.value.applicationUrl),
      resumeVersion: omitBlank(form.value.resumeVersion),
      jobDescription: omitBlank(form.value.jobDescription),
      workArrangement: omitBlank(form.value.workArrangement),
      discoveredAt: omitBlank(form.value.discoveredAt),
      deadlineAt: omitBlank(form.value.deadlineAt),
      followUpAt: omitBlank(form.value.followUpAt),
      interviewAt: omitBlank(form.value.interviewAt),
      assessmentAt: omitBlank(form.value.assessmentAt),
      recruiterName: omitBlank(form.value.recruiterName),
      recruiterEmail: omitBlank(form.value.recruiterEmail),
      contactDetails: omitBlank(form.value.contactDetails),
      salaryRange: omitBlank(form.value.salaryRange),
      salaryCurrency: omitBlank(form.value.salaryCurrency),
      salaryPeriod: omitBlank(form.value.salaryPeriod),
      initialNote: omitBlank(form.value.initialNote)
    } as CreateJobApplicationRequest;
    emit('save', payload);
  }
};
</script>

<template>
  <div v-if="modelValue" class="modal fade show d-block" tabindex="-1" role="dialog" aria-modal="true" style="background: rgba(0,0,0,0.5)">
    <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable" role="document">
      <div class="modal-content glass-modal">
        <div class="modal-header border-bottom-0">
          <h5 class="modal-title">{{ isEditing ? 'Edit Job Application' : 'Track New Application' }}</h5>
          <button type="button" class="btn-close" @click="closeModal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="save">
            <!-- Core details -->
            <h6 class="text-uppercase text-muted small mb-2">Core details</h6>
            <div class="row">
              <div class="col-md-6 mb-3">
                <label for="title" class="form-label">Job Title <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="title" v-model="form.jobTitle" required placeholder="e.g. Senior Frontend Developer">
              </div>
              <div class="col-md-6 mb-3">
                <label for="company" class="form-label">Company Name <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="company" v-model="form.companyName" required placeholder="e.g. Acme Corp">
              </div>
              <div class="col-md-4 mb-3">
                <label for="location" class="form-label">Location</label>
                <input type="text" class="form-control" id="location" v-model="form.jobLocation" placeholder="e.g. Johannesburg">
              </div>
              <div class="col-md-4 mb-3">
                <label for="job-type" class="form-label">Employment type</label>
                <input type="text" class="form-control" id="job-type" v-model="form.jobType" list="employment-type-options" placeholder="e.g. Contract">
                <datalist id="employment-type-options">
                  <option v-for="t in employmentTypes" :key="t" :value="t" />
                </datalist>
              </div>
              <div class="col-md-4 mb-3">
                <label for="work-arrangement" class="form-label">Work arrangement</label>
                <select class="form-select" id="work-arrangement" v-model="form.workArrangement">
                  <option value="">Not specified</option>
                  <option v-for="a in workArrangements" :key="a.value" :value="a.value">{{ a.label }}</option>
                </select>
              </div>
              <div class="col-12 mb-3">
                <label for="application-url" class="form-label">Job posting URL</label>
                <input type="url" class="form-control" id="application-url" v-model="form.applicationUrl" placeholder="https://...">
              </div>
              <div class="col-12 mb-3">
                <label for="job-description" class="form-label">Job description</label>
                <textarea class="form-control" id="job-description" v-model="form.jobDescription" rows="4" placeholder="Paste the posting text for later reference..."></textarea>
              </div>
            </div>

            <!-- Application info -->
            <h6 class="text-uppercase text-muted small mb-2 mt-3">Application info</h6>
            <div class="row">
              <div class="col-md-3 mb-3">
                <label for="discovered-at" class="form-label">Discovered</label>
                <input type="date" class="form-control" id="discovered-at" v-model="form.discoveredAt">
              </div>
              <div class="col-md-3 mb-3">
                <label for="deadline-at" class="form-label">Deadline</label>
                <input type="date" class="form-control" id="deadline-at" v-model="form.deadlineAt">
              </div>
              <div class="col-md-3 mb-3">
                <label for="follow-up-at" class="form-label">Follow-up</label>
                <input type="date" class="form-control" id="follow-up-at" v-model="form.followUpAt">
              </div>
              <div class="col-md-3 mb-3">
                <label for="resume-version" class="form-label">Resume version</label>
                <input type="text" class="form-control" id="resume-version" v-model="form.resumeVersion" placeholder="e.g. Standard-v1">
              </div>
              <div class="col-md-6 mb-3" v-if="isEditing">
                <label for="status" class="form-label">Status</label>
                <select class="form-select" id="status" v-model="form.status">
                  <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div class="col-md-6 mb-3" v-if="isEditing">
                <label class="form-label d-block">Visibility</label>
                <div class="form-check form-switch">
                  <input class="form-check-input" type="checkbox" role="switch" id="visible-switch" v-model="form.visible">
                  <label class="form-check-label" for="visible-switch">Visible (for future sharing features)</label>
                </div>
              </div>
            </div>

            <!-- Recruitment -->
            <h6 class="text-uppercase text-muted small mb-2 mt-3">Recruitment</h6>
            <div class="row">
              <div class="col-md-4 mb-3">
                <label for="recruiter-name" class="form-label">Contact name</label>
                <input type="text" class="form-control" id="recruiter-name" v-model="form.recruiterName" placeholder="e.g. Jane Recruiter">
              </div>
              <div class="col-md-4 mb-3">
                <label for="recruiter-email" class="form-label">Contact email</label>
                <input type="email" class="form-control" id="recruiter-email" v-model="form.recruiterEmail" placeholder="jane@company.com">
              </div>
              <div class="col-md-4 mb-3">
                <label for="contact-details" class="form-label">Other contact details</label>
                <input type="text" class="form-control" id="contact-details" v-model="form.contactDetails" placeholder="Phone, portal handle...">
              </div>
              <div class="col-md-6 mb-3">
                <label for="interview-at" class="form-label">Interview date/time</label>
                <input type="datetime-local" class="form-control" id="interview-at" v-model="form.interviewAt">
              </div>
              <div class="col-md-6 mb-3">
                <label for="assessment-at" class="form-label">Assessment date</label>
                <input type="date" class="form-control" id="assessment-at" v-model="form.assessmentAt">
              </div>
            </div>

            <!-- Compensation -->
            <h6 class="text-uppercase text-muted small mb-2 mt-3">Compensation</h6>
            <div class="row">
              <div class="col-md-6 mb-3">
                <label for="salary-range" class="form-label">Salary expectation</label>
                <input type="text" class="form-control" id="salary-range" v-model="form.salaryRange" placeholder="e.g. R45k - R55k">
              </div>
              <div class="col-md-3 mb-3">
                <label for="salary-currency" class="form-label">Currency</label>
                <input type="text" class="form-control" id="salary-currency" v-model="form.salaryCurrency" placeholder="e.g. ZAR">
              </div>
              <div class="col-md-3 mb-3">
                <label for="salary-period" class="form-label">Period</label>
                <select class="form-select" id="salary-period" v-model="form.salaryPeriod">
                  <option value="">Not specified</option>
                  <option v-for="p in salaryPeriods" :key="p" :value="p">{{ p }}</option>
                </select>
              </div>
            </div>

            <div class="mb-3" v-if="!isEditing">
              <label for="initialNote" class="form-label">Initial Note</label>
              <textarea class="form-control" id="initialNote" v-model="form.initialNote" rows="3" placeholder="Where you found it, referral details, thoughts..."></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer border-top-0">
          <button type="button" class="btn btn-secondary" @click="closeModal">Cancel</button>
          <button type="button" class="btn btn-primary" @click="save">{{ isEditing ? 'Save Changes' : 'Track Application' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.glass-modal {
  background: rgba(var(--bs-body-color-rgb), 0.95);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(var(--bs-body-color-rgb), 0.5);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
}
</style>
