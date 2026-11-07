<template>
  <div class="user-qualifications-page py-5 flex-grow-1">
    <!--suppress HtmlUnknownBooleanAttribute -->
    <div class="container">
      <!-- Header -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-3 animate-fade-in-up">
        <h2 class="mb-0 glass-text">Manage My Qualifications</h2>
        <button class="btn btn-primary interactive-lift" @click="openAddModal">
          <PlusCircle class="me-2" />Add New Qualification
        </button>
      </div>

      <!-- Modals -->
      <LoadingModal :visible="isLoading"/>
      <ErrorModal :message="error || ''" :visible="!!error" title="An Error Occurred"
                  @close="error = null"/>
      <SuccessModal :message="successMessage || ''" :visible="!!successMessage" title="Success"
                    @close="successMessage = null"/>
      <ConfirmModal
        ref="confirmModalRef"
        :message="`Are you sure you want to delete the qualification '${qualificationToDelete?.qualificationName}'?`"
        title="Confirm Deletion"
        @close="qualificationToDelete = null"
        @confirm="handleDeleteQualification"
      />

      <!-- REFACTOR: The modal is now a self-contained component -->
      <QualificationFormModal
        ref="qualificationFormModalRef"
        :is-editing="isEditing"
        :qualification="currentQualificationForModal"
        @save="handleSaveQualification"
      />

      <!-- Qualifications List -->
      <div v-if="!isLoading && qualifications.length > 0" class="card glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <ul class="list-group list-group-flush">
          <li v-for="qual in qualifications" :key="qual.uuid"
              class="list-group-item d-flex justify-content-between align-items-center">
            <div>
              <h5 class="mb-1">{{ qual.qualificationName }}</h5>
              <p class="mb-1 text-primary fw-bold">
                {{ qual.institutionName }}
              </p>
              <small class="d-block text-muted">{{ qual.startYear }} -
                {{ qual.stillStudying ? 'Present' : qual.completionYear }}</small>
              <small v-if="qual.fieldOfStudy" class="d-block text-muted">{{
                  qual.fieldOfStudy
                }}</small>
              <small v-if="qual.grade" class="text-muted">Grade: {{ qual.grade }}</small>
            </div>
            <div class="actions d-flex align-items-center">
              <VisibilityToggle :is-loading="!!qual.isVisibilityLoading" :visible="!!qual.visible"
                                class="me-3" @toggle="handleVisibilityToggle(qual)"/>
              <button class="btn btn-sm btn-outline-primary me-2" title="Edit Qualification"
                      @click="openEditModal(qual)">
                <Pencil />
              </button>
              <button class="btn btn-sm btn-outline-danger" title="Delete Qualification"
                      @click="openDeleteConfirm(qual)">
                <Trash2 />
              </button>
            </div>
          </li>
        </ul>
      </div>

      <!-- Empty State -->
      <div v-else-if="!isLoading" class="text-center p-5 glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <div class="empty-state-icon mb-4">
          <GraduationCap />
        </div>
        <h4 class="glass-title">No Qualifications Found</h4>
        <p class="glass-subtitle">You haven't added any qualifications yet. Click the button above
          to get started!</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { qualificationsApi } from '@/services/api/user.api';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore';
import { authService } from '@/services/authService';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';
import QualificationFormModal from '@/components/user/QualificationFormModal.vue';
import VisibilityToggle from '@/components/common/VisibilityToggle.vue';
import type { Qualification } from '@/types';
import { PlusCircle, Pencil, Trash2, GraduationCap } from 'lucide-vue-next';

interface QualificationWithLoading extends Qualification {
  isVisibilityLoading: boolean;
}

// --- State ---
const qualifications = ref<QualificationWithLoading[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);
const qualificationToDelete = ref<Qualification | null>(null);

// --- Modal State ---
const qualificationFormModalRef = ref<{ show: () => void; hide: () => void } | null>(null);
const confirmModalRef = ref<{ show: () => void; hide: () => void } | null>(null);
const isEditing = ref(false);
const currentQualificationForModal = ref<Qualification | null>(null);

// --- Store and Services ---
const portfolioStore = usePublicPortfolioStore();

const refreshPublicData = async () => {
  const userSlug = authService.user.value?.slug;
  if (userSlug) {
    await portfolioStore.fetchPortfolio(userSlug, true);
  }
};

// --- Lifecycle Hooks ---
onMounted(async () => {
  await fetchQualifications();
});

