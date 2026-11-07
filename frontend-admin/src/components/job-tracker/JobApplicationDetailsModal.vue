<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { jobApplicationService } from '@/services/jobApplicationService';
import type { JobApplication } from '@/types';
import { STATUS_META } from '@/types';
import { MapPin, FileText, XCircle, Send, ExternalLink, CalendarClock, AlarmClock } from 'lucide-vue-next';

const props = defineProps<{
  modelValue: boolean;
  applicationUuid?: string | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
}>();

const application = ref<JobApplication | null>(null);
const loading = ref(false);
const newNoteContent = ref('');
const submittingNote = ref(false);

watch(() => props.modelValue, async (newVal) => {
  if (newVal && props.applicationUuid) {
    await loadDetails(props.applicationUuid);
    newNoteContent.value = '';
  }
});

const loadDetails = async (uuid: string) => {
  loading.value = true;
  try {
    application.value = await jobApplicationService.getByUuid(uuid);
  } catch (err) {
    console.error('Failed to load application details', err);
    // Optionally emit close or show error
  } finally {
    loading.value = false;
  }
};

const closeModal = () => {
  emit('update:modelValue', false);
  application.value = null;
};

const addNote = async () => {
  if (!application.value || !newNoteContent.value.trim()) return;

  submittingNote.value = true;
  try {
    // Optimistic update could be done here, but let's wait for server response for safety
    const addedNote = await jobApplicationService.addNote(application.value.uuid, newNoteContent.value);

    // Refresh details or push to local list if we trust the response
    if (application.value.notes) {
      application.value.notes.push(addedNote);
    } else {
      application.value.notes = [addedNote];
    }

    newNoteContent.value = '';
  } catch (err) {
    console.error('Failed to add note', err);
    alert('Failed to add note.');
  } finally {
    submittingNote.value = false;
  }
};

const deleteNote = async (noteUuid: string) => {
  if (!application.value || !confirm('Delete this note?')) return;

  try {
    await jobApplicationService.deleteNote(application.value.uuid, noteUuid);
    application.value.notes = application.value.notes?.filter(n => n.uuid !== noteUuid);
  } catch (err) {
    console.error('Failed to delete note', err);
    alert('Failed to delete note.');
  }
};

const formatDate = (dateStr?: string) => dateStr ? new Date(dateStr).toLocaleString() : '';
const formatDay = (dateStr?: string) => dateStr ? new Date(dateStr + (dateStr.length === 10 ? 'T00:00:00' : '')).toLocaleDateString() : '';

const statusMeta = computed(() => {
  if (!application.value) return { label: '', badge: 'bg-secondary' };
  return STATUS_META[application.value.status] ?? { label: application.value.status, badge: 'bg-secondary' };
});

const workArrangementLabel = computed(() => {
  switch ((application.value?.workArrangement ?? '').toUpperCase()) {
    case 'REMOTE': return 'Remote';
    case 'HYBRID': return 'Hybrid';
    case 'ON_SITE': return 'On-site';
    default: return '';
  }
});

const salaryLine = computed(() => {
  const app = application.value;
  if (!app?.salaryRange) return '';
  let line = app.salaryRange;
  if (app.salaryCurrency) line += ` ${app.salaryCurrency}`;
  if (app.salaryPeriod) line += ` / ${app.salaryPeriod.toLowerCase()}`;
  return line;
});
</script>

