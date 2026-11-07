<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { jobApplicationService } from '@/services/jobApplicationService';
import type { JobApplication, CreateJobApplicationRequest, UpdateJobApplicationRequest, ApplicationStatus } from '@/types';
import { APPLICATION_STATUS_ORDER, STATUS_META } from '@/types';
import JobApplicationCard from './JobApplicationCard.vue';
import JobApplicationModal from './JobApplicationModal.vue';
import JobApplicationDetailsModal from './JobApplicationDetailsModal.vue';
import { Briefcase, Plus, FileX, Search, Archive } from 'lucide-vue-next';

/**
 * Phase 8.4: dedicated Job Application management page.
 *
 * The API returns the owner's full list (no server pagination), so
 * search/filter/sort run client-side — consistent with the current
 * architecture. Filters are real: every control below affects the rendered
 * list; empty states distinguish "nothing tracked yet" from "no matches".
 */

const applications = ref<JobApplication[]>([]);
const loading = ref(false);
const loadError = ref<string | null>(null);

const showModal = ref(false);
const showDetailsModal = ref(false);
const selectedApplicationUuid = ref<string | null>(null);
const editingApplication = ref<JobApplication | null>(null);

// --- Toolbar state ---
const searchTerm = ref('');
const statusFilter = ref<'ALL' | ApplicationStatus>('ALL');
const typeFilter = ref('ALL');
const arrangementFilter = ref('ALL');
const sortBy = ref<'statusUpdatedAt' | 'createdAt' | 'appliedAt' | 'followUpAt' | 'deadlineAt'>('statusUpdatedAt');
const showArchived = ref(false);

/** Employment-type options offered by the create/edit form (free-text field). */
const EMPLOYMENT_TYPES = ['Full-Time', 'Part-Time', 'Contract', 'Temporary', 'Internship', 'Freelance'];
const WORK_ARRANGEMENTS = ['REMOTE', 'HYBRID', 'ON_SITE'];

const statusCounts = computed(() => {
  const counts = new Map<ApplicationStatus, number>();
  for (const app of applications.value) {
    counts.set(app.status, (counts.get(app.status) ?? 0) + 1);
  }
  return counts;
});

const filteredApplications = computed(() => {
  let list = [...applications.value];

  // Archived applications are hidden unless explicitly requested (or the
  // user filters on ARCHIVED directly) so the active pipeline stays clean.
  if (!showArchived.value && statusFilter.value !== 'ARCHIVED') {
    list = list.filter(app => app.status !== 'ARCHIVED');
  }

  const term = searchTerm.value.trim().toLowerCase();
  if (term) {
    list = list.filter(app =>
      app.companyName.toLowerCase().includes(term)
      || app.jobTitle.toLowerCase().includes(term));
  }
  if (statusFilter.value !== 'ALL') {
    list = list.filter(app => app.status === statusFilter.value);
  }
  if (typeFilter.value !== 'ALL') {
    list = list.filter(app => (app.jobType ?? '').toLowerCase() === typeFilter.value.toLowerCase());
  }
  if (arrangementFilter.value !== 'ALL') {
    list = list.filter(app =>
      (app.workArrangement ?? '').toUpperCase() === arrangementFilter.value);
  }

  const direction = sortBy.value === 'deadlineAt' || sortBy.value === 'followUpAt' ? 1 : -1; // soonest first for dates ahead
  list.sort((a, b) => {
    const av = a[sortBy.value] ?? '';
    const bv = b[sortBy.value] ?? '';
    if (av === bv) return 0;
    if (av === '') return 1; // missing values sink to the bottom
    if (bv === '') return -1;
    return av < bv ? -direction : direction;
  });
  return list;
});

const fetchApplications = async () => {
  loading.value = true;
  loadError.value = null;
  try {
    applications.value = await jobApplicationService.getAll();
  } catch (error) {
    console.error('Failed to fetch applications', error);
    loadError.value = 'Could not load your job applications. Please try again later.';
  } finally {
    loading.value = false;
  }
};

const openCreateModal = () => {
  editingApplication.value = null;
  showModal.value = true;
};

const openEditModal = (uuid: string) => {
  const app = applications.value.find(a => a.uuid === uuid);
  if (app) {
    editingApplication.value = app;
    showModal.value = true;
  }
};

const openView = (uuid: string) => {
  selectedApplicationUuid.value = uuid;
  showDetailsModal.value = true;
};

const handleSave = async (data: CreateJobApplicationRequest | UpdateJobApplicationRequest) => {
  try {
    if (editingApplication.value) {
      await jobApplicationService.update(editingApplication.value.uuid, data as UpdateJobApplicationRequest);
    } else {
      await jobApplicationService.create(data as CreateJobApplicationRequest);
    }
    showModal.value = false;
    await fetchApplications();
  } catch (error) {
    console.error('Failed to save application', error);
    alert('Failed to save application. Please try again.');
  }
};

const handleDelete = async (uuid: string) => {
  if (!confirm('Are you sure you want to delete this application?')) return;

  try {
    await jobApplicationService.delete(uuid);
    await fetchApplications();
  } catch (error) {
    console.error('Failed to delete application', error);
    alert('Failed to delete application.');
  }
};

