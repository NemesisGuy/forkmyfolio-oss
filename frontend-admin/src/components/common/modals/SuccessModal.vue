<template>
  <teleport to="body">
    <div ref="modalRef" class="modal fade" tabindex="-1" aria-hidden="true" :id="modalId">
      <div class="modal-dialog modal-dialog-centered">
        <!-- THEME FIX: Uses 'glass-modal' and custom border/text colors -->
        <div class="modal-content glass-modal border-success border-opacity-50">
          <div class="modal-header bg-success bg-opacity-10 border-bottom border-success border-opacity-10">
            <h5 class="modal-title glass-text text-success" :id="modalId + 'Label'">
              <CheckCircle class="me-2" /> {{ title }}
            </h5>
            <button type="button" class="btn-close" :class="{ 'btn-close-white': currentTheme === 'dark' }" data-bs-dismiss="modal" aria-label="Close" @click="handleClose"></button>
          </div>
          <div class="modal-body p-4 text-center">
            <p class="mb-0 glass-text fs-5">{{ message }}</p>
          </div>
          <div class="modal-footer border-0 justify-content-center pb-4">
            <button type="button" class="btn btn-success px-4 shadow-sm interactive-lift" data-bs-dismiss="modal" @click="handleClose">
              Great!
            </button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { Modal } from 'bootstrap';
import { useTheme } from '@/services/themeService.js';
import { CheckCircle } from 'lucide-vue-next';

const { currentTheme } = useTheme();

const props = withDefaults(defineProps<{
  title?: string;
  message: string;
  visible?: boolean;
  modalId?: string;
}>(), {
  title: 'Success',
  visible: false,
  modalId: () => `success-modal-${Math.random().toString(36).slice(2, 11)}`
});

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;

/**
 * Dismiss the modal even when Bootstrap refuses to.
 *
 * <p>Bootstrap's `Modal.hide()` early-returns while the SHOW transition is
 * running (`_isTransitioning`), so a Great!/Close click during the modal's
 * ~300ms fade-in used to leave it stuck open forever: the click already
 * emitted `close` (so `visible` went false and the watcher never re-fired),
 * the hide no-opped, and nothing was left to ever remove the `show` class
 * (e2e run G: success modal still on screen 5s after dismissal; the same
 * race class as GuestPassManagement Cancel, fixed the same way).
 */
const dismiss = () => {
  try {
    modalInstance?.hide();
  } catch {
    // Instance may already be tearing down — the forced cleanup below still runs.
  }

  const el = modalRef.value;
  // Only force when hide() actually no-opped (`show` still present). When the
  // normal hide started, Bootstrap already removed `show` and the fade-out
  // animation is left alone.
  if (el && el.classList.contains('show')) {
    el.classList.remove('show');
    el.setAttribute('aria-hidden', 'true');
    el.removeAttribute('aria-modal');
    el.style.display = 'none';
    document.querySelectorAll('.modal-backdrop').forEach(backdrop => backdrop.remove());
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
  }
};

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
    dismiss();
  }
});

const handleClose = () => {
  emit('close');
  dismiss();
};
</script>

<style scoped>
/* Scoped styles removed in favor of global .glass-modal and theme-adaptive variables */
.modal-title {
  font-weight: 600;
}
</style>
