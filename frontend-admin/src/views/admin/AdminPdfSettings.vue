<template>
  <!-- Use v-if on LoadingModal to fully remove from DOM when done -->
  <LoadingModal v-if="isLoading" :visible="isLoading"/>

  <div v-show="!isLoading" class="admin-pdf-settings-page py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">
          <h1 class="display-5 mb-4">System PDF Export Settings</h1>

          <!-- Error State -->
          <div v-if="error" class="alert alert-danger">
            <h4 class="alert-heading"><Ban class="me-1" aria-hidden="true"/> Error</h4>
            <p>Could not load PDF settings. Please try again later.</p>
            <pre class="small">{{ error.message }}</pre>
          </div>

          <!-- Settings Form -->
          <form v-else @submit.prevent="handleSaveSettings">
            <div class="card glass-card shadow-sm">
              <div class="card-header">
                <h5 class="mb-0">System Default PDF Template</h5>
              </div>
              <div class="card-body p-4">
                <div class="mb-3">
                  <label class="form-label" for="defaultPdfTemplate">
                    System default PDF template
                  </label>
                  <p class="form-text text-muted mt-0 mb-2">
                    This is the system-wide default used for all public portfolio PDF downloads when a user has not set their own template. It applies to the homepage download button and as a fallback for individual portfolios.
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
// Import from the correct API modules to complete the refactor.
import {getAdminSettings, updateAdminSettings} from '@/services/api/admin.api.js';
import {publicApi} from '@/services/api/public.api.js';
import {settingsService} from '@/services/settingsService.js';
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
const isDirty = ref(false);
const error = ref<Error | null>(null);

const showSuccessModal = ref(false);
const showErrorModal = ref(false);
const errorMessage = ref('');

// Computed with getter/setter for two-way binding + dirty tracking
const selectedTemplate = computed({
  get: () => pdfSetting.value?.value || '',
  set: (val: string) => {
    if (pdfSetting.value && pdfSetting.value.value !== val) {
      pdfSetting.value.value = val;
      isDirty.value = true;
    }
  },
});

// Sleep helper to guarantee minimum modal visible time
const sleep = (ms: number) => new Promise((res) => setTimeout(res, ms));

onMounted(async () => {
  isLoading.value = true;
  error.value = null;

  const minDelay = sleep(500);

  try {
    // Fetch admin settings and available templates in parallel.
    const [settings, templates] = await Promise.all([
      getAdminSettings(),
      publicApi.getAvailablePdfTemplates(), // Use the public API now
    ]);

    const defaultSetting = (settings as unknown as AdminSetting[]).find(
      (s) => s.name === 'DEFAULT_PDF_TEMPLATE'
    );

    if (!defaultSetting) {
      throw new Error(
        "The 'DEFAULT_PDF_TEMPLATE' setting was not found in backend."
      );
    }

    pdfSetting.value = defaultSetting;
    originalTemplateValue.value = defaultSetting.value;
    availableTemplates.value = templates as unknown as string[];
  } catch (err: unknown) {
    console.error('Error loading settings:', err);
    error.value = err instanceof Error ? err : new Error(String(err));
  } finally {
    await minDelay;
    isLoading.value = false;
    await nextTick(); // ensure DOM updates for immediate interactivity
  }
});

const handleSaveSettings = async () => {
  if (!isDirty.value || !pdfSetting.value) return;
  isSaving.value = true;

  try {
    const payload = [
      {
        uuid: pdfSetting.value.uuid,
        value: selectedTemplate.value,
      },
    ];

    const updatedSettings = await updateAdminSettings(payload);
    settingsService.updateSettings(updatedSettings);

    originalTemplateValue.value = selectedTemplate.value;
    isDirty.value = false;
    showSuccessModal.value = true;
  } catch (err: unknown) {
    console.error('Save failed:', err);
    errorMessage.value = (err as Error).message || 'Failed to save settings. Try again.';
    showErrorModal.value = true;
  } finally {
    isSaving.value = false;
  }
};

const resetChanges = () => {
  if (!pdfSetting.value) return;
  selectedTemplate.value = originalTemplateValue.value;
  isDirty.value = false;
};

// Helper to make human readable template names
const formatTemplateName = (key: string) => {
  if (!key) return '';
  return key
    .split('-')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
};
</script>

<style scoped>
.admin-pdf-settings-page h1 {
  font-weight: 300;
}

.form-text {
  font-size: 0.9rem;
}
</style>
