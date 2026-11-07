<template>
  <div class="testimonials-page py-5">
    <div class="container">
      <div class="text-center mb-5">
        <h1 class="display-4 fw-bold animate-fade-in-up glass-text">
          <MessageSquareQuote aria-hidden="true" :size="48" /> Testimonials
        </h1>
        <p class="lead animate-fade-in-up glass-subtitle" style="animation-delay: 0.1s;">
          What colleagues and clients are saying about my work.
        </p>
      </div>

      <LoadingModal :visible="isLoading"/>

      <!-- A skeleton loader that mimics the new testimonial card style -->
      <div v-if="isLoading" class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        <div v-for="n in 6"
             :key="n"
             :style="{ 'animation-delay': (n * 0.05) + 's' }"
             class="col animate-fade-in-up">
          <div class="card glass-card glass-card-floating h-100">
            <div class="card-body d-flex flex-column">
              <div class="flex-grow-1">
                <div class="skeleton-line skeleton-subtitle mb-2"></div>
                <div class="skeleton-line skeleton-subtitle mb-2"></div>
                <div class="skeleton-line skeleton-grade" style="width: 70%;"></div>
              </div>
              <div class="mt-auto text-end">
                <div class="skeleton-line skeleton-title"
                     style="width: 60%; margin-left: auto;"></div>
                <div class="skeleton-line skeleton-subtitle"
                     style="width: 80%; margin-left: auto;"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <AlertTriangle class="text-warning mb-3" style="font-size: 3rem;" />
          <h5 class="card-title glass-title mb-3">Unable to Load Testimonials</h5>
          <p class="card-text glass-description">
            {{ error.message || 'Could not load testimonials. Please try again later.' }}
          </p>
        </div>
      </div>

      <!-- State for when a portfolio is loaded and has testimonials -->
      <div v-else-if="testimonials.length > 0"
           class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        <div v-for="(testimonial, index) in testimonials"
             :key="testimonial.uuid"
             :style="{ 'animation-delay': (index * 0.1 + 0.2) + 's' }"
             class="col animate-fade-in-up"
        >
          <div
            class="card glass-card glass-card-floating h-100 interactive-card-lift interactive-card-shadow-primary"
            role="button" tabindex="0"
            @click="selectTestimonial(testimonial)"
          >
            <div class="card-body d-flex flex-column">
              <Quote aria-hidden="true" class="card-quote-icon" />
              <figure class="mb-0 d-flex flex-column flex-grow-1">
                <blockquote class="blockquote mb-4 flex-grow-1">
                  <p class="glass-description">{{ testimonial.quote }}</p>
                </blockquote>
                <figcaption class="blockquote-footer mt-auto text-end">
                  <strong class="d-block glass-title">{{ testimonial.authorName }}</strong>
                  <cite :title="testimonial.authorTitle"
                        class="glass-subtitle">{{ testimonial.authorTitle }}</cite>
                </figcaption>
              </figure>
            </div>
          </div>
        </div>
      </div>

      <!-- State for when a portfolio is loaded but has no testimonials -->
      <div v-else class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <div class="empty-state-icon mb-4">
            <MessageSquareQuote />
          </div>
          <h4 class="card-title glass-title mb-3">No Testimonials Yet</h4>
          <!-- Generic message for public visitors -->
          <p v-if="!isOwner" class="card-text glass-subtitle mb-4">
            This user has not added any testimonials yet. Please check back later.
          </p>

          <!-- Helpful tip for the portfolio owner -->
          <div v-else class="alert alert-info mt-3">
            <p class="mb-1"><strong>Hey there!</strong> It looks like you don't have any testimonials
              visible on your public page.</p>
            <p class="mb-0">
              Go to your
              <router-link :to="{ name: 'my-testimonials', params: { slug: currentSlug } }">
                Testimonial Management
              </router-link>
              page to add new entries.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Details Modal -->
    <TestimonialDetailsModal
      :testimonial="selectedTestimonial"
      @close="closeModal"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch, type Ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {authService} from '@/services/authService.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import TestimonialDetailsModal from '@/components/public/TestimonialDetailsModal.vue';
import type { Portfolio, Testimonial, User } from '@/types';
import { MessageSquareQuote, AlertTriangle, Quote } from 'lucide-vue-next';

// Use the store to get reactive state.
const {
  portfolio,
  isLoading,
  error,
  currentSlug
} = usePublicPortfolioStore() as {
  portfolio: Ref<Portfolio | null>,
  isLoading: Ref<boolean>,
  error: Ref<Error | null>,
  currentSlug: Ref<string | null>
};

const route = useRoute();
const router = useRouter();

// The testimonials are now a computed property derived from the central store.
// FIX: The public page should only display testimonials that are marked as visible.
// The portfolio store contains all testimonials (visible and hidden), so we must
// filter them here on the client side.
const testimonials = computed(() => {

  return (portfolio.value?.testimonials || []).filter(t => t.visible);
});
// Check if the currently logged-in user is the owner of this portfolio.
const isOwner = computed(() => {
  const user = authService.user.value as User | null;
  return authService.isAuthenticated.value && user?.slug === currentSlug.value;
});

// --- Modal State & Route-Awareness ---
// We now sync the selected testimonial with a query parameter in the URL.
// This allows the browser's back button to close the modal naturally.
const selectedTestimonial = ref<Testimonial | null>(null);

// Watch for changes in the query parameters to show/hide the modal.
watch(() => route.query.testimonial, (uuid) => {
  if (uuid) {
    const found = testimonials.value.find(t => t.uuid === uuid);
    if (found) {
      selectedTestimonial.value = found;
    } else {
      selectedTestimonial.value = null; // Clear if UUID is invalid
    }
  } else {
    selectedTestimonial.value = null;
  }
}, { immediate: true });

const selectTestimonial = (testimonial: Testimonial) => {
  // Push the unique UUID to the current URL.
  // Using push creates a history entry, so 'Back' will naturally close the modal.
  router.push({
    query: { ...route.query, testimonial: testimonial.uuid }
  });
};

const closeModal = () => {
  // When closing, we remove the query parameter.
  // We use replace: true so that the 'History' doesn't become polluted
  // with multiple "closed" states if they open/close multiple times.
  const newQuery = { ...route.query };
  delete newQuery.testimonial;

  // However, if we arrived here via a 'push', we might want to just go back.
  // But if they shared the link, going back might take them away from the site.
  // So we just replace the state to clear the URL.
  router.replace({ query: newQuery });
};
</script>

<style scoped>
.testimonials-page {
  overflow-x: hidden;
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out forwards;
}

.card[role="button"] {
  cursor: pointer;
}

/* Watermark Quote Icon */
.card-quote-icon {
  position: absolute;
  top: -10px;
  left: -10px;
  font-size: 8rem;
  color: var(--glass-shimmer);
  transform: rotate(-15deg);
  z-index: 0;
  pointer-events: none;
  opacity: 0.5;
}

/* Card Body */
.card-body {
  z-index: 1;
}

/* Blockquote Styling */
.blockquote p {
  font-size: 1.1rem;
  font-style: italic;
  line-height: 1.7;
}

.blockquote-footer {
  font-size: 0.9rem;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--bs-primary);
  opacity: 0.6;
}
</style>
