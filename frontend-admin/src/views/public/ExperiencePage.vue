<template>
  <div class="experience-page py-5">
    <div class="container">
      <!-- Centered and animated hero section -->
      <div class="text-center mb-5">
        <h1 class="display-4 fw-bold animate-fade-in-up glass-text"><Briefcase class="me-2" aria-hidden="true" :size="48" style="vertical-align: middle;"/> Work Experience</h1>
        <p class="lead animate-fade-in-up glass-subtitle" style="animation-delay: 0.1s;">
          A timeline of my professional journey and accomplishments.
        </p>
      </div>

      <!-- The glassmorphic modal will overlay everything while loading -->
      <LoadingModal :visible="isLoading"/>

      <!-- A skeleton loader that mimics the new timeline style -->
      <div v-if="isLoading" class="timeline">
        <div v-for="n in 3" :key="n" class="timeline-item">
          <div class="timeline-content card glass-card glass-card-floating h-100">
            <div class="card-body">
              <div class="skeleton-line skeleton-title"></div>
              <div class="skeleton-line skeleton-subtitle"></div>
              <div class="skeleton-line skeleton-grade"
                   style="width: 50%; margin-top: 1rem; margin-bottom: 1rem;"></div>
              <div class="skeleton-line skeleton-subtitle" style="width: 90%;"></div>
              <div class="skeleton-line skeleton-subtitle" style="width: 75%;"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Error state with glassmorphic styling -->
      <div v-else-if="error" class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <AlertTriangle class="text-warning mb-3" style="font-size: 3rem;" />
          <h5 class="card-title glass-title mb-3">Unable to Load Experience</h5>
          <p class="card-text glass-description">
            Could not load work experience data. Please try again later.
          </p>
        </div>
      </div>

      <div v-else-if="experiences.length > 0" class="timeline">
        <div v-for="(exp, index) in experiences" :key="exp.uuid"
             :style="{ 'animation-delay': (index * 0.15) + 0.2 + 's' }"
             class="timeline-item animate-fade-in-up">
          <div class="timeline-content card glass-card glass-card-floating h-100 interactive-card-lift interactive-card-shadow-primary"
               role="button"
               tabindex="0"
               @click="selectExperience(exp)"
          >
            <div class="card-body">
              <div class="d-flex align-items-start mb-3">
                <a v-if="exp.companyUrl" :href="exp.companyUrl" class="company-logo-link"
                   rel="noopener noreferrer" target="_blank">
                  <img v-if="exp.companyLogoUrl" :alt="`${exp.companyName} Logo`"
                       :src="exp.companyLogoUrl" class="company-logo me-3">
                  <div v-else class="company-logo-placeholder me-3"><Building2 />
                  </div>
                </a>
                <div v-else class="company-logo-link">
                  <img v-if="exp.companyLogoUrl" :alt="`${exp.companyName} Logo`"
                       :src="exp.companyLogoUrl" class="company-logo me-3">
                  <div v-else class="company-logo-placeholder me-3"><Building2 />
                  </div>
                </div>

                <div class="flex-grow-1">
                  <h5 class="card-title glass-title">{{ exp.jobTitle }}</h5>
                  <h6 class="card-subtitle mb-2 glass-subtitle">{{ exp.companyName }}</h6>
                  <div class="small text-muted">
                    <span><Calendar class="me-1" />{{ formatDate(exp.startDate) }} - {{
                        exp.endDate ? formatDate(exp.endDate) : 'Present'
                      }}</span>
                    <span class="mx-2">|</span>
                    <span><MapPin class="me-1" />{{
                        exp.location
                      }} ({{ exp.locationType?.replace('_', ' ') }})</span>
                  </div>
                </div>
              </div>

              <!-- Phase 7.4 §10 (self-XSS hardening): owner text rendered as TEXT,
                   never as HTML (v-html executes owner-supplied onerror/onload
                   handlers for every visitor). Line breaks come from pre-wrap CSS. -->
              <p v-if="exp.description" class="card-text glass-description experience-description">{{ exp.description
                }}</p>

              <div v-if="exp.achievements" class="mt-3">
                <h6 class="achievements-title">Key Achievements</h6>
                <div class="achievements-text">{{ exp.achievements }}</div>
              </div>

              <div v-if="exp.skills && exp.skills.length > 0" class="mt-4">
                <h6 class="skills-title">Skills Used</h6>
                <div class="d-flex flex-wrap gap-2">
                  <SkillBadge
                    v-for="skill in exp.skills"
                    :key="skill.skillId"
                    :skill="skill"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Enhanced empty state with a helpful tip for the owner -->
      <div v-else class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <div class="empty-state-icon mb-4">
            <Briefcase />
          </div>
          <h4 class="card-title glass-title mb-3">No Work Experience Yet</h4>

          <!-- Generic message for public visitors -->
          <p v-if="!isOwner" class="card-text glass-subtitle mb-4">
            Work history has not been added yet. Please check back later.
          </p>

          <!-- Helpful tip for the portfolio owner -->
          <div v-else class="alert alert-info mt-3">
            <p class="mb-1"><strong>Hey there!</strong> It looks like you don't have any work
              experience visible on your public page.</p>
            <p class="mb-0">
              Go to your
              <router-link :to="{ name: 'my-experience', params: { slug: currentSlug } }">Experience
                Management
              </router-link>
              page to add new entries or make existing ones visible.
            </p>
          </div>
        </div>
      </div>

      <!-- Details Modal -->
      <ExperienceDetailsModal
        :experience="selectedExperience"
        @close="closeModal"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, type Ref} from 'vue';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {authService} from '@/services/authService.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import SkillBadge from '@/components/common/SkillBadge.vue';