<template>
  <div v-if="modelValue" class="modal fade show d-block" tabindex="-1" role="dialog" aria-modal="true" style="background: rgba(0,0,0,0.5)">
    <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable" role="document">
      <div class="modal-content glass-modal">
        <div class="modal-header border-bottom-0">
          <h5 class="modal-title">Application Details</h5>
          <button type="button" class="btn-close" @click="closeModal" aria-label="Close"></button>
        </div>

        <div class="modal-body">
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border text-primary" role="status">
              <span class="visually-hidden">Loading...</span>
            </div>
          </div>

          <div v-else-if="application">
            <!-- Header Info -->
            <div class="d-flex justify-content-between align-items-start mb-4">
              <div>
                <h3 class="fw-bold mb-1">{{ application.jobTitle }}</h3>
                <h5 class="text-muted">{{ application.companyName }}</h5>
                <p class="text-muted small mb-0">
                  <MapPin class="me-1" />{{ application.jobLocation || 'Location unknown' }}
                  <span v-if="workArrangementLabel" class="ms-2 badge bg-light text-dark">{{ workArrangementLabel }}</span>
                  <span v-if="application.jobType" class="ms-2 badge bg-light text-dark">{{ application.jobType }}</span>
                </p>
              </div>
              <span class="badge fs-6" :class="statusMeta.badge" data-testid="details-status">{{ statusMeta.label }}</span>
            </div>

            <!-- Key dates -->
            <div class="row mb-3 small">
              <div class="col-6 col-md-4" v-if="application.discoveredAt">
                <strong>Discovered:</strong> {{ formatDay(application.discoveredAt) }}
              </div>
              <div class="col-6 col-md-4" v-if="application.appliedAt">
                <strong>Applied:</strong> {{ formatDay(application.appliedAt) }}
              </div>
              <div class="col-6 col-md-4" v-if="application.deadlineAt">
                <CalendarClock class="me-1" style="width: 14px; height: 14px;" /><strong>Deadline:</strong> {{ formatDay(application.deadlineAt) }}
              </div>
              <div class="col-6 col-md-4" v-if="application.followUpAt">
                <AlarmClock class="me-1" style="width: 14px; height: 14px;" /><strong>Follow-up:</strong> {{ formatDay(application.followUpAt) }}
              </div>
              <div class="col-6 col-md-4" v-if="application.interviewAt">
                <strong>Interview:</strong> {{ formatDate(application.interviewAt) }}
              </div>
              <div class="col-6 col-md-4" v-if="application.assessmentAt">
                <strong>Assessment:</strong> {{ formatDay(application.assessmentAt) }}
              </div>
            </div>

            <!-- Posting / contact / compensation -->
            <div class="row mb-3 small text-muted">
              <div class="col-md-6" v-if="application.applicationUrl">
                <strong>Posting:</strong>
                <a :href="application.applicationUrl" target="_blank" rel="noopener noreferrer" class="ms-1">
                  Open listing <ExternalLink style="width: 12px; height: 12px;" />
                </a>
              </div>
              <div class="col-md-6" v-if="application.resumeVersion">
                <strong>Resume version:</strong> {{ application.resumeVersion }}
              </div>
              <div class="col-md-4" v-if="application.recruiterName">
                <strong>Contact:</strong> {{ application.recruiterName }}
              </div>
              <div class="col-md-4" v-if="application.recruiterEmail">
                <strong>Email:</strong> <a :href="`mailto:${application.recruiterEmail}`">{{ application.recruiterEmail }}</a>
              </div>
              <div class="col-md-4" v-if="application.contactDetails">
                <strong>Other:</strong> {{ application.contactDetails }}
              </div>
              <div class="col-md-6" v-if="salaryLine">
                <strong>Salary:</strong> {{ salaryLine }}
              </div>
            </div>

            <!-- Job description -->
            <div v-if="application.jobDescription" class="mb-3">
              <h6 class="mb-1"><FileText class="me-1" style="width: 16px; height: 16px;" />Job Description</h6>
              <div class="card border-0 bg-light bg-opacity-75">
                <div class="card-body py-2 px-3" style="white-space: pre-wrap; max-height: 200px; overflow-y: auto;">{{ application.jobDescription }}</div>
              </div>
            </div>

            <div class="row mb-2 small text-muted">
              <div class="col-6">
                <strong>Created:</strong> {{ formatDate(application.createdAt) }}
              </div>
              <div class="col-6 text-end">
                <span v-if="application.updatedAt"><strong>Updated:</strong> {{ formatDate(application.updatedAt) }}</span>
              </div>
            </div>

            <hr class="text-white-50" />

            <!-- Notes Section -->
            <h5 class="mb-3"><FileText class="me-2" />Notes</h5>

            <div class="notes-container mb-3">
              <div v-if="!application.notes || application.notes.length === 0" class="text-center text-muted py-3 fst-italic">
                No notes yet. Keep track of specific details here!
              </div>

              <div v-for="note in application.notes" :key="note.uuid" class="card mb-2 border-0 bg-light bg-opacity-75">
                <div class="card-body py-2 px-3">
                  <div class="d-flex justify-content-between align-items-start">
                    <div style="white-space: pre-wrap;">{{ note.content }}</div>
                    <button class="btn btn-link link-danger btn-sm p-0 ms-2" @click="deleteNote(note.uuid)" title="Delete Note">
                      <XCircle />
                    </button>
                  </div>
                  <div class="text-muted small mt-1" style="font-size: 0.75rem;">
                    {{ formatDate(note.createdAt) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Add Note Form -->
            <div class="mt-3">
              <label for="newNote" class="form-label visually-hidden">Add a note</label>
              <div class="input-group">
                <textarea
                  class="form-control"
                  id="newNote"
                  v-model="newNoteContent"
                  rows="2"
                  placeholder="Add a new note (e.g. Interview scheduled for Friday...)"
                ></textarea>
                <button class="btn btn-primary" type="button" @click="addNote" :disabled="submittingNote || !newNoteContent.trim()">
                  <Send />
                </button>
              </div>
            </div>

          </div>

          <div v-else class="text-center text-danger py-4">
            Failed to load application details.
          </div>
        </div>

        <div class="modal-footer border-top-0">
          <button type="button" class="btn btn-secondary" @click="closeModal">Close</button>
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

.notes-container {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 5px;
}
</style>
