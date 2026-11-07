<template>
  <teleport to="body">
    <!--
      FIX: By adding `v-bind="$attrs"`, any non-prop attributes (like the `class`
      attribute passed from ProjectDetailsPage) are applied directly to this div.
      This, combined with `inheritAttrs: false` in the script, resolves the
      "extraneous non-props attributes" warning.
    -->
    <div v-if="visible" v-bind="$attrs" class="loading-modal-overlay">
      <!-- Backdrop with blur effect -->
      <div class="loading-modal-backdrop"></div>
      <!-- Spinner Container -->
      <div class="loading-modal-content">
        <!-- THIS IS THE FIX: A custom glassmorphic spinner -->
        <div class="glass-spinner"></div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
/**
 * @file src/components/common/LoadingModal.vue
 * @description A full-screen, glassmorphic loading modal component.
 * Displays a blurred, semi-transparent overlay and a centered, glowing spinner.
 */

// FIX: Disable automatic attribute inheritance. This allows us to manually
// apply attributes to the correct element in the template, which is necessary
// because the component's root is conditional (`v-if`).
defineOptions({
  inheritAttrs: false,
});

defineProps<{
  /**
   * Controls the visibility of the loading modal.
   */
  visible: boolean;
}>();
</script>

<style scoped>
.loading-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1060;
  display: flex;
  justify-content: center;
  align-items: center;
}

.loading-modal-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  /* THIS IS THE FIX: Frosted glass effect */
  background-color: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px); /* For Safari */
}

.loading-modal-content {
  position: relative;
  z-index: 1061;
}

/* THIS IS THE FIX: Custom Glassmorphic Spinner */
.glass-spinner {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 4px solid rgba(var(--bs-body-color-rgb), 0.2);
  border-top-color: #ffffff;
  animation: spin 1s linear infinite;
  box-shadow: 0 0 15px rgba(var(--bs-body-color-rgb), 0.3);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