import ExperienceDetailsModal from '@/components/public/ExperienceDetailsModal.vue';
import {formatDisplayDate as formatDate} from '@/utils/dateUtils.js';
import { AlertTriangle, Building2, Calendar, MapPin, Briefcase } from 'lucide-vue-next';
import type { Portfolio, Experience, User } from '@/types';

// Get all necessary reactive properties from the store.
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

// Experiences are a computed property from the store's portfolio.
const experiences = computed(() => {

  const rawExperiences = (portfolio.value?.experiences || []).filter((e: Experience) => e.visible);

  // We just need to sort the experiences by date and return them.
  return [...rawExperiences].sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime());
});

// Check if the currently logged-in user is the owner of this portfolio.
const isOwner = computed(() => {
  const user = authService.user.value as User | null;
  return authService.isAuthenticated.value && user?.slug === currentSlug.value;
});

// --- Modal State & Focus Management ---
// REFACTOR: Manually manage focus to prevent accessibility issues.
// We store the element that triggered the modal and return focus to it on close.
const selectedExperience = ref<Experience | null>(null);
let lastFocusedElement: HTMLElement | null = null;

const selectExperience = (experience: Experience) => {
  lastFocusedElement = document.activeElement as HTMLElement; // Store the focused element
  selectedExperience.value = experience;
};

const closeModal = () => {
  selectedExperience.value = null;
  lastFocusedElement?.focus(); // Return focus to the trigger
  lastFocusedElement = null;
};
</script>

<style scoped>
/* --- Page Styling --- */
.timeline-content[role="button"] {
  cursor: pointer;
}

.experience-page {
  overflow-x: hidden;
}

.experience-description {
  white-space: pre-wrap;
  word-wrap: break-word;
}

.achievements-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 0.9rem;
  padding-left: 1rem;
  border-left: 3px solid var(--bs-primary);
  color: var(--glass-text-secondary);
}

.achievements-title, .skills-title {
  font-weight: 600;
  color: var(--glass-text-secondary);
  margin-bottom: 0.5rem;
}

.company-logo, .company-logo-placeholder {
  width: 50px;
  height: 50px;
  border-radius: 0.5rem;
  object-fit: contain;
  flex-shrink: 0;
  background-color: rgba(var(--bs-body-color-rgb), 0.05);
  border: 1px solid var(--glass-border);
}

.company-logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: var(--glass-text-secondary);
}

.company-logo-link {
  display: block;
  transition: transform 0.2s ease;
}

.company-logo-link:hover {
  transform: scale(1.05);
}

/* --- Link Transitions & Performance --- */
.timeline-content {
  will-change: transform, box-shadow;
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
  /* FIX: The previous color was not visible enough. Using a text-based color variable
     ensures it has proper contrast in both light and dark modes. */
  background-image: linear-gradient(to bottom, transparent, var(--glass-text-secondary), transparent);
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
}

/* The little arrow pointing from the card to the timeline */
.timeline-item::before {
  content: '';
  position: absolute;
  top: 28px;
  width: 15px;
  height: 15px;
  background: var(--glass-bg); /* Use the glass background for consistency */
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transform: translateY(-50%) rotate(45deg);
  z-index: 0;
}

.timeline-item:nth-child(odd)::before {
  right: 32.5px;
  /* FIX: Use a theme-aware color with better contrast for the arrow. */
  border-top: 1px solid var(--glass-text-secondary);
  border-right: 1px solid var(--glass-text-secondary);
  border-left: none;
  border-bottom: none;
}

.timeline-item:nth-child(even)::before {
  left: 32.5px;
  /* FIX: Use a theme-aware color with better contrast for the arrow. */
  border-left: 1px solid var(--glass-text-secondary);
  border-bottom: 1px solid var(--glass-text-secondary);
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
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--bs-primary);
  opacity: 0.6;
}
</style>
