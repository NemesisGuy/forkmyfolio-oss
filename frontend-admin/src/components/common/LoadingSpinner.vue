<template>
  <div class="spinner-border" role="status">
    <span class="visually-hidden">Loading...</span>
  </div>
</template>

<script setup lang="ts">
/**
 * @file src/components/common/LoadingSpinner.vue
 * @description A simple, reusable loading spinner component without any layout wrappers.
 * Displays a Bootstrap spinner, intended to be placed and styled by its parent component.
 */

// No script logic needed for this simple component.
</script>

<style scoped>
/* Default size for the spinner. Color is inherited or set via text-color utilities. */
.spinner-border {
  width: 3rem;
  height: 3rem;
}
</style>
