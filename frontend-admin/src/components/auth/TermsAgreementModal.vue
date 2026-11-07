<template>
  <div ref="modalRef" aria-hidden="true" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content glass-modal">
        <div class="modal-header">
          <h5 class="modal-title">Terms of Service & Privacy Policy</h5>
          <!-- No close button in header to make it "blocking" -->
        </div>
        <div class="modal-body">
          <!-- Loading state -->
          <div v-if="isLoadingContent" class="text-center p-4">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2 small text-muted">Loading legal documents...</p>
          </div>

          <!-- Error state -->
          <div v-else-if="contentError" class="alert alert-danger">
            {{ contentError }}
          </div>

          <!-- Content -->
          <div v-else>
            <div class="policy-content" v-html="termsContent"></div>
            <div class="policy-content mt-3" v-html="privacyContent"></div>
            <p class="small mt-3">
              By checking the box below, you acknowledge that you have read, understood, and agree to be bound by these terms.
            </p>
          </div>
        </div>
        <div class="modal-footer d-flex justify-content-between align-items-center">
          <div class="form-check">
            <input id="modalTermsCheck" v-model="isAccepted" class="form-check-input" type="checkbox">
            <label class="form-check-label small" for="modalTermsCheck">
              I have read and agree to the terms.
            </label>
          </div>
          <div>
            <!-- The cancel button is only shown if the parent is listening for the 'cancel' event -->
            <button v-if="$attrs.onCancel" class="btn btn-outline-secondary me-2" type="button" @click="handleCancel">
              Cancel
            </button>
            <button :disabled="!isAccepted" class="btn btn-primary" type="button" @click="handleConfirm">
              {{ confirmButtonText }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { Modal } from 'bootstrap';
import { getTermsOfService, getPrivacyPolicy } from '@/services/api/policy.api.js';

defineProps(/** @props */ {
  /**
   * The text to display on the main confirmation button.
   */
  confirmButtonText: {
    type: String,
    default: 'Confirm & Create Account'
  }
});

const emit = defineEmits(/** @emits */ {
  /** Emitted when the user accepts the terms and clicks the confirm button. */
  'confirm': null,
  /** Emitted when the user clicks the cancel button. */
  'cancel': null
});

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;
const isAccepted = ref(false);

// --- New state for dynamic content ---
const termsContent = ref('');
const privacyContent = ref('');
const isLoadingContent = ref(true);
const contentError = ref<string | null>(null);

const fetchPolicies = async () => {
  isLoadingContent.value = true;
  contentError.value = null;
  try {
    // Fetch both policies concurrently for better performance
    const [terms, privacy] = await Promise.all([
      getTermsOfService() as Promise<{ content: string }>,
      getPrivacyPolicy() as Promise<{ content: string }>
    ]);
    termsContent.value = terms.content;
    privacyContent.value = privacy.content;
  } catch (error) {
    console.error("Failed to load policy documents:", error);
    contentError.value = "Could not load the terms and conditions. Please try again later.";
  } finally {
    isLoadingContent.value = false;
  }
};

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value, {
      backdrop: 'static', // Makes it non-skippable by clicking outside
      keyboard: false // Prevents closing with Esc key
    });
  }
});

const handleConfirm = () => {
  if (isAccepted.value) {
    emit('confirm');
    modalInstance?.hide();
  }
};

const handleCancel = () => {
  emit('cancel');
  modalInstance?.hide();
};

onUnmounted(() => {
  if (modalInstance) {
    modalInstance.dispose();
  }
});

const show = () => {
  fetchPolicies();
  modalInstance?.show();
};
const hide = () => modalInstance?.hide();

// Expose the show/hide methods to the parent component
defineExpose({ show, hide });
</script>

<style scoped>
.policy-content {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid var(--bs-border-color);
  padding: 1rem;
  border-radius: var(--bs-border-radius);
  background-color: var(--bs-tertiary-bg);
}
</style>
