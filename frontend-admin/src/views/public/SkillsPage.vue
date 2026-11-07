<template>
  <div ref="pageRef" class="skills-page py-5">
    <div class="container-fluid">
      <!-- Hero Section -->
      <div class="text-center mb-5">
        <h1 class="display-4 fw-bold animate-fade-in-up glass-text">
          <Wrench aria-hidden="true" :size="48" /> Skills & Proficiencies
        </h1>
        <p class="lead animate-fade-in-up glass-subtitle" style="animation-delay: 0.1s;">
          A curated list of my technical competencies and tools I love to use.
        </p>
      </div>

      <LoadingModal :visible="isLoading"/>

      <!-- Skeleton loader -->
      <div v-if="isLoading" class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-4">
        <div v-for="n in 8" :key="n" class="col">
          <div class="card glass-card glass-card-floating h-100 text-center">
            <div
              class="card-body py-3 d-flex flex-column justify-content-center align-items-center">
              <div class="skeleton-icon mb-3"></div>
              <div class="skeleton-line skeleton-title" style="width: 70%;"></div>
              <div class="skeleton-line skeleton-subtitle" style="width: 80%; height: 8px;"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <AlertTriangle class="text-warning mb-3" style="font-size: 3rem;" />
          <h5 class="card-title glass-title mb-3">Unable to Load Skills</h5>
          <p class="card-text glass-description">
            Could not load skills data. Please try again later.
          </p>
        </div>
      </div>

      <!-- Content -->
      <div v-else-if="groupedByLevel.length > 0">
        <div
          v-for="(levelGroup, levelIndex) in groupedByLevel"
          :key="levelGroup.name"
          :style="{ 'animation-delay': (levelIndex * 0.2) + 's' }"
          class="mb-5 animate-fade-in-up text-center"
        >
          <!-- FIX: Increased bottom margin from mb-4 to mb-5 for more spacing -->
          <h2 :data-level="levelGroup.levelKey"
              class="display-5 mb-5 fw-bold glass-text level-heading">
            {{ levelGroup.name }}
          </h2>

          <div
            class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 row-cols-xl-6 g-3 skill-col">
            <div
              v-for="(skill, skillIndex) in levelGroup.skills"
              :key="skill.uuid"
              :style="{ 'animation-delay': (levelIndex * 0.2 + skillIndex * 0.05) + 0.2 + 's' }"
              class="col animate-fade-in-up"
            >
              <div
                class="card glass-card glass-card-floating h-100 text-center shadow-sm interactive-card-lift interactive-card-shadow-primary"
                role="button"
                tabindex="0"
                @click="selectSkill(skill)"
              >
                <div v-if="skill.category" :title="skill.category" class="category-icon"
                     data-bs-toggle="tooltip">
                  <i :class="getCategoryIcon(skill.category)"></i>
                </div>

                <div
                  class="card-body py-3 d-flex flex-column justify-content-start align-items-center">
                  <div class="skill-icon mb-3">
                    <i :class="getIconClass(skill)"/>
                  </div>
                  <h6 class="card-title">{{ skill.name }}</h6>
                  <p v-if="skill.description" class="card-text small text-body-secondary px-2">
                    {{ skill.description }}</p>
                  <p v-else class="card-text small text-body-secondary px-2">&nbsp;</p>
                  <!-- FIX: Removed .toLowerCase() to match the uppercase CSS selectors -->
                  <div
                    :data-level="skill.level"
                    :title="skill.level.charAt(0).toUpperCase() + skill.level.slice(1).toLowerCase()"
                    class="proficiency-indicator"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <div class="empty-state-icon mb-4">
            <Wrench />
          </div>
          <h4 class="card-title glass-title mb-3">No Skills Yet</h4>

          <!-- Generic message for public visitors -->
          <p v-if="!isOwner" class="card-text glass-subtitle mb-4">
            The toolbox is being organized. Please check back later for a list of skills.
          </p>

          <!-- Helpful tip for the portfolio owner -->
          <div v-else class="alert alert-info mt-3">
            <p class="mb-1"><strong>Hey there!</strong> It looks like you don't have any skills
              visible on your public page.</p>
            <p class="mb-0">
              Go to your
              <router-link :to="{ name: 'my-skills', params: { slug: currentSlug } }">Skill
                Management
              </router-link>
              page to add new skills or make existing ones visible.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Details Modal -->
    <SkillDetailsModal
      :skill="selectedSkill"
      @close="closeModal"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, onUpdated, ref, type Ref} from 'vue';
