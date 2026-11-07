<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue';
import { AlertOctagon } from 'lucide-vue-next';
import { useTheme } from '~/composables/useTheme';

/**
 * @file app/components/modals/ErrorModal.vue
 * @description Glassmorphic error modal used for download/API failures.
 * Ported from ForkMyFolio-frontend-vue/src/components/common/modals/ErrorModal.vue.
 *
 * DEAD-MODAL FIX (UAT round 2): same root cause as ConfirmModal — the element
 * lives inside <ClientOnly><teleport>, so `modalRef.value` was null when the
 * component's onMounted hook ran (the ClientOnly slot renders later), the
 * Bootstrap Modal instance was never created, `show()` silently no-oped and
 * download/API errors never surfaced. Visibility is now pure reactive state
 * driven by the `visible` prop; the exposed behaviour and look are unchanged.
 */

const { currentTheme } = useTheme();

const props = withDefaults(defineProps<{
  title?: string;
  message: string | string[];
  visible?: boolean;
  modalId?: string;
}>(), {
  title: 'Error',
  visible: false,
  modalId: () => `error-modal-${Math.random().toString(36).slice(2, 11)}`,
});

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const modalStyle = computed(() => {
  return {
    backgroundColor: currentTheme.value === 'dark'
      ? 'rgba(16, 16, 16, 0.55)'
      : 'rgba(var(--bs-body-color-rgb), 0.6)',
  };
});

const isOpen = ref(false);

// Visibility follows the parent-controlled prop (the Vue-side contract).
watch(() => props.visible, (newVal) => {
  isOpen.value = !!newVal;
}, { immediate: true });

const handleClose = () => {
  if (!isOpen.value) return;
  isOpen.value = false;
  emit('close');
};

// Behaviour parity with the Bootstrap modal: Escape closes, body scroll locks
// while open, cleanup is guaranteed on unmount.
const onKeydown = (e: KeyboardEvent) => {
  if (!isOpen.value) return;
  if (e.key === 'Escape') handleClose();
};

watch(isOpen, (open) => {
  document.body.classList.toggle('modal-open', open);
  document.body.style.overflow = open ? 'hidden' : '';
  if (open) {
    document.addEventListener('keydown', onKeydown);
  } else {
    document.removeEventListener('keydown', onKeydown);
  }
});

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown);
  document.body.classList.remove('modal-open');
  document.body.style.overflow = '';
});

defineExpose({
  hide: () => {
    isOpen.value = false;
  },
});
</script>

<template>
  <ClientOnly>
    <teleport to="body">
      <template v-if="isOpen">
        <div class="modal-backdrop fade show" aria-hidden="true" @click="handleClose"></div>
        <div ref="modalRef" :id="modalId" :aria-labelledby="modalId + 'Label'" aria-modal="true"
             class="modal fade show" role="dialog" style="display: block;" tabindex="-1">
          <div class="modal-dialog modal-dialog-centered">
            <!-- Uses 'glass-modal' and custom border/text colors -->
            <div class="modal-content glass-modal border-danger border-opacity-50" :style="modalStyle">
              <div class="modal-header bg-danger bg-opacity-10 border-bottom border-danger border-opacity-10">
                <h5 :id="modalId + 'Label'" class="modal-title glass-text text-danger">
                  <AlertOctagon class="me-2" /> {{ title }}
                </h5>
                <button aria-label="Close" class="btn-close" :class="{ 'btn-close-white': currentTheme === 'dark' }"
                        type="button" @click="handleClose"></button>
              </div>
              <div class="modal-body p-4 text-center">
                <template v-if="Array.isArray(message)">
                  <ul class="mb-0 ps-3 text-start">
                    <li v-for="(item, index) in message" :key="index" class="mb-1 glass-description">{{ item }}</li>
                  </ul>
                </template>
                <template v-else>
                  <p class="mb-0 glass-text fs-5">{{ message }}</p>
                </template>
              </div>
              <div class="modal-footer border-0 justify-content-center pb-4">
                <button class="btn btn-danger px-4 shadow-sm interactive-lift" type="button"
                        @click="handleClose">
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </teleport>
  </ClientOnly>
</template>

<style scoped>
.modal-title {
  font-weight: 600;
}

/* Our own backdrop + dialog stack on the same z-index band Bootstrap uses. */
.modal {
  z-index: 1055;
}

.modal-backdrop {
  z-index: 1050;
}
</style>
