<template>
  <div class="toast-container position-fixed top-0 end-0 p-3">
    <TransitionGroup name="toast-fade">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        :class="toastClass(notification.type)"
        aria-atomic="true"
        aria-live="assertive"
        class="toast show"
        role="alert"
      >
        <div class="toast-header">
          <component :is="iconComponent(notification.type)" class="me-2" />
          <strong class="me-auto">{{ title(notification.type) }}</strong>
          <button
            aria-label="Close"
            class="btn-close"
            type="button"
            @click="notificationService.remove(notification.id)"
          ></button>
        </div>
        <div class="toast-body">
          {{ notification.message }}
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { notificationService } from '@/services/notificationService.js';
import { CheckCircle, AlertTriangle, Info } from 'lucide-vue-next';

interface Notification {
  id: number;
  message: string;
  type?: 'success' | 'error' | 'warning' | 'info';
}

const notifications = computed(() => (notificationService.notifications.value as unknown) as Notification[]);

const toastClass = (type: string | undefined) => {
  switch (type) {
    case 'success':
      return 'text-bg-success';
    case 'error':
      return 'text-bg-danger';
    case 'warning':
      return 'text-bg-warning';
    default:
      return 'text-bg-info';
  }
};

const iconComponent = (type) => {
  switch (type) {
    case 'success':
      return CheckCircle;
    case 'error':
    case 'warning':
      return AlertTriangle;
    default:
      return Info;
  }
};

const title = (type) => {
  switch (type) {
    case 'success':
      return 'Success';
    case 'error':
      return 'Error';
    case 'warning':
      return 'Warning';
    default:
      return 'Information';
  }
};
</script>

<style scoped>
.toast-container {
  z-index: 2000; /* Ensure it's above modals */
  pointer-events: none; /* THIS IS THE FIX: The container is now click-through. */
}

.toast {
  pointer-events: auto; /* THIS IS THE FIX: The toasts inside are clickable again. */
  transition: all 0.5s ease;
  border: none;
}

.toast-header {
  border-bottom: 1px solid rgba(var(--bs-body-color-rgb), 0.2);
}

/* Vue Transition styles */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.4s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(100px);
}
</style>