import { Wrench, AlertTriangle } from 'lucide-vue-next';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {authService} from '@/services/authService.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
// REFACTOR: Import business logic from centralized services for consistency and maintainability.
import {getIconClass, getCategoryIcon} from '@/services/iconService.js';
import SkillDetailsModal from '@/components/public/SkillDetailsModal.vue';
import {groupSkillsByLevel} from '@/services/skillsService.js';
import type { Portfolio, Skill, User } from '@/types';

import {Tooltip} from 'bootstrap';

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
// FIX: The portfolio store populates the `userSkills` property, not `skills`.
// This was the reason no data was being displayed on the page.
// By changing `portfolio.value?.skills` to `portfolio.value?.userSkills`,
// the component now correctly reads the skill data fetched from the API.
// FIX: The public page should only display skills that are marked as visible.
// The portfolio store contains all user skills, so we filter them here.

const skills = computed(() => (portfolio.value?.userSkills || []).filter(s => s.visible));

// Check if the currently logged-in user is the owner of this portfolio.
const isOwner = computed(() => {
  const user = authService.user.value as User | null;
  return authService.isAuthenticated.value && user?.slug === currentSlug.value;
});

// --- Modal State & Focus Management ---
// REFACTOR: Manually manage focus to prevent accessibility issues.
// We store the element that triggered the modal and return focus to it on close.
const selectedSkill = ref<Skill | null>(null);
let lastFocusedElement: HTMLElement | null = null;

const selectSkill = (skill: Skill) => {
  lastFocusedElement = document.activeElement as HTMLElement; // Store the focused element
  selectedSkill.value = skill;
};

const closeModal = () => {
  selectedSkill.value = null;
  lastFocusedElement?.focus(); // Return focus to the trigger
  lastFocusedElement = null;
};

// --- Tooltip Management ---
// REFACTOR: The tooltip initialization logic has been improved to be more performant and robust.
// Instead of querying the entire document, it now queries only within this component's scope.
// It also properly manages the lifecycle of each tooltip instance to prevent memory leaks.
const pageRef = ref<HTMLElement | null>(null);
let tooltipInstances: Tooltip[] = [];

const manageTooltips = () => {
  // 1. Dispose of any existing tooltips this component has created.
  tooltipInstances.forEach(t => t.dispose());
  tooltipInstances = [];

  // 2. Find all tooltip triggers within this component's template.
  if (pageRef.value) {
    const tooltipTriggerList = pageRef.value.querySelectorAll('[data-bs-toggle="tooltip"]');
    // 3. Create new Tooltip instances and store them.
    tooltipInstances = Array.from(tooltipTriggerList).map(el => new Tooltip(el, {
      container: 'body',
      trigger: 'hover'
    }));
  }
};

onMounted(manageTooltips);
onUpdated(manageTooltips);
onUnmounted(() => tooltipInstances.forEach(t => t.dispose()));

// REFACTOR: The grouping logic is now handled by the centralized skillsService,
// and icon logic is handled by iconService. This component is now much simpler.
const groupedByLevel = computed(() => groupSkillsByLevel(skills.value) as { name: string, levelKey: string, skills: Skill[] }[]);
</script>

<style scoped>
.skills-page {
  overflow-x: hidden;
}

.card[role="button"] {
  cursor: pointer;
}

.level-heading {
  position: relative;
  display: inline-block;
  padding-bottom: 0.75rem;
}

