<template>
  <!-- THIS IS THE FIX: The component is now a full-fledged, animated glass card -->
  <div
    class="card glass-card shimmering animate-fade-in-up interactive-card-lift interactive-card-shadow-primary">
    <div class="card-body text-center p-5">
      <i v-if="iconClass" :class="iconClasses"></i>
      <h2 class="display-6">{{ title }}</h2>
      <p class="lead text-muted">{{ message }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';

const props = defineProps<{
  /**
   * The main title to display.
   */
  title?: string;
  /**
   * The descriptive message below the title.
   */
  message?: string;
  /**
   * The icon CSS class to use. Migrated from Bootstrap Icons to generic/Lucide classes.
   */
  iconClass?: string;
}>();

// Set defaults
const title = computed(() => props.title ?? 'Nothing to see here yet!');
const message = computed(() => props.message ?? 'Please check back later.');
const iconClass = computed(() => props.iconClass ?? '');

// Computes the full list of classes for the icon element
const iconClasses = computed(() => {
  return `${iconClass.value} display-1 text-muted mb-3`;
});
</script>

<style scoped>
/*
  THIS IS THE FIX: Lifts the card's content above the shimmering
  pseudo-element, making it fully visible.
*/
.card-body {
  position: relative;
  z-index: 1;
}

h2 {
  font-weight: 300;
}

/* THIS IS THE FIX: Adds the fade-in animation */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out forwards;
}
</style>
