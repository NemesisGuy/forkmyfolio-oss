<script setup lang="ts">
/**
 * @file app/components/modals/TestimonialDetailsModal.vue
 * @description Public "testimonial details" Bootstrap modal.
 * Ported from ForkMyFolio-frontend-vue/src/components/public/TestimonialDetailsModal.vue.
 *
 * The modal is shown when the `testimonial` prop is not null. For deep links
 * (`?testimonial=<uuid>`) the SSR render applies the Bootstrap "open" state
 * (`.show` + `display:block`) so the modal is present server-side; the client
 * Bootstrap Modal instance then takes over (it re-applies the same classes and
 * manages the backdrop, focus trap and dismiss events).
 */
import type { Modal } from 'bootstrap';
import { Quote } from 'lucide-vue-next';

interface Testimonial {
  id: number;
  uuid?: string;
  authorName: string;
  authorTitle?: string;
  quote: string;
  imageUrl?: string;
  visible?: boolean;
}

const props = defineProps<{
  /**
   * The testimonial object to display. The modal is shown when this prop is not null.
   */
  testimonial: Testimonial | null;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;
// A deep link resolved before the Bootstrap JS chunk was loaded on the client.
let pendingShow = false;

/**
 * SSR/hydration-rendered "open" state. Bootstrap's class events manage the real
 * lifecycle on the client; this only mirrors the initial deep-linked state so
 * the server renders the modal as open.
 */
const showOpenState = ref(false);

onMounted(async () => {
  const bootstrap = await import('bootstrap');
  if (modalRef.value) {
    modalInstance = new bootstrap.Modal(modalRef.value);
    // FIX: Use 'hide.bs.modal' to fire the close event earlier, preventing a focus race condition.
    modalRef.value.addEventListener('hide.bs.modal', () => emit('close'));
    // After the fade-out completes, hand the open-state classes back to Bootstrap.
    modalRef.value.addEventListener('hidden.bs.modal', () => {
      showOpenState.value = false;
    });
    if (pendingShow) {
      pendingShow = false;
      modalInstance.show();
    }
  }
});

onUnmounted(() => {
  modalInstance?.hide();
  modalInstance?.dispose?.();
  modalInstance = null;
});

watch(() => props.testimonial, (newTestimonial) => {
  if (newTestimonial) {
    showOpenState.value = true;
    if (modalInstance) {
      modalInstance.show();
    } else {
      // Modal instance not ready yet (deep link during hydration).
      pendingShow = true;
    }
  } else {
    if (modalInstance) {
      modalInstance.hide();
    } else {
      showOpenState.value = false;
    }
  }
}, { immediate: true });
</script>

<template>
  <div ref="modalRef" aria-hidden="true" class="modal fade" tabindex="-1"
       :class="{ show: showOpenState }"
       :style="showOpenState ? { display: 'block' } : undefined">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div v-if="testimonial" class="modal-content glass-modal">
        <div class="modal-header">
          <h5 class="modal-title">
            Testimonial from {{ testimonial.authorName }}
          </h5>
          <!-- FIX: Removed btn-close-white. The default btn-close is theme-aware and will be visible in both light and dark modes. -->
          <button aria-label="Close" class="btn-close" data-bs-dismiss="modal" type="button"></button>
        </div>
        <div class="modal-body text-center p-4 p-md-5">
          <Quote class="display-1 text-primary opacity-25" />
          <figure class="mt-3">
            <blockquote class="blockquote">
              <p class="fs-4 fst-italic">"{{ testimonial.quote }}"</p>
            </blockquote>
            <figcaption class="blockquote-footer mt-3">
              <strong class="d-block fs-5">{{ testimonial.authorName }}</strong>
              <cite v-if="testimonial.authorTitle" :title="testimonial.authorTitle">{{
                  testimonial.authorTitle
                }}</cite>
            </figcaption>
          </figure>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline-secondary" data-bs-dismiss="modal" type="button">Close
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* No specific styles needed here, uses global glass-modal and Bootstrap classes */
</style>
