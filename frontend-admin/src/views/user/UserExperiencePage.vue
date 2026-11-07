<template>
  <div class="user-experience-page py-5 flex-grow-1">
    <div class="container">
      <!-- Header -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-3 animate-fade-in-up">
        <h2 class="mb-0 glass-text">Manage My Experience</h2>
        <button class="btn btn-primary interactive-lift" @click="openAddModal">
          <PlusCircle class="me-2" />Add New Experience
        </button>
      </div>

      <!-- Common Modals -->
      <LoadingModal :visible="isLoading"/>
      <ErrorModal :message="error || ''" :visible="!!error" title="An Error Occurred"
                  @close="error = null"/>
      <SuccessModal :message="successMessage || ''" :visible="!!successMessage" title="Success"
                    @close="successMessage = null"/>
      <ConfirmModal
        ref="confirmModalRef"
        :message="`Are you sure you want to delete the entry for '${experienceToDelete?.jobTitle} at ${experienceToDelete?.companyName}'?`"
        title="Confirm Deletion"
        @close="experienceToDelete = null"
        @confirm="handleDeleteExperience"
      />

      <!-- The new, self-contained form modal component -->
      <ExperienceFormModal
        ref="experienceFormModalRef"
        :available-skills="platformSkills.map(s => s.name)"
        :experience="currentExperience"
        @save="handleSaveExperience"
      />

      <!-- Experience Timeline -->
      <div v-if="!isLoading && experiences.length > 0" class="timeline">
        <div v-for="(exp, index) in experiences" :key="exp.uuid"
             :style="{ 'animation-delay': (index * 0.15) + 's' }"
             class="timeline-item animate-fade-in-up">
          <div class="timeline-content card glass-card h-100 p-0">
            <div class="card-header d-flex justify-content-between align-items-center">
              <div class="d-flex align-items-center text-truncate">
                <img v-if="exp.companyLogoUrl" :alt="`${exp.companyName} Logo`"
                     :src="exp.companyLogoUrl" class="company-logo-header me-2">
                <div v-else class="company-logo-placeholder-header me-2">
                  <Building2 />
                </div>
                <h6 :title="exp.companyName" class="mb-0 glass-subtitle text-truncate">{{
                    exp.companyName
                  }}</h6>
              </div>
              <div class="actions d-flex align-items-center flex-shrink-0">
                <VisibilityToggle :is-loading="!!exp.isVisibilityLoading" :visible="!!exp.visible"
                                  class="me-3" @toggle="handleVisibilityToggle(exp)"/>
                <button class="btn btn-sm btn-outline-primary me-2" title="Edit Experience"
                        @click="openEditModal(exp)">
                  <Pencil />
                </button>
                <button class="btn btn-sm btn-outline-danger" title="Delete Experience"
                        @click="openDeleteConfirm(exp)">
                  <Trash2 />
                </button>
              </div>
            </div>
            <div class="card-body">
              <h5 class="card-title glass-title">{{ exp.jobTitle }}</h5>
              <p class="card-text glass-text-secondary small mb-3">
                <Calendar class="me-1" />
                {{ formatDate(exp.startDate) }} -
                {{ exp.endDate ? formatDate(exp.endDate) : 'Present' }}
                <br>
                <MapPin class="me-1" />
                {{ exp.location }}
              </p>
              <!-- Phase 7.4 §10 (self-XSS hardening): text, not HTML -->
              <p v-if="exp.description"
                 class="card-text glass-description description-text mt-2 mb-0">{{ exp.description }}</p>
              <!-- Phase 7.4 (§7) FIX: achievements were previously invisible in the
                   management list (only editable in the modal and shown publicly),
                   so users could not see their persisted data here. -->
              <div v-if="exp.achievements" class="achievements-block mt-2 mb-0">
                <h6 class="achievements-title small text-uppercase text-muted mb-1">Key
                  Achievements</h6>
                <div class="card-text glass-description small achievements-text">{{ exp.achievements }}</div>
              </div>
              <div v-if="exp.skills && exp.skills.length > 0" class="mt-3 pt-3 border-top-glass">
                <div class="d-flex flex-wrap gap-2">
                  <template v-for="skill in exp.skills" :key="typeof skill === 'string' ? skill : skill.skillId">
                    <SkillBadge v-if="typeof skill === 'object'" :skill="skill"/>
                    <span v-else class="badge btn-outline-primary">{{ skill }}</span>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!isLoading" class="text-center p-5 glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <div class="empty-state-icon mb-4"><Briefcase /></div>
        <h4 class="glass-title">No Experience Found</h4>
        <p class="glass-subtitle">You haven't added any work experience yet. Click the button above
          to get started!</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { PlusCircle, Building2, Pencil, Trash2, Calendar, MapPin, Briefcase } from 'lucide-vue-next';
