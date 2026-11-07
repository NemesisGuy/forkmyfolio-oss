<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { getIconClass } from '~/utils/skillsUtils';
import type { Skill } from '~/utils/skillsUtils';

/**
 * @file app/components/SkillBadge.vue
 * @description A stylized badge displaying a skill with its icon and glass tooltip.
 * Ported from ForkMyFolio-frontend-vue/src/components/common/SkillBadge.vue.
 */

const props = defineProps<{
  skill: Skill;
}>();

const tooltipText = computed(() => {
  if (props.skill.description) {
    return props.skill.description;
  }
  if (props.skill.category) {
    return `Category: ${props.skill.category}`;
  }
  return props.skill.name;
});

// --- Tooltip Management ---
const badgeRef = ref<HTMLElement | null>(null);
let tooltipInstance: any = null;

onMounted(async () => {
  if (badgeRef.value) {
    const bootstrap = await import('bootstrap');
    tooltipInstance = new bootstrap.Tooltip(badgeRef.value, {
      container: 'body',
      trigger: 'hover',
      html: true,
    });
  }
});

onUnmounted(() => {
  tooltipInstance?.dispose();
});
</script>

<template>
  <span
    ref="badgeRef"
    :title="tooltipText"
    class="badge skill-badge d-flex align-items-center"
    data-bs-toggle="tooltip"
  >
    <i :class="[getIconClass(skill), 'me-2']"></i>
    {{ skill.name }}
  </span>
</template>

<style>
/* --- Global Tooltip Style Overrides --- */
/* This block is NOT scoped, so it will style tooltips everywhere */

.tooltip {
  /* Default "glass" style for light mode */
  --bs-tooltip-bg: var(--glass-bg);
  --bs-tooltip-color: var(--glass-text);
  --bs-tooltip-opacity: 1;
  --bs-tooltip-border-radius: 1rem; /* More pronounced rounded corners */
  /* Apply glassmorphism to the tooltip itself */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid var(--bs-gray-500);
}

/* Specific override for a solid dark tooltip in dark mode */
[data-bs-theme="dark"] .tooltip {
  --bs-tooltip-bg: var(--bs-dark);
  --bs-tooltip-color: var(--bs-light);
  border-color: var(--glass-border); /* Use a lighter border in dark mode */
  backdrop-filter: none; /* Disable the blur effect for a solid background */
  -webkit-backdrop-filter: none;
}

.tooltip-inner {
  padding: 0.5rem 1rem;
  font-weight: 500;
}
</style>

<style scoped>
/* --- Scoped Badge Styling --- */
/* Base styles for all skill badges */
.skill-badge {
  background-color: rgba(var(--bs-primary-rgb), 0.15);
  color: var(--bs-primary); /* Text and default icon color */
  border: 1px solid rgba(var(--bs-primary-rgb), 0.2);
  font-weight: 500;
  padding: 0.4em 0.75em;
  transition: all 0.2s ease-in-out;
  border-radius: 1rem; /* This creates the rounded "pill" shape */
  cursor: help; /* Indicates that it's hoverable for more info */
}

.skill-badge:hover {
  transform: translateY(-2px);
  background-color: rgba(var(--bs-primary-rgb), 0.25);
  border-color: rgba(var(--bs-primary-rgb), 0.3);
  box-shadow: 0 4px 10px rgba(var(--bs-primary-rgb), 0.2);
}

.skill-badge i {
  font-size: 1.1em;
  line-height: 1;
}
</style>
