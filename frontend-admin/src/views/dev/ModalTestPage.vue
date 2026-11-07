<template>
  <!-- THIS IS THE FIX: Removed the py-5 class to prevent vertical overflow -->
  <div class="modal-test-page flex-grow-1 d-flex align-items-center">
    <div class="container">
      <!-- The content is now wrapped in a beautiful, animated glass card -->
      <div
        class="card glass-card shimmering animate-fade-in-up interactive-card-lift interactive-card-shadow-primary">
        <div class="card-body p-4 p-md-5">
          <h1 class="card-title display-5">Modal Test Page</h1>
          <p class="card-text text-muted">Use these buttons to test the application's modals.</p>
          <hr class="my-4">
          <div class="d-flex gap-2 flex-wrap">
            <!-- Buttons now have interactive classes -->
            <button class="btn btn-success interactive-lift" @click="triggerSuccessModal">
              Show Success Modal
            </button>
            <button class="btn btn-danger interactive-lift" @click="triggerErrorModalString">
              Show Error Modal (String)
            </button>
            <button class="btn btn-danger interactive-lift" @click="triggerErrorModalList">
              Show Error Modal (List)
            </button>
            <button class="btn btn-primary interactive-lift"
                    @click="triggerConfirmModal('primary')">
              Show Confirm Modal (Primary)
            </button>
            <!-- THIS IS THE FIX: Added buttons to test the new confirm modal types -->
            <button class="btn btn-danger interactive-lift" @click="triggerConfirmModal('danger')">
              Show Confirm Modal (Danger)
            </button>
            <button class="btn btn-success interactive-lift"
                    @click="triggerConfirmModal('success')">
              Show Confirm Modal (Success)
            </button>
          </div>
          <div v-if="confirmResult" class="mt-4 alert alert-info">
            <strong>Confirm Modal Result:</strong> {{ confirmResult }}
          </div>
        </div>
      </div>
    </div>

    <!-- Modals to be tested -->
    <SuccessModal
      :message="successMessage"
      :title="successTitle"
      :visible="showSuccess"
      @close="closeSuccessModal"
    />

    <ErrorModal
      :message="errorMessage"
      :title="errorTitle"
      :visible="showError"
      @close="closeErrorModal"
    />

    <ConfirmModal
      :cancelText="cancelButtonText"
      :confirmText="confirmButtonText"
      :message="confirmMessage"
      :title="confirmTitle"
      :type="confirmType"
      :visible="showConfirm"
      @cancel="handleCancel"
      @close="handleCancel"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';

/**
 * @file src/views/dev/ModalTestPage.vue
 * @description A development page for testing modal components.
 */

// --- Success Modal State & Logic ---
const showSuccess = ref(false);
const successTitle = ref('Operation Successful');
const successMessage = ref('Your action was completed without any issues.');

const triggerSuccessModal = () => {
  showSuccess.value = true;
};

const closeSuccessModal = () => {
  showSuccess.value = false;
};


// --- Error Modal State & Logic ---
const showError = ref(false);
const errorTitle = ref('An Error Occurred');
const errorMessage = ref<string | string[]>(''); // Can be a string or an array

const triggerErrorModalString = () => {
  errorTitle.value = 'Login Failed';
  errorMessage.value = 'The credentials you provided are incorrect. Please try again.';
  showError.value = true;
};

const triggerErrorModalList = () => {
  errorTitle.value = 'Validation Failed';
  errorMessage.value = [
    'Username must be at least 3 characters.',
    'Email address is invalid.',
    'Password does not meet complexity requirements.'
  ];
  showError.value = true;
};

const closeErrorModal = () => {
  showError.value = false;
};

// --- Confirm Modal State & Logic ---
const showConfirm = ref(false);
const confirmTitle = ref('Confirm Action');
const confirmMessage = ref('Are you sure you want to proceed with this action?');
const confirmButtonText = ref('Proceed');
const cancelButtonText = ref('Abort');
const confirmResult = ref<string | null>(null);
const confirmType = ref<'primary' | 'danger' | 'success'>('primary'); // To control the button color

const triggerConfirmModal = (type: 'primary' | 'danger' | 'success' = 'primary') => {
  confirmResult.value = null;
  confirmType.value = type;
  confirmTitle.value = `Confirm ${type.charAt(0).toUpperCase() + type.slice(1)} Action`;
  confirmMessage.value = `Are you sure you want to proceed with this ${type} action?`;
  showConfirm.value = true;
};

const handleConfirm = () => {
  confirmResult.value = `Confirmed (${confirmType.value})!`;
  showConfirm.value = false;
};

const handleCancel = () => {
  confirmResult.value = `Cancelled (${confirmType.value}).`;
  showConfirm.value = false;
};
</script>

<style scoped>
/* THIS IS THE FIX: The page now takes up the full viewport height for better vertical centering */
.modal-test-page {
  min-height: calc(100vh - 56px - 1px); /* Full height minus navbar and its border */
  overflow-x: hidden;
}

.card-body {
  position: relative;
  z-index: 1;
}

h1 {
  font-weight: 300;
}

/* THIS IS THE FIX: Adds the fade-in animation */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out forwards;
}
</style>