import { experiencesApi } from '@/services/api/user.api';
import { platformSkillApi } from '@/services/api/skill.api';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore';
import { authService } from '@/services/authService';
import { formatDisplayDate as formatDate } from '@/utils/dateUtils';

// Import all child components
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';
import ExperienceFormModal from '@/components/user/ExperienceFormModal.vue';
import SkillBadge from '@/components/common/SkillBadge.vue';
import VisibilityToggle from '@/components/common/VisibilityToggle.vue';
import type { Experience, Skill, User } from '@/types';

interface ExperienceWithLoading extends Experience {
  isVisibilityLoading?: boolean;
}

// --- State for this page ---
const experiences = ref<ExperienceWithLoading[]>([]);
const platformSkills = ref<Skill[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);
const experienceToDelete = ref<ExperienceWithLoading | null>(null);
const currentExperience = ref<Experience | null>(null); // For editing

// --- Modal Instances ---
const experienceFormModalRef = ref<{ show: () => void; hide: () => void } | null>(null);
const confirmModalRef = ref<{ show: () => void; hide: () => void } | null>(null);

// --- Store and Services ---
const portfolioStore = usePublicPortfolioStore();

const refreshPublicData = async () => {
  const userSlug = (authService.user.value as User | null)?.slug;
  if (userSlug) {
    await portfolioStore.fetchPortfolio(userSlug, true);
  }
};

// --- Lifecycle ---
onMounted(async () => {
  // Fetch experiences and all available skills in parallel for faster loading.
  await Promise.all([
    fetchExperiences(),
    fetchPlatformSkills()
  ]);
});

// REFACTOR: Centralize the creation of the API payload to ensure consistency.
const buildPayload = (exp: Experience) => {
  // Create a clean payload object for the API.
  const payload: Partial<Experience> = {
    ...exp,
  };

  // FIX: The skills can be an array of objects (from the main list) or an array of strings (from the form).
  // The API always expects an array of strings. This logic handles both cases.
  if (Array.isArray(exp.skills) && exp.skills.length > 0) {
    // If the first item is an object with a 'name' property, map to names.
    if (typeof exp.skills[0] === 'object' && exp.skills[0] !== null && 'name' in (exp.skills[0] as unknown as object)) {
      payload.skills = (exp.skills as Skill[]).map((skill: Skill) => skill.name);
    } // Otherwise, it's already an array of strings from the form, so we do nothing.
  } else {
    payload.skills = [];
  }

  // Remove any frontend-only state properties before sending to the backend.
  delete (payload as Partial<ExperienceWithLoading>).isVisibilityLoading;

  return payload;
};

// --- Data Fetching ---
const fetchExperiences = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    const fetched = await experiencesApi.getAll() as Experience[];
    // FIX: Implement a more robust sorting logic. It now sorts by displayOrder first,
    // and then by start date (newest first) as a secondary criterion. This handles the
    // case where all display orders are 0, sorting them chronologically.
    experiences.value = (fetched as Experience[])
      .map(e => ({ ...e, isVisibilityLoading: false } as ExperienceWithLoading)) // Add loading state for the toggle
      .sort((a, b) => {
        // Primary sort: displayOrder (lower numbers first)
        const orderA = a.displayOrder ?? 999;
        const orderB = b.displayOrder ?? 999;
        if (orderA !== orderB) {
          return orderA - orderB;
        }
        // Secondary sort: startDate (newest first)
        const dateA = a.startDate ? new Date(a.startDate).getTime() : 0;
        const dateB = b.startDate ? new Date(b.startDate).getTime() : 0;
        return dateB - dateA;
      });

  } catch (err: unknown) {
    console.error("Failed to fetch experiences:", err);
    error.value = err instanceof Error ? err.message : 'An unexpected error occurred.';
  } finally {
    isLoading.value = false;
  }
};

