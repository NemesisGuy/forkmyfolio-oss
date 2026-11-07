<script setup lang="ts">
/**
 * @file app/components/modals/LoadingModal.vue
 * @description A full-screen, glassmorphic loading modal component.
 * Ported from ForkMyFolio-frontend-vue/src/components/common/modals/LoadingModal.vue.
 */

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

<template>
  <ClientOnly>
    <teleport to="body">
      <div v-if="visible" class="loading-modal-overlay">
        <!-- Backdrop with blur effect -->
        <div class="loading-modal-backdrop"></div>
        <!-- Spinner Container -->
        <div class="loading-modal-content">
          <div class="glass-spinner"></div>
        </div>
      </div>
    </teleport>
  </ClientOnly>
</template>

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
  /* Frosted glass effect */
  background-color: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px); /* For Safari */
}

.loading-modal-content {
  position: relative;
  z-index: 1061;
}

/* Custom Glassmorphic Spinner */
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
