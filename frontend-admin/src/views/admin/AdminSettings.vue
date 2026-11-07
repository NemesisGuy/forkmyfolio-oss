<template>
  <div class="admin-settings py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">

          <div class="text-center mb-5">
            <h1 class="display-5 fw-light glass-text animate-fade-in-up">Application Settings</h1>
            <p class="lead glass-subtitle animate-fade-in-up" style="animation-delay: 0.1s;">
              Control global settings that affect all users unless they have their own override.
            </p>
          </div>

          <LoadingModal :visible="isLoading || isSaving"/>
          <SuccessModal
            :visible="showSuccessModal"
            message="The application settings have been updated successfully."
            title="Settings Saved"
            @close="showSuccessModal = false"
          />
          <ErrorModal
            :message="errorMessage"
            :visible="showErrorModal"
            title="Save Failed"
            @close="showErrorModal = false"
          />

          <div v-if="error" class="alert alert-danger glass-card-dark animate-fade-in-up">
            <h4 class="alert-heading"><Ban class="me-1" aria-hidden="true"/> Error</h4>
            <p>Could not load application settings. Please try again later.</p>
            <pre class="small">{{ error.message }}</pre>
          </div>

          <form v-else-if="!isLoading" class="animate-fade-in-up" style="animation-delay: 0.2s;"
                @submit.prevent="handleSave">
            <div class="card glass-card">
              <div class="card-header">
                <h5 class="mb-0">Global Section Visibility</h5>
              </div>
              <div class="card-body p-4">
                <ul class="list-group list-group-flush">
                  <li v-for="setting in settings" :key="setting.uuid"
                      class="list-group-item px-0 d-flex justify-content-between align-items-center">
                    <div>
                      <h6 class="mb-0">{{ formatSettingName(setting.name) }}</h6>
                      <small class="text-muted">{{ setting.description }}</small>
                    </div>
                    <div class="form-check form-switch">
                      <input
                        :id="`switch-${setting.name}`"
                        v-model="setting.value"
                        :disabled="isSaving"
                        class="form-check-input"
                        false-value="false"
                        role="switch"
                        true-value="true"
                        type="checkbox"
                        @change="markAsDirty"
                      >
                    </div>
                  </li>
                </ul>
              </div>
            </div>

            <div class="d-flex justify-content-end mt-4">
              <button :disabled="!isDirty || isSaving" class="btn btn-secondary me-2" type="button"
                      @click="resetChanges">
                Reset
              </button>
              <button :disabled="!isDirty || isSaving" class="btn btn-primary" type="submit">
                <span v-if="isSaving" aria-hidden="true"
                      class="spinner-border spinner-border-sm me-2"
                      role="status"></span>
                {{ isSaving ? 'Saving...' : 'Save Changes' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import { Ban } from 'lucide-vue-next';
// THIS IS THE FIX: Import from the main 'api/index.js' barrel file
import {ApiError, getAdminSettings, updateAdminSettings} from '@/services/api/index.js';
import {settingsService} from '@/services/settingsService.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';

const settings = ref([]);
const originalSettings = ref([]);
const isLoading = ref(true);
const isSaving = ref(false);
const error = ref(null);
const isDirty = ref(false);

const showSuccessModal = ref(false);
const showErrorModal = ref(false);
const errorMessage = ref('');

const copySettings = (source) => {
  if (!Array.isArray(source)) return [];
  return source.map(setting => ({...setting}));
};

onMounted(async () => {
  try {
    const fetchedSettings = await getAdminSettings() || [];
    // Filter out the PDF template setting, as it's managed elsewhere
    const displaySettings = fetchedSettings.filter(s => s.name !== 'DEFAULT_PDF_TEMPLATE');
    settings.value = copySettings(displaySettings);
    originalSettings.value = copySettings(displaySettings);
  } catch (err) {
    console.error('Failed to fetch settings:', err);
    error.value = err instanceof ApiError ? err : {message: 'An unexpected error occurred.'};
  } finally {
    isLoading.value = false;
  }
});

const markAsDirty = () => {
  isDirty.value = true;
};

const handleSave = async () => {
  if (!isDirty.value) return;
  isSaving.value = true;

  try {
    const payload = settings.value.map((s) => ({uuid: s.uuid, value: s.value}));
    const updatedSettings = await updateAdminSettings(payload);

    settingsService.updateSettings(updatedSettings);

    const displaySettings = updatedSettings.filter(s => s.name !== 'DEFAULT_PDF_TEMPLATE');
    settings.value = copySettings(displaySettings);
    originalSettings.value = copySettings(displaySettings);
    isDirty.value = false;

    showSuccessModal.value = true;
  } catch (err) {
    console.error('Failed to save settings:', err);
    errorMessage.value = err.message || 'An unexpected error occurred. Please try again.';
    showErrorModal.value = true;
  } finally {
    isSaving.value = false;
  }
};

const resetChanges = () => {
  settings.value = copySettings(originalSettings.value);
  isDirty.value = false;
};

const formatSettingName = (name) => {
  if (!name) return '';
  return name
    .replace('SHOW_', '')
    .replace(/_/g, ' ')
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
};
</script>

<style scoped>
.admin-settings-page .display-5 {
  font-weight: 300;
}

.list-group-item {
  background-color: transparent;
  border: none;
  border-bottom: 1px solid var(--glass-border);
  padding-top: 1rem;
  padding-bottom: 1rem;
  color: var(--glass-text);
}

.list-group-item:last-child {
  border-bottom: none;
}

.list-group-item .text-muted {
  color: var(--glass-text-secondary) !important;
}

.form-check-input {
  width: 3em;
  height: 1.5em;
  cursor: pointer;
}

.card-header {
  background-color: rgba(var(--bs-body-color-rgb), 0.05);
  border-bottom: 1px solid var(--glass-border-hover);
}
</style>