const fetchPlatformSkills = async () => {
  try {
    // FIX: Fetch all available skills for the suggestion box, not just the user's skills.
    platformSkills.value = await platformSkillApi.getAll() as Skill[];
  } catch (err: unknown) {
    console.error("Failed to load platform skills for suggestions:", err);
    // Non-critical error, the form will still work but without suggestions.
  }
};

// --- Modal Handling ---
const openAddModal = () => {
  currentExperience.value = null;
  experienceFormModalRef.value?.show();
};

const openEditModal = (exp: Experience) => {
  // Pass a deep copy to the modal to prevent direct mutation of the list item
  currentExperience.value = JSON.parse(JSON.stringify(exp));
  experienceFormModalRef.value?.show();
};

const openDeleteConfirm = (exp: ExperienceWithLoading) => {
  experienceToDelete.value = exp;
  confirmModalRef.value?.show();
};

// --- CRUD Operations ---
const handleSaveExperience = async (experienceData: Experience) => {
  isLoading.value = true;
  experienceFormModalRef.value?.hide();
  try {
    // REFACTOR: Always build the payload in the parent component for consistency.
    // This ensures the data is in the correct format for the API.
    const payload = buildPayload(experienceData);
    if (payload.uuid) {
      await experiencesApi.update(payload.uuid, payload);
      successMessage.value = `Experience at '${payload.companyName}' was updated.`;
    } else {
      await experiencesApi.create(payload);
      successMessage.value = `Experience at '${payload.companyName}' was added.`;
    }
    await fetchExperiences();
    await refreshPublicData();
  } catch (err: unknown) {
    console.error("Failed to save experience:", err);
    error.value = err instanceof Error ? err.message : 'Failed to save experience.';
  } finally {
    isLoading.value = false;
  }
};

const handleDeleteExperience = async () => {
  const target = experienceToDelete.value;
  if (!target) return;
  isLoading.value = true;
  confirmModalRef.value?.hide();
  try {
    if (!target.uuid) return;
    await experiencesApi.remove(target.uuid);
    // Optimistically remove from the local array for a faster UI response
    experiences.value = experiences.value.filter(e => e.uuid !== target.uuid);
    await refreshPublicData();
    successMessage.value = `Experience at '${target.companyName}' was deleted.`;
  } catch (err: unknown) {
    console.error("Failed to delete experience:", err);
    error.value = err instanceof Error ? err.message : 'Failed to delete experience.';
  } finally {
    isLoading.value = false;
  } // The @close event on the modal will reset experienceToDelete.
};

const handleVisibilityToggle = async (exp: ExperienceWithLoading) => {
  exp.isVisibilityLoading = true;
  const originalVisibility = exp.visible;
  exp.visible = !exp.visible; // Optimistic update
  try {
    if (!exp.uuid) return;
    const payload = buildPayload(exp);
    await experiencesApi.update(exp.uuid, payload);
    successMessage.value = `Visibility for '${exp.jobTitle}' updated.`;
    await refreshPublicData();
  } catch (err: unknown) {
    exp.visible = originalVisibility; // Revert on error
    console.error("Failed to update visibility:", err);
    error.value = err instanceof Error ? err.message : 'Failed to update visibility.';
  } finally {
    exp.isVisibilityLoading = false;
  }
};
</script>

<style scoped>
/* The page-specific styles remain, but all modal-related styles are now gone! */
.description-text {
  white-space: pre-wrap;
  font-size: 0.95em;
  color: var(--glass-text-secondary);
}