.level-heading::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 4px;
  border-radius: 2px;
}

.level-heading[data-level='EXPERT']::after {
  background: linear-gradient(90deg, var(--bs-success), var(--bs-success-text-emphasis));
}

.level-heading[data-level='ADVANCED']::after {
  background: linear-gradient(90deg, var(--bs-info), var(--bs-info-text-emphasis));
}

.level-heading[data-level='INTERMEDIATE']::after {
  background: linear-gradient(90deg, var(--bs-primary), var(--bs-primary-text-emphasis));
}

.level-heading[data-level='BEGINNER']::after {
  background: linear-gradient(90deg, var(--bs-warning), var(--bs-warning-text-emphasis));
}

.skill-icon {
  font-size: 2rem;
  /* The animation on hover provides a more engaging effect than a simple transition. */
}

.card:hover .skill-icon {
  /* Add the fa-beat-fade animation on hover for a more dynamic effect. */
  animation: beat-fade-effect 1s ease-in-out infinite;
}

/*
  FIX: The selector is now more specific to target only the description text inside skill cards.
  This prevents the style from incorrectly applying to the empty state message, which also uses the .card-text class.
*/
.skill-col .card-text {
  font-size: 0.75rem;
  line-height: 1.4;
  min-height: 2.8em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: auto;
}

.proficiency-indicator {
  width: 80%;
  height: 6px;
  margin-top: 0.5rem;
  border-radius: 4px;
  /* FIX: Use a theme-aware background color for better contrast in light mode */
  background-color: var(--bs-secondary-bg);
  overflow: hidden;
  position: relative;
  margin-top: auto;
}

.proficiency-indicator::after {
  content: '';
  position: absolute;
  left: 0;
  height: 100%;
  width: 0;
  border-radius: 4px;
  animation: fill-bar 1s ease-out 0.5s forwards;
}

.proficiency-indicator[data-level='EXPERT']::after {
  --target-width: 100%;
  background: linear-gradient(90deg, var(--bs-success), var(--bs-success-text-emphasis));
  box-shadow: 0 0 8px var(--bs-success);
}

.proficiency-indicator[data-level='ADVANCED']::after {
  --target-width: 80%;
  background: linear-gradient(90deg, var(--bs-info), var(--bs-info-text-emphasis));
  box-shadow: 0 0 8px var(--bs-info);
}

.proficiency-indicator[data-level='INTERMEDIATE']::after {
  --target-width: 60%;
  background: linear-gradient(90deg, var(--bs-primary), var(--bs-primary-text-emphasis));
  box-shadow: 0 0 8px var(--bs-primary);
}

.proficiency-indicator[data-level='BEGINNER']::after {
  --target-width: 35%;
  background: linear-gradient(90deg, var(--bs-warning), var(--bs-warning-text-emphasis));
  box-shadow: 0 0 8px var(--bs-warning);
}

.card:hover .proficiency-indicator::after {
  animation: reset-and-fill 1.2s ease-out forwards;
}

.skill-col .card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.category-icon {
  position: absolute;
  top: 10px;
  right: 12px;
  font-size: 1rem;
  color: var(--bs-secondary-color);
  opacity: 0.5;
  transition: all 0.3s ease;
  cursor: help;
  z-index: 10;
}

.card:hover .category-icon {
  opacity: 1;
  transform: scale(1.1);
}

/* --- Performance --- */
.card {
  will-change: transform, box-shadow;
}

@keyframes fill-bar {
  to {
    width: var(--target-width);
  }
}


@keyframes reset-and-fill {
  0% {
    width: 0;
  }
  20% {
    width: 0;
  }
  100% {
    width: var(--target-width);
  }
}

/* Add the keyframes for the beat-fade animation */
@keyframes beat-fade-effect {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    /* This now includes the more pronounced "grow" effect you wanted */
    transform: scale(1.2);
    opacity: 0.4;
  }
}
</style>