const resetFilters = () => {
  searchTerm.value = '';
  statusFilter.value = 'ALL';
  typeFilter.value = 'ALL';
  arrangementFilter.value = 'ALL';
  sortBy.value = 'statusUpdatedAt';
};

onMounted(() => {
  fetchApplications();
});
</script>

<template>
  <div class="container-fluid py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="mb-0 text-white"><Briefcase class="me-2" />Job Applications</h2>
      <button class="btn btn-primary" @click="openCreateModal">
        <Plus class="me-1" /> Track New
      </button>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-light" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- Load error state -->
    <div v-else-if="loadError" class="alert alert-danger">
      <p class="mb-0">{{ loadError }}</p>
    </div>

    <template v-else>
      <!-- Status summary chips -->
      <div class="d-flex flex-wrap gap-2 mb-3" data-testid="status-summary">
        <button
          v-for="status in APPLICATION_STATUS_ORDER"
          :key="status"
          class="btn btn-sm"
          :class="statusFilter === status ? 'btn-light' : 'btn-outline-light'"
          type="button"
          @click="statusFilter = statusFilter === status ? 'ALL' : status">
          {{ STATUS_META[status].label }}
          <span class="badge ms-1" :class="STATUS_META[status].badge">{{ statusCounts.get(status) ?? 0 }}</span>
        </button>
      </div>

      <!-- Toolbar: search / filters / sort -->
      <div class="card glass-card mb-4" data-testid="tracker-toolbar">
        <div class="card-body">
          <div class="row g-2 align-items-end">
            <div class="col-12 col-md-4">
              <label class="form-label small mb-1" for="tracker-search">Search</label>
              <div class="input-group input-group-sm">
                <span class="input-group-text"><Search /></span>
                <input
                  id="tracker-search"
                  v-model="searchTerm"
                  class="form-control"
                  placeholder="Company or job title..."
                  type="search"
                >
              </div>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small mb-1" for="filter-status">Status</label>
              <select id="filter-status" v-model="statusFilter" class="form-select form-select-sm">
                <option value="ALL">All statuses</option>
                <option v-for="status in APPLICATION_STATUS_ORDER" :key="status" :value="status">
                  {{ STATUS_META[status].label }}
                </option>
              </select>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small mb-1" for="filter-type">Employment type</label>
              <select id="filter-type" v-model="typeFilter" class="form-select form-select-sm">
                <option value="ALL">All types</option>
                <option v-for="t in EMPLOYMENT_TYPES" :key="t" :value="t">{{ t }}</option>
              </select>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small mb-1" for="filter-arrangement">Work arrangement</label>
              <select id="filter-arrangement" v-model="arrangementFilter" class="form-select form-select-sm">
                <option value="ALL">All arrangements</option>
                <option v-for="a in WORK_ARRANGEMENTS" :key="a" :value="a">{{ a }}</option>
              </select>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small mb-1" for="sort-by">Sort by</label>
              <select id="sort-by" v-model="sortBy" class="form-select form-select-sm">
                <option value="statusUpdatedAt">Last status change</option>
                <option value="createdAt">Date created</option>
                <option value="appliedAt">Date applied</option>
                <option value="followUpAt">Follow-up date</option>
                <option value="deadlineAt">Deadline</option>
              </select>
            </div>
          </div>
          <div class="d-flex justify-content-between align-items-center mt-2">
            <div class="form-check form-switch">
              <input id="show-archived" v-model="showArchived" class="form-check-input" type="checkbox" role="switch">
              <label class="form-check-label small" for="show-archived">
                <Archive class="me-1" style="width: 14px; height: 14px;" />Show archived
              </label>
            </div>
            <button class="btn btn-sm btn-outline-light" type="button" @click="resetFilters">Reset filters</button>
          </div>
        </div>
      </div>

      <!-- Empty states -->
      <div v-if="applications.length === 0" class="text-center py-5 text-white-50">
        <FileX class="fs-1 mb-3 d-block" />
        <p class="lead">No applications tracked yet. Start your journey!</p>
        <button class="btn btn-outline-light mt-2" @click="openCreateModal">Track First Application</button>
      </div>
      <div v-else-if="filteredApplications.length === 0" class="text-center py-5 text-white-50">
        <Search class="fs-1 mb-3 d-block" />
        <p class="lead">No applications match your search or filters.</p>
        <button class="btn btn-outline-light mt-2" @click="resetFilters">Clear filters</button>
      </div>

      <!-- Card grid -->
      <div v-else class="row row-cols-1 row-cols-md-2 row-cols-lg-3 row-cols-xl-4 g-4">
        <div class="col" v-for="app in filteredApplications" :key="app.uuid">
          <JobApplicationCard
            :application="app"
            @view="openView"
            @edit="openEditModal"
            @delete="handleDelete"
          />
        </div>
      </div>
    </template>

    <JobApplicationModal
      v-model="showModal"
      :application="editingApplication"
      @save="handleSave"
    />

    <JobApplicationDetailsModal
      v-model="showDetailsModal"
      :application-uuid="selectedApplicationUuid"
    />
  </div>
</template>
