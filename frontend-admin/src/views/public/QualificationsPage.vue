<template>
  <div class="qualifications-page py-5">
    <div class="container">
      <div class="text-center mb-5">
        <h1 class="display-4 fw-bold animate-fade-in-up glass-text">
          <GraduationCap class="me-2" aria-hidden="true" :size="48" style="vertical-align: middle;"/> Qualifications & Certifications
        </h1>
        <p class="lead text-muted animate-fade-in-up glass-subtitle"
           style="animation-delay: 0.1s;">
          My academic achievements and professional certifications.
        </p>
      </div>

      <LoadingModal :visible="isLoading"/>

      <!-- Skeleton loader -->
      <div v-if="isLoading" class="row row-cols-1 row-cols-lg-2 g-4">
        <div v-for="n in 4"
             :key="n"
             :style="{ 'animation-delay': (n * 0.05) + 's' }"
             class="col animate-fade-in-up">
          <div class="card glass-card glass-card-floating h-100">
            <div class="card-body d-flex align-items-center p-4">
              <div class="skeleton-icon me-4"></div>
              <div class="flex-grow-1">
                <div class="skeleton-line skeleton-title"></div>
                <div class="skeleton-line skeleton-subtitle"></div>
                <div class="skeleton-line skeleton-grade"></div>
              </div>
              <div class="skeleton-year-badge ms-3"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <AlertTriangle class="text-warning mb-3" style="font-size: 3rem;" />
          <h5 class="card-title glass-title mb-3">Unable to Load Qualifications</h5>
          <p class="card-text glass-description">
            Could not load qualifications data. Please try again later.
          </p>
        </div>
      </div>

      <!-- Content -->
      <div v-else-if="qualifications.length > 0" class="row row-cols-1 row-cols-lg-2 g-4">
        <div v-for="(qual, index) in qualifications" :key="qual.uuid"
             :style="{ 'animation-delay': (index * 0.1) + 0.2 + 's' }"
             class="col animate-fade-in-up">
          <div
            :aria-label="`View details for ${qual.qualificationName}`"
            class="card glass-card glass-card-floating h-100 interactive-card-lift interactive-card-shadow-primary position-relative"
            role="button" tabindex="0"
            @click="selectQualification(qual)">
            <div class="card-body d-flex align-items-center p-4 position-relative">
              <div class="qual-icon me-4">
                <img v-if="qual.institutionLogoUrl" :alt="`${qual.institutionName} Logo`"
                     :src="qual.institutionLogoUrl" class="institution-logo">
                <Building2 v-else />
              </div>
              <div class="flex-grow-1">
                <h5 class="card-title mb-2 glass-title">
                  {{ qual.qualificationName }}
                </h5>
                <h6 class="card-subtitle mb-2 glass-subtitle">
                  <MapPin class="me-1" />{{ qual.institutionName }}
                </h6>
                <p v-if="qual.fieldOfStudy" class="card-text mt-2 mb-0 small glass-description">
                  <BookOpen class="me-1 text-info" />
                  {{ qual.fieldOfStudy }}
                </p>
              </div>
              <div class="year-badge ms-3">
                <span v-if="qual.stillStudying">{{ qual.startYear }} - Present</span>
                <span v-else>{{ qual.completionYear || qual.startYear }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <div class="empty-state-icon mb-4">
            <BadgeCheck />
          </div>
          <h4 class="card-title glass-title mb-3">Qualifications Coming Soon</h4>
          <!-- Generic message for public visitors -->
          <p v-if="!isOwner" class="card-text glass-subtitle mb-4">
            The owner is currently updating their degrees and certifications. Please check back later.
          </p>

          <!-- Helpful tip for the portfolio owner -->
          <div v-else class="alert alert-info mt-3">
            <p class="mb-1"><strong>Hey there!</strong> It looks like you don't have any
              qualifications visible on your public page.</p>
            <p class="mb-0">
              Go to your
              <router-link :to="{ name: 'my-qualifications', params: { slug: currentSlug } }">
                Qualification Management
              </router-link>
              page to add new entries or make existing ones visible.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- The details modal, which is now consistent with other modals in the app. -->
    <QualificationDetailsModal
      :qualification="selectedQualification"
      @close="closeModal"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, ref, type Ref} from 'vue';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {authService} from '@/services/authService.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
// REFACTOR: Import the new, reusable modal component.
import QualificationDetailsModal from '@/components/public/QualificationDetailsModal.vue';
import type { Portfolio, Qualification, User } from '@/types';
import { AlertTriangle, Building2, MapPin, BookOpen, BadgeCheck, GraduationCap } from 'lucide-vue-next';

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

// Check if the currently logged-in user is the owner of this portfolio.
const isOwner = computed(() => {
  const user = authService.user.value as User | null;
  return authService.isAuthenticated.value && user?.slug === currentSlug.value;
});

const qualifications = computed(() => {
  // FIX: The public page should only display qualifications that are marked as visible.
  // The portfolio store contains all qualifications, so we filter them here.

  const quals = (portfolio.value?.qualifications || []).filter(q => q.visible);
  // CORRECTED: More robust sorting
  return [...quals].sort((a, b) => {
    const yearA = a.stillStudying ? Infinity : a.completionYear || a.startYear || 0;
    const yearB = b.stillStudying ? Infinity : b.completionYear || b.startYear || 0;
    return yearB - yearA;
  });
});

// --- Modal State & Focus Management ---
// REFACTOR: Manually manage focus to prevent accessibility issues.
// We store the element that triggered the modal and return focus to it on close.
const selectedQualification = ref<Qualification | null>(null);
let lastFocusedElement: HTMLElement | null = null;

const selectQualification = (qual: Qualification) => {
  lastFocusedElement = document.activeElement as HTMLElement; // Store the focused element
  selectedQualification.value = qual;
};

const closeModal = () => {
  selectedQualification.value = null;
  lastFocusedElement?.focus(); // Return focus to the trigger
  lastFocusedElement = null;
};
</script>

<style scoped>
/* --- Page Styling --- */
.qualifications-page {
  overflow-x: hidden;
}

.card[role="button"] {
  cursor: pointer;
}

.qual-icon {
  font-size: 2.5rem;
  color: var(--bs-primary);
  opacity: 0.8;
  width: 40px; /* Ensure consistent width for icon/logo container */
  text-align: center;
}

/* ADDED: Styles for the institution logo */
.institution-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 0.25rem;
  background-color: rgba(var(--bs-body-color-rgb), 0.1);
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--bs-primary);
  opacity: 0.6;
}

.skeleton-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.skeleton-year-badge {
  width: 60px;
  height: 30px;
  border-radius: 50rem;
}

.skeleton-grade {
  width: 40%;
  height: 14px;
  margin-top: 0.5rem;
}
</style>
