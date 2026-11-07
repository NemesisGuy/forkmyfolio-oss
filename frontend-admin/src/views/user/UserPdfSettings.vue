<template>
  <!-- Use v-if on LoadingModal to fully remove from DOM when done -->
  <LoadingModal :visible="isLoading"/>

  <div v-show="!isLoading" class="user-pdf-settings-page py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">
          <h1 class="display-5 mb-4">My PDF Settings</h1>
          <p class="lead text-muted mb-5">
            Customize the default template used when your portfolio is downloaded as a PDF.
          </p>

          <!-- Error State -->
          <div v-if="error" class="alert alert-danger">
            <h4 class="alert-heading"><Ban class="me-1" aria-hidden="true"/> Error</h4>
            <p>Could not load your PDF settings. Please try again later.</p>
            <pre class="small">{{ error.message }}</pre>
          </div>

          <!-- Settings Form -->
          <form v-else @submit.prevent="handleSaveSettings">
            <div class="card glass-card shadow-sm">
              <div class="card-header">
                <h5 class="mb-0">My Default PDF Template</h5>
              </div>
              <div class="card-body p-4">
                <div class="mb-3">
                  <label class="form-label" for="defaultPdfTemplate">
                    Portfolio Download Template
                  </label>
                  <p class="form-text text-muted mt-0 mb-2">
                    Select the template that will be used when visitors click the download button on
                    your portfolio.
                  </p>
                  <select
                    id="defaultPdfTemplate"
                    v-model="selectedTemplate"
                    :disabled="isSaving"
                    class="form-select"
                  >
                    <option disabled value="">Please select a template</option>
                    <option
                      v-for="template in availableTemplates"
                      :key="template"
                      :value="template"
                    >
                      {{ formatTemplateName(template) }}
                    </option>
                  </select>
                </div>
              </div>
            </div>

            <div class="mt-4 text-end">
              <button
                :disabled="!isDirty || isSaving"
                class="btn btn-secondary me-2"
                type="button"
                @click="resetChanges"
              >
                Reset
              </button>
              <button
                :disabled="!isDirty || isSaving"
                class="btn btn-primary"
                type="submit"
              >
                <span
                  v-if="isSaving"
                  aria-hidden="true"
                  class="spinner-border spinner-border-sm me-2"
                  role="status"
                ></span>
                {{ isSaving ? 'Saving...' : 'Save Settings' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <SuccessModal
      :visible="showSuccessModal"
      message="Your PDF settings have been updated successfully."
      title="Settings Saved"
      @close="showSuccessModal = false"
    />
    <ErrorModal
      :message="errorMessage"
      :visible="showErrorModal"
      title="Save Failed"
      @close="showErrorModal = false"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, ref} from 'vue';
import { Ban } from 'lucide-vue-next';
// --- THIS IS THE FIX ---
// Import from the correct API modules for better separation of concerns.
import {settingsApi} from '@/services/api/user.api.js';
import {publicApi} from '@/services/api/public.api.js';
import {settingsService} from '@/services/settingsService.js';
import {ApiError} from '@/services/api/index.js';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import type { AdminSetting } from '@/types';

// --- State ---
const pdfSetting = ref<AdminSetting | null>(null);
const originalTemplateValue = ref('');
const availableTemplates = ref<string[]>([]);

const isLoading = ref(true);
const isSaving = ref(false);
const error = ref<{message: string} | null>(null);

const showSuccessModal = ref(false);
const showErrorModal = ref(false);
const errorMessage = ref('');

/**
 * The selected template value is now a simple ref, bound directly to the select input.
 * This simplifies the logic and decouples the UI state from the full setting object.
 */
const selectedTemplate = ref('');
const isDirty = computed(() => selectedTemplate.value !== originalTemplateValue.value);

// Sleep helper to guarantee minimum modal visible time, preventing UI flicker.
const sleep = (ms: number) => new Promise((res) => setTimeout(res, ms));

onMounted(async () => {
  error.value = null;

  const minDelay = sleep(500);

  try {
    // Fetch user settings and available templates in parallel for efficiency.
    const [userSettings, templates] = await Promise.all([
      settingsApi.getAll() as Promise<AdminSetting[]>,
      (publicApi.getAvailablePdfTemplates() as unknown) as Promise<string[]>, // Use the public API now
    ]);

    // Find the user's specific PDF template setting from the server.
    const defaultSettingFromServer = userSettings.find(
      (s) => s.name === 'portfolio.pdf.template'
    );

    if (defaultSettingFromServer) {
      // If the setting exists, store it and set the initial values for the form.
      pdfSetting.value = defaultSettingFromServer;
      selectedTemplate.value = defaultSettingFromServer.value;
      originalTemplateValue.value = defaultSettingFromServer.value;
    } else {
      // If the setting is missing, we log a warning and prepare the UI to be functional
      // but unsaveable. The save handler will catch the missing UUID.
      console.warn("The 'portfolio.pdf.template' setting was not found for this user.");
      selectedTemplate.value = '';
      originalTemplateValue.value = '';
    }

    availableTemplates.value = templates;
  } catch (err: unknown) {
    console.error('Error loading user settings:', err);
    error.value = err instanceof ApiError ? err : {message: (err as Error).message || 'Unexpected error.'};
  } finally {
    await minDelay;
    isLoading.value = false;
    await nextTick(); // ensure DOM updates for immediate interactivity
  }
});

/**
 * Saves the user's selected PDF template setting to the backend.
 */
const handleSaveSettings = async () => {
  if (!isDirty.value) return;
  isSaving.value = true;

  // If the setting object or its UUID is missing, it means the backend didn't initialize
  // it correctly. We must block the save and inform the user.
  if (!pdfSetting.value?.uuid) {
    errorMessage.value = 'Cannot save setting. The PDF template setting was not found for your account. Please contact support.';
    showErrorModal.value = true;
    isSaving.value = false;
    return;
  }

  try {
    // The payload only needs the UUID and the new value for an update.
    const payload = [{
      uuid: pdfSetting.value.uuid,
      value: selectedTemplate.value,
    }];

    const updatedSettings = await settingsApi.update(payload) as AdminSetting[];
    // Update the central settings service so the whole app is aware.
    settingsService.updateSettings(updatedSettings);

    // After a successful save, find the setting again from the fresh list to update local state.
    const newPdfSetting = updatedSettings.find(s => s.name === 'portfolio.pdf.template');
    if (newPdfSetting) {
      pdfSetting.value = newPdfSetting;
      selectedTemplate.value = newPdfSetting.value;
      originalTemplateValue.value = newPdfSetting.value;
    }

    showSuccessModal.value = true;
  } catch (err: unknown) {
    console.error('Save failed:', err);
    errorMessage.value = (err as Error).message || 'Failed to save your settings. Please try again.';
    showErrorModal.value = true;
  } finally {
    isSaving.value = false;
  }
};

/**
 * Resets the selection back to its original value.
 */
const resetChanges = () => {
  selectedTemplate.value = originalTemplateValue.value;
};

/**
 * Formats a template key like 'classic-condensed' into a more readable 'Classic Condensed'.
 * @param {string} key The raw template name.
 * @returns {string} The formatted name.
 */
const formatTemplateName = (key: string) => {
  if (!key) return '';
  return key
    .split('-')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
};
</script>

<style scoped>
.user-pdf-settings-page h1 {
  font-weight: 300;
}

.form-text {
  font-size: 0.9rem;
}
</style>