/* Phase 7.4 (§7): management-list rendering of persisted achievements */
.achievements-text {
  white-space: pre-wrap;
  word-wrap: break-word;
}

.form-check-input {
  cursor: pointer;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--glass-text);
}

.border-top-glass {
  border-top: 1px solid var(--glass-border);
}

.card-header {
  background-color: rgba(var(--bs-body-color-rgb), 0.03);
  border-bottom: 1px solid var(--glass-border);
  padding: 0.75rem 1.25rem;
}

.company-logo-header, .company-logo-placeholder-header {
  width: 32px;
  height: 32px;
  border-radius: 0.35rem;
  object-fit: contain;
  flex-shrink: 0;
  background-color: rgba(var(--bs-body-color-rgb), 0.05);
}

.company-logo-placeholder-header {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: var(--glass-text-secondary);
}

/* --- Timeline Styling --- */
.timeline {
  position: relative;
  padding: 20px 0;
  max-width: 900px;
  margin: 0 auto;
}

.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 3px;
  background-image: linear-gradient(to bottom, transparent, var(--glass-border-hover), transparent);
  transform: translateX(-50%);
  border-radius: 3px;
}

.timeline-item {
  position: relative;
  width: 50%;
  padding: 10px 40px;
  margin-bottom: 20px;
}

.timeline-item:nth-child(odd) {
  left: 0;
}

.timeline-item:nth-child(even) {
  left: 50%;
}

.timeline-item::after {
  content: '';
  position: absolute;
  width: 25px;
  height: 25px;
  right: -12.5px;
  background-color: var(--glass-bg);
  border: 4px solid var(--bs-primary);
  top: 15px;
  border-radius: 50%;
  z-index: 1;
  box-shadow: 0 0 15px rgba(var(--bs-primary-rgb), 0.5);
  transition: all 0.3s ease;
}

.timeline-item:hover::after {
  transform: scale(1.1);
  box-shadow: 0 0 25px rgba(var(--bs-primary-rgb), 0.7);
}

.timeline-item:nth-child(even)::after {
  left: -12.5px;
}

.timeline-content {
  position: relative;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.timeline-item:hover .timeline-content {
  transform: translateY(-5px);
  box-shadow: 0 1rem 2rem var(--glass-shadow);
}

.timeline-item .actions {
  opacity: 0;
  transition: opacity 0.3s ease;
}

.timeline-item:hover .actions {
  opacity: 1;
}

/* The little arrow pointing from the card to the timeline */
.timeline-item::before {
  content: '';
  position: absolute;
  top: 28px;
  width: 15px;
  height: 15px;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transform: translateY(-50%) rotate(45deg);
  z-index: 0;
}

.timeline-item:nth-child(odd)::before {
  right: 32.5px;
  border-top: 1px solid var(--glass-border);
  border-right: 1px solid var(--glass-border);
  border-left: none;
  border-bottom: none;
}

.timeline-item:nth-child(even)::before {
  left: 32.5px;
  border-left: 1px solid var(--glass-border);
  border-bottom: 1px solid var(--glass-border);
  border-top: none;
  border-right: none;
}

/* --- Responsive Adjustments --- */
@media (max-width: 767.98px) {
  .timeline::before {
    left: 15px;
  }

  .timeline-item {
    width: 100%;
    padding-left: 70px;
    padding-right: 25px;
  }

  .timeline-item:nth-child(odd),
  .timeline-item:nth-child(even) {
    left: 0;
  }


    .timeline-item::after {
    left: 2.5px;
  }

  .timeline-item::before {
    left: 62.5px;
    right: auto;
    border-left: 1px solid var(--glass-border);
    border-bottom: 1px solid var(--glass-border);
    border-top: none;
    border-right: none;
  }

  .timeline-item .actions {
    opacity: 1; /* Always show actions on mobile */
    flex-direction: column;
    align-items: flex-end !important;
    gap: 0.5rem;
  }

  .timeline-item .actions .form-check {
    margin-right: 0 !important;
  }
}
</style>
