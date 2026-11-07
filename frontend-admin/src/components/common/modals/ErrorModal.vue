<template>
  <teleport to="body">
    <div ref="modalRef" class="modal fade" tabindex="-1" aria-hidden="true" :id="modalId">
      <div class="modal-dialog modal-dialog-centered">
        <!-- THEME FIX: Uses 'glass-modal' and custom border/text colors -->
        <div class="modal-content glass-modal border-danger border-opacity-50" :style="modalStyle">
          <div class="modal-header bg-danger bg-opacity-10 border-bottom border-danger border-opacity-10">
            <h5 class="modal-title glass-text text-danger" :id="modalId + 'Label'">
              <AlertOctagon class="me-2" /> {{ title }}
            </h5>
            <button type="button" class="btn-close" :class="{ 'btn-close-white': currentTheme === 'dark' }" data-bs-dismiss="modal" aria-label="Close" @click="handleClose"></button>
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
            <button type="button" class="btn btn-danger px-4 shadow-sm interactive-lift" data-bs-dismiss="modal" @click="handleClose">
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { Modal } from 'bootstrap';
import { useTheme } from '@/services/themeService.js';
import { AlertOctagon } from 'lucide-vue-next';

const { currentTheme } = useTheme();

const modalStyle = computed(() => {
  return {
    backgroundColor: currentTheme.value === 'dark'
      ? 'rgba(16, 16, 16, 0.55)'
      : 'rgba(var(--bs-body-color-rgb), 0.6)'
  };
});

const props = withDefaults(defineProps<{
  title?: string;
  message: string | string[];
  visible?: boolean;
  modalId?: string;
}>(), {
  title: 'Error',
  visible: false,
  modalId: () => `error-modal-${Math.random().toString(36).slice(2, 11)}`
});

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
    modalRef.value.addEventListener('hide.bs.modal', () => {
      emit('close');
    });

    if (props.visible) {
      modalInstance.show();
    }
  }
});

onUnmounted(() => {
  modalInstance?.hide();
  modalInstance = null;
});

watch(() => props.visible, (newVal) => {
  if (newVal) {
    modalInstance?.show();
  } else {
    modalInstance?.hide();
  }
});

const handleClose = () => {
  emit('close');
};
</script>

<style scoped>
/* Scoped styles removed in favor of global .glass-modal and theme-adaptive variables */
.modal-title {
  font-weight: 600;
}
</style>
