<template>
  <div class="user-testimonials-page py-5 flex-grow-1">
    <div class="container">
      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center mb-4 animate-fade-in-up">
        <h2 class="mb-0 glass-text">Manage My Testimonials</h2>
        <button class="btn btn-primary interactive-lift" @click="openAddModal">
          <PlusCircle class="me-2" />Add New Testimonial
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
        :message="`Are you sure you want to delete the testimonial from '${testimonialToDelete?.authorName}'?`"
        title="Confirm Deletion"
        @close="testimonialToDelete = null"
        @confirm="handleDeleteTestimonial"
      />

      <!-- REFACTOR: The modal is now a self-contained component -->
      <TestimonialFormModal
        ref="testimonialFormModalRef"
        :is-editing="isEditing"
        :testimonial="currentTestimonialForModal"
        @save="handleSaveTestimonial"
      />

      <!-- Testimonials List -->
      <div v-if="!isLoading && testimonials.length > 0" class="card glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <ul class="list-group list-group-flush">
          <li v-for="testimonial in testimonials" :key="testimonial.uuid" class="list-group-item">
            <div class="d-flex justify-content-between align-items-start">
              <blockquote class="blockquote mb-0">
                <p class="quote-text">“{{ testimonial.quote }}”</p>
                <footer class="blockquote-footer mt-2">{{ testimonial.authorName }} <cite
                  v-if="testimonial.authorTitle"
                  :title="testimonial.authorTitle">{{ testimonial.authorTitle }}</cite></footer>
              </blockquote>
              <div class="actions d-flex align-items-center">
                <VisibilityToggle :is-loading="!!testimonial.isVisibilityLoading"
                                  :visible="!!testimonial.visible"
                                  class="me-3" @toggle="handleVisibilityToggle(testimonial)"/>
                <button class="btn btn-sm btn-outline-primary me-2"
                        title="Edit Testimonial" @click="openEditModal(testimonial)">
                  <Pencil />
                </button>
                <button class="btn btn-sm btn-outline-danger"
                        title="Delete Testimonial" @click="openDeleteConfirm(testimonial)">
                  <Trash2 />
                </button>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <!-- Empty State -->
      <div v-else-if="!isLoading" class="text-center p-5 glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <div class="empty-state-icon mb-4">
          <MessageSquareQuote />
        </div>
        <h4 class="glass-title">No Testimonials Found</h4>
        <p class="glass-subtitle">You haven't added any testimonials yet. Click the button above to
          get started!</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { PlusCircle, Pencil, Trash2, MessageSquareQuote } from 'lucide-vue-next';
import { testimonialsApi } from '@/services/api/user.api';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore';
import { authService } from '@/services/authService';
import type { Testimonial } from '@/types';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import VisibilityToggle from '@/components/common/VisibilityToggle.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';
import TestimonialFormModal, { type TestimonialFormState } from '@/components/user/TestimonialFormModal.vue';

interface TestimonialWithLoading extends Testimonial {
  isVisibilityLoading: boolean;
}

// --- State ---
const testimonials = ref<TestimonialWithLoading[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);
const testimonialToDelete = ref<Testimonial | null>(null);

// --- Modal State ---
const testimonialFormModalRef = ref<{ show: () => void; hide: () => void } | null>(null);
const confirmModalRef = ref<{ show: () => void; hide: () => void } | null>(null);
const isEditing = ref(false);
const currentTestimonialForModal = ref<Testimonial | null>(null);

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
  await fetchTestimonials();
});

// --- Data Fetching ---
const fetchTestimonials = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    const rawTestimonials = await testimonialsApi.getAll() as Testimonial[];
    testimonials.value = rawTestimonials.map(t => ({
      ...t,
      isVisibilityLoading: false
    })) as TestimonialWithLoading[];
  } catch (err: unknown) {
    console.error("Failed to fetch user testimonials:", err);
    error.value = err instanceof Error ? err.message : 'An unexpected error occurred while fetching your testimonials.';
  } finally {
    isLoading.value = false;
  }
};

// --- Modal Handling ---
const openAddModal = () => {
  isEditing.value = false;
  currentTestimonialForModal.value = null; // Signal to the modal to use its initial state
  testimonialFormModalRef.value?.show();
};