// --- Data Fetching ---
const fetchQualifications = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    const fetched = await qualificationsApi.getAll() as Qualification[];
    qualifications.value = fetched
      .map(q => ({ ...q, isVisibilityLoading: false } as QualificationWithLoading))
      .sort((a, b) => {
        // REFACTOR: Use the more robust sorting from the public page and add a secondary sort.
        const yearA = a.stillStudying ? Infinity : a.completionYear || a.startYear || 0;
        const yearB = b.stillStudying ? Infinity : b.completionYear || b.startYear || 0;
        if (yearA !== yearB) {
          return yearB - yearA; // Sort by year (descending, with "Present" first)
        }
        // Secondary sort: alphabetically by name if years are the same
        return a.qualificationName.localeCompare(b.qualificationName);
      }) as QualificationWithLoading[];
  } catch (err: unknown) {
    console.error("Failed to fetch user qualifications:", err);
    error.value = err instanceof Error ? err.message : 'An unexpected error occurred while fetching your qualifications.';
  } finally {
    isLoading.value = false;
  }
};

// --- Modal Handling ---
const openAddModal = () => {
  isEditing.value = false;
  currentQualificationForModal.value = null;
  qualificationFormModalRef.value?.show();
};

const openEditModal = (qual: Qualification) => {
  isEditing.value = true;
  currentQualificationForModal.value = JSON.parse(JSON.stringify(qual));
  qualificationFormModalRef.value?.show();
};

const openDeleteConfirm = (qual: Qualification) => {
  qualificationToDelete.value = qual;
  confirmModalRef.value?.show();
};

const buildPayload = (qual: Qualification): Partial<Qualification> => ({
  qualificationName: qual.qualificationName,
  institutionName: qual.institutionName,
  institutionLogoUrl: qual.institutionLogoUrl || undefined,
  institutionWebsite: qual.institutionWebsite || undefined,
  fieldOfStudy: qual.fieldOfStudy || undefined,
  level: qual.level,
  startYear: qual.startYear,
  // Ensure completionYear is null if still studying, otherwise send the value or null
  completionYear: qual.stillStudying ? undefined : (qual.completionYear || undefined),
  stillStudying: qual.stillStudying,
  grade: qual.grade || undefined,
  credentialUrl: qual.credentialUrl || undefined,
  visible: qual.visible
});

const handleSaveQualification = async (qualificationData: Qualification) => {
  isLoading.value = true;
  error.value = null;
  qualificationFormModalRef.value?.hide();

  try {
    const payload = buildPayload(qualificationData);
    if (isEditing.value) {
      if (!qualificationData.uuid) throw new Error("Qualification UUID is missing.");
      await qualificationsApi.update(qualificationData.uuid, payload);
      successMessage.value = `Qualification '${payload.qualificationName}' was updated successfully.`;
    } else {
      await qualificationsApi.create(payload);
      successMessage.value = `Qualification '${payload.qualificationName}' was added successfully.`;
    }
    await fetchQualifications();
    await refreshPublicData();
  } catch (err: unknown) {
    console.error("Failed to save qualification:", err);
    error.value = err instanceof Error ? err.message : 'An error occurred while saving the qualification.';
  } finally {
    isLoading.value = false;
  }
};

const handleDeleteQualification = async () => {
  const target = qualificationToDelete.value;
  if (!target || !target.uuid) return;

  isLoading.value = true;
  error.value = null;
  confirmModalRef.value?.hide();
  try {
    await qualificationsApi.remove(target.uuid);
    // Optimistically remove from the local array for a faster UI response
    qualifications.value = qualifications.value.filter(q => q.uuid !== target.uuid);
    await refreshPublicData();
    // CORRECTED: Referenced qualificationName instead of non-existent 'name'
    successMessage.value = `Qualification '${target.qualificationName}' was deleted successfully.`;
  } catch (err: unknown) {
    console.error("Failed to delete qualification:", err);
    error.value = err instanceof Error ? err.message : 'An error occurred while deleting the qualification.';
  } finally {
    isLoading.value = false;
  } // The @close event on the modal will reset qualificationToDelete.
};

const handleVisibilityToggle = async (qual: QualificationWithLoading) => {
  qual.isVisibilityLoading = true;
  const originalVisibility = qual.visible;
  qual.visible = !qual.visible;

  try {
    const payload = buildPayload(qual);
    if (!qual.uuid) throw new Error("Qualification UUID is missing.");
    await qualificationsApi.update(qual.uuid, payload);
    successMessage.value = `Visibility for '${qual.qualificationName}' updated.`;
    await refreshPublicData();
  } catch (err: unknown) {
    qual.visible = originalVisibility;
    console.error("Failed to update visibility:", err);
    error.value = err instanceof Error ? err.message : 'An error occurred while updating visibility.';
  } finally {
    qual.isVisibilityLoading = false;
  }
};
</script>

<style scoped>
.list-group-item {
  background-color: transparent;
  border-bottom: 1px solid var(--glass-border);
  color: var(--glass-text);
  transition: background-color 0.3s ease;
  padding: 1.25rem;
}

.list-group-item:last-child {
  border-bottom: none;
}

.list-group-item:hover {
  background-color: var(--glass-bg-hover);
}

.list-group-item .actions {
  opacity: 0;
  transition: opacity 0.2s ease-in-out;
  flex-shrink: 0;
  margin-left: 1rem;
}

.list-group-item:hover .actions {
  opacity: 1;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--glass-text);
}
</style>
