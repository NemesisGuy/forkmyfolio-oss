<template>
  <teleport to="body">
    <div :id="modalId" ref="modalRef" aria-hidden="true" :aria-labelledby="modalId + 'Label'"
         class="modal fade" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <!-- THEME FIX: Uses 'glass-modal' class for consistent theme-adaptive background and blur -->
        <div class="modal-content glass-modal">
          <div class="modal-header border-bottom border-light border-opacity-10">
            <h5 :id="modalId + 'Label'" class="modal-title glass-text font-weight-bold">{{ title }}</h5>
            <button
              :class="['btn-close', { 'btn-close-white': currentTheme === 'dark' }]"
              aria-label="Close"
              data-bs-dismiss="modal"
              type="button"
              @click="handleClose"
            ></button>
          </div>
          <div class="modal-body p-4">
            <p class="glass-description fs-5 mb-0">{{ message }}</p>
          </div>
          <div class="modal-footer border-top-0 pb-4">
            <button class="btn btn-outline-secondary px-4 interactive-lift" data-bs-dismiss="modal" type="button" @click="handleClose">
              {{ cancelText }}
            </button>
            <button :class="['btn', confirmButtonClass, 'px-4 shadow-sm interactive-lift']" data-bs-dismiss="modal" type="button"
                    @click="handleConfirm">
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
/**
 * @file src/components/common/ConfirmModal.vue
 * @description A reusable, glassmorphic modal for asking users to confirm an action.
 */
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useTheme} from '@/services/themeService.js';
import {Modal} from 'bootstrap';

const {currentTheme} = useTheme();

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

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
    modalRef.value.addEventListener('hidden.bs.modal', () => {
      emit('close');
    });
  }
});

onUnmounted(() => {
  modalInstance?.hide();
  modalInstance = null;
});

const handleConfirm = () => {
  emit('confirm');
};

const handleClose = () => {
  emit('close');
};

defineExpose({
  show: () => modalInstance?.show(),
  hide: () => modalInstance?.hide(),
});
</script>

<style scoped>
.modal-title {
  font-weight: 600;
}
</style>