const openEditModal = (testimonial: Testimonial) => {
  isEditing.value = true;
  // Pass a deep copy to the modal to prevent direct mutation of the list item
  currentTestimonialForModal.value = JSON.parse(JSON.stringify(testimonial));
  testimonialFormModalRef.value?.show();
};

const openDeleteConfirm = (testimonial: Testimonial) => {
  testimonialToDelete.value = testimonial;
  confirmModalRef.value?.show();
};

// --- CRUD Operations ---
const buildPayload = (testimonial: Testimonial | TestimonialFormState): Partial<Testimonial> => {
  return {
    quote: testimonial.quote,
    authorName: testimonial.authorName,
    authorTitle: testimonial.authorTitle || undefined,
    visible: testimonial.visible,
  };
};

const handleSaveTestimonial = async (testimonialData: TestimonialFormState) => {
  isLoading.value = true;
  error.value = null;
  testimonialFormModalRef.value?.hide();

  try {
    // This helper function creates a clean data object for the API.
    const payload = buildPayload(testimonialData);

    if (isEditing.value) {
      // Update an existing testimonial
      await testimonialsApi.update(testimonialData.uuid, payload);
      successMessage.value = `Testimonial from '${payload.authorName}' was updated successfully.`;
    } else {
      // Create a new testimonial
      await testimonialsApi.create(payload);
      successMessage.value = `Testimonial from '${payload.authorName}' was added successfully.`;
    }

    await fetchTestimonials(); // Refresh the list with the latest data
    await refreshPublicData();
  } catch (err: unknown) {
    console.error("Failed to save testimonial:", err);
    error.value = err instanceof Error ? err.message : 'An error occurred while saving the testimonial.';
  } finally {
    isLoading.value = false;
  }
};

const handleDeleteTestimonial = async () => {
  const target = testimonialToDelete.value;
  if (!target || !target.uuid) return;

  isLoading.value = true;
  error.value = null;
  confirmModalRef.value?.hide();
  try {
    await testimonialsApi.remove(target.uuid);
    // Optimistically remove from the local array for a faster UI response
    testimonials.value = testimonials.value.filter(t => t.uuid !== target.uuid);
    await refreshPublicData();
    successMessage.value = `Testimonial from '${target.authorName}' was deleted successfully.`;
  } catch (err: unknown) {
    console.error("Failed to delete testimonial:", err);
    error.value = err instanceof Error ? err.message : 'An error occurred while deleting the testimonial.';
  } finally {
    isLoading.value = false;
  } // The @close event on the modal will reset testimonialToDelete.
};

const handleVisibilityToggle = async (testimonial: TestimonialWithLoading) => {
  testimonial.isVisibilityLoading = true;
  const originalVisibility = testimonial.visible;
  testimonial.visible = !testimonial.visible; // Optimistic update

  try {
    const payload = buildPayload(testimonial);
    if (!testimonial.uuid) throw new Error("Testimonial UUID is missing.");
    await testimonialsApi.update(testimonial.uuid, payload);
    successMessage.value = `Visibility for testimonial from '${testimonial.authorName}' updated.`;
    await refreshPublicData();
  } catch (err: unknown) {
    testimonial.visible = originalVisibility; // Revert on error
    console.error("Failed to update visibility:", err);
    error.value = err instanceof Error ? err.message : 'An error occurred while updating visibility.';
  } finally {
    testimonial.isVisibilityLoading = false;
  }
};
</script>

<style scoped>
.list-group-item {
  background-color: transparent;
  border-bottom: 1px solid var(--glass-border);
  color: var(--glass-text);
  transition: background-color 0.3s ease;
  padding: 1.5rem;
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
  margin-left: 1.5rem;
}

.list-group-item:hover .actions {
  opacity: 1;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--glass-text);
}

.form-check-input {
  cursor: pointer;
}

.blockquote {
  flex-grow: 1;
}

.quote-text {
  font-style: italic;
  font-size: 1.1rem;
  color: var(--glass-text);
  margin-bottom: 0.5rem;
}

.blockquote-footer {
  color: var(--glass-text-secondary);
}

.blockquote-footer cite {
  color: var(--glass-text-secondary);
  font-style: normal;
}
</style>
