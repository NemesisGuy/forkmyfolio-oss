<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue';
import { useTheme } from '~/composables/useTheme';

/**
 * @file app/components/modals/ConfirmModal.vue
 * @description A reusable, glassmorphic modal for asking users to confirm an action.
 * Ported from ForkMyFolio-frontend-vue/src/components/common/modals/ConfirmModal.vue.
 *
 * DEAD-MODAL FIX (UAT round 2, root of the perceived "dead logout"):
 * The previous implementation created a Bootstrap JS Modal instance inside
 * `onMounted` via `await import('bootstrap')` — but the element lives inside
 * <ClientOnly><teleport>, whose slot content is NOT yet attached when the
 * component's own onMounted hook runs. `modalRef.value` was therefore null,
 * no instance was ever created, `show()` silently no-oped, and the logout
 * confirmation modal never appeared (the session stayed live: dead logout).
 * Visibility is now pure reactive state — no runtime Bootstrap-JS dependency
 * and no attach race. Identical look (glass + Bootstrap classes) and behaviour
 * (backdrop click / Escape / close button), same exposed show()/hide() API.
 */

const { currentTheme } = useTheme();

const props = withDefaults(defineProps<{
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'primary' | 'success' | 'danger' | 'warning';
  modalId?: string;
}>(), {
  confirmText: 'Confirm',
  cancelText: 'Cancel',
  type: 'primary',
  modalId: () => `confirm-modal-${Math.random().toString(36).slice(2, 11)}`,
});

const confirmButtonClass = computed(() => {
  return `btn-${props.type}`;
});

const emit = defineEmits<{
  (e: 'confirm'): void;
  (e: 'close'): void;
}>();

const isOpen = ref(false);

const show = () => {
  isOpen.value = true;
};

const hide = () => {
  isOpen.value = false;
};

const handleConfirm = () => {
  isOpen.value = false;
  emit('confirm');
};

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
  show,
  hide,
});
</script>

<template>
  <ClientOnly>
    <teleport to="body">
      <template v-if="isOpen">
        <div class="modal-backdrop fade show" aria-hidden="true" @click="handleClose"></div>
        <div :id="modalId" :aria-labelledby="modalId + 'Label'" aria-modal="true"
             class="modal fade show" role="dialog" style="display: block;" tabindex="-1">
          <div class="modal-dialog modal-dialog-centered">
            <!-- Uses 'glass-modal' class for consistent theme-adaptive background and blur -->
            <div class="modal-content glass-modal">
              <div class="modal-header border-bottom border-light border-opacity-10">
                <h5 :id="modalId + 'Label'" class="modal-title glass-text font-weight-bold">{{ title }}</h5>
                <button
                  :class="['btn-close', { 'btn-close-white': currentTheme === 'dark' }]"
                  aria-label="Close"
                  type="button"
                  @click="handleClose"
                ></button>
              </div>
              <div class="modal-body p-4">
                <p class="glass-description fs-5 mb-0">{{ message }}</p>
              </div>
              <div class="modal-footer border-top-0 pb-4">
                <button class="btn btn-outline-secondary px-4 interactive-lift" type="button" @click="handleClose">
                  {{ cancelText }}
                </button>
                <button :class="['btn', confirmButtonClass, 'px-4 shadow-sm interactive-lift']" type="button"
                        @click="handleConfirm">
                  {{ confirmText }}
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
