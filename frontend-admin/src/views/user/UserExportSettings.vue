<template>
  <LoadingModal :visible="isLoading"/>

  <div v-show="!isLoading" class="user-export-settings-page py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">
          <h1 class="display-5 mb-4">Export Settings</h1>
          <p class="lead text-muted mb-2">
            Choose what goes into the PDF and Markdown files you download of your
            <strong>own</strong> portfolio.
          </p>
          <p class="text-muted small mb-5">
            <Info class="me-1" aria-hidden="true"/>
            Public visitors always receive the public version of your portfolio — these
            preferences only apply to your own downloads while you are logged in.
          </p>

          <!-- Error State -->
          <div v-if="error" class="alert alert-danger">
            <h4 class="alert-heading"><Ban class="me-1" aria-hidden="true"/> Error</h4>
            <p>Could not load your export settings. Please try again later.</p>
            <pre class="small">{{ error.message }}</pre>
          </div>

          <!-- Settings Form -->
          <form v-else @submit.prevent="handleSaveSettings">
            <!-- Export Mode -->
            <div class="card glass-card shadow-sm mb-4">
              <div class="card-header">
                <h5 class="mb-0">Export Mode</h5>
              </div>
              <div class="card-body p-4">
                <div class="form-check mb-3">
                  <input
                    id="mode-full"
                    v-model="selectedMode"
                    :disabled="isSaving"
                    class="form-check-input"
                    name="exportMode"
                    type="radio"
                    value="FULL"
                    @change="isDirty = true"
                  >
                  <label class="form-check-label" for="mode-full">
                    <strong>Full Portfolio</strong>
                    <p class="form-text text-muted mt-0">
                      Every enabled section in its full form — complete descriptions and all
                      bullet points. This is the default.
                    </p>
                  </label>
                </div>
                <div class="form-check">
                  <input
                    id="mode-brief"
                    v-model="selectedMode"
                    :disabled="isSaving"
                    class="form-check-input"
                    name="exportMode"
                    type="radio"
                    value="BRIEF"
                    @change="isDirty = true"
                  >
                  <label class="form-check-label" for="mode-brief">
                    <strong>Brief Resume</strong>
                    <p class="form-text text-muted mt-0">
                      A concise CV: long descriptions are left out and achievement/feature
                      lists are trimmed to the top three. Your text is never rewritten —
                      only shortened by omission.
                    </p>
                  </label>
                </div>
              </div>
            </div>

            <!-- Section Toggles -->
            <div class="card glass-card shadow-sm">
              <div class="card-header">
                <h5 class="mb-0">Sections to Include</h5>
              </div>
              <div class="card-body p-4">
                <ul class="list-group list-group-flush">
                  <li v-for="section in sections" :key="section.key"
                      class="list-group-item px-0 d-flex justify-content-between align-items-center">
                    <div>
                      <h6 class="mb-0">{{ section.label }}</h6>
                      <small class="text-muted">{{ section.description }}</small>
                    </div>
                    <div class="form-check form-switch">
                      <input
                        :id="`export-switch-${section.key}`"
                        v-model="section.enabled"
                        :disabled="isSaving"
                        class="form-check-input"
                        role="switch"
                        type="checkbox"
                        @change="isDirty = true"
                      >
                    </div>
                  </li>
                </ul>
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
      message="Your export settings have been updated successfully."
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
import {nextTick, onMounted, ref} from 'vue';
import { Ban, Info } from 'lucide-vue-next';
import {settingsApi} from '@/services/api/user.api.js';
import {settingsService} from '@/services/settingsService.js';
import {ApiError} from '@/services/api/index.js';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import type { AdminSetting } from '@/types';

/**
 * Phase 8.3 — Export Settings CMS.
 *
 * Persists the owner's personal export preferences (mode + section toggles)
 * through the existing GET/PUT /api/v1/me/settings mechanism. The backend
 * seeds the global `export.*` settings with safe defaults (FULL + all
 * enabled), so a user who has never configured anything sees the defaults
 * here and their exports behave exactly as before.
 *
 * PRIVACY NOTE: these preferences only ever affect OWNER exports. The
 * backend ignores them for PUBLIC downloads (PRV-1), which the page states
 * explicitly so the toggle semantics are never misunderstood.
 */

const MODE_SETTING = 'export.mode';
const SECTION_PREFIX = 'export.section.';

/** Section metadata: UserSetting name suffix, label and explanation. */
const sections = ref([
  {key: 'summary', label: 'Summary / About', description: 'Your profile summary paragraph.', enabled: true},
  {key: 'experience', label: 'Experience', description: 'Your work history entries.', enabled: true},
  {key: 'projects', label: 'Projects', description: 'Your project entries and key features.', enabled: true},
  {key: 'skills', label: 'Skills', description: 'Your skills grouped by category.', enabled: true},
  {key: 'qualifications', label: 'Qualifications / Education', description: 'Degrees, certificates and courses.', enabled: true},
  {key: 'testimonials', label: 'Testimonials', description: 'Quotes from colleagues and clients.', enabled: true},
  {key: 'contact', label: 'Contact details', description: 'The email / website / social block at the top.', enabled: true},
]);

const selectedMode = ref('FULL');
const isDirty = ref(false);

const isLoading = ref(true);
const isSaving = ref(false);
const error = ref<{message: string} | null>(null);

const showSuccessModal = ref(false);
const showErrorModal = ref(false);
const errorMessage = ref('');

// Server-side setting objects (uuid + server value) keyed by setting name —
// the save handler only sends settings whose value actually changed.
const serverSettings = ref<Map<string, AdminSetting>>(new Map());
const originalMode = ref('FULL');
const originalSectionValues = ref<Map<string, boolean>>(new Map());

// Sleep helper to guarantee minimum modal visible time, preventing UI flicker.
const sleep = (ms: number) => new Promise((res) => setTimeout(res, ms));

onMounted(async () => {
  error.value = null;
  const minDelay = sleep(500);

  try {
    const userSettings = await settingsApi.getAll() as AdminSetting[];

    // Index the server list by name so every export.* setting (with its
    // uuid) is available for saving.
    const byName = new Map<string, AdminSetting>();
    for (const setting of userSettings) {
      if (setting.name === MODE_SETTING || setting.name.startsWith(SECTION_PREFIX)) {
        byName.set(setting.name, setting);
      }
    }
    serverSettings.value = byName;

    // Mode: server value if present and valid, otherwise the safe default.
    const modeSetting = byName.get(MODE_SETTING);
    const serverMode = (modeSetting?.value ?? 'FULL').toUpperCase();
    selectedMode.value = serverMode === 'BRIEF' ? 'BRIEF' : 'FULL';
    originalMode.value = selectedMode.value;

    // Sections: server value ("false" disables; anything else enables —
    // mirroring the backend's forgiving parser), otherwise default enabled.
    for (const section of sections.value) {
      const setting = byName.get(SECTION_PREFIX + section.key);
      section.enabled = setting ? setting.value.trim().toLowerCase() !== 'false' : true;
      originalSectionValues.value.set(section.key, section.enabled);
    }

    if (!modeSetting) {
      console.warn("The 'export.mode' setting was not found for this user. Saving will be blocked.");
    }
  } catch (err: unknown) {
    console.error('Error loading export settings:', err);
    error.value = err instanceof ApiError ? err : {message: (err as Error).message || 'Unexpected error.'};
  } finally {
    await minDelay;
    isLoading.value = false;
    await nextTick(); // ensure DOM updates for immediate interactivity
  }
});

/**
 * Saves only the settings whose value changed since load. Sends uuid+value
 * pairs to the existing bulk update endpoint; unrelated UserSetting values
 * are never touched.
 */
const handleSaveSettings = async () => {
  if (!isDirty.value) return;
  isSaving.value = true;

  const modeSetting = serverSettings.value.get(MODE_SETTING);
  if (!modeSetting?.uuid) {
    errorMessage.value = 'Cannot save settings. The export settings were not found for your account. Please contact support.';
    showErrorModal.value = true;
    isSaving.value = false;
    return;
  }

  try {
    const payload: {uuid: string; value: string}[] = [];

    if (selectedMode.value !== originalMode.value) {
      payload.push({uuid: modeSetting.uuid, value: selectedMode.value});
    }
    for (const section of sections.value) {
      if (section.enabled !== originalSectionValues.value.get(section.key)) {
        const setting = serverSettings.value.get(SECTION_PREFIX + section.key);
        if (!setting?.uuid) {
          throw new Error(`The export section setting '${section.key}' was not found for your account.`);
        }
        payload.push({uuid: setting.uuid, value: String(section.enabled)});
      }
    }

    if (payload.length === 0) {
      // Nothing actually changed (e.g. toggled on then off again).
      isDirty.value = false;
      showSuccessModal.value = true;
      return;
    }

    // The backend returns the FULL refreshed effective list.
    const updatedSettings = await settingsApi.update(payload) as AdminSetting[];
    settingsService.updateSettings(updatedSettings);

    // Re-sync local originals from the authoritative response.
    const refreshedByName = new Map(updatedSettings.map((s) => [s.name, s]));
    const refreshedMode = refreshedByName.get(MODE_SETTING);
    if (refreshedMode) {
      serverSettings.value.set(MODE_SETTING, refreshedMode);
      const serverMode = (refreshedMode.value ?? 'FULL').toUpperCase();
      selectedMode.value = serverMode === 'BRIEF' ? 'BRIEF' : 'FULL';
      originalMode.value = selectedMode.value;
    }
    for (const section of sections.value) {
      const refreshed = refreshedByName.get(SECTION_PREFIX + section.key);
      if (refreshed) {
        serverSettings.value.set(SECTION_PREFIX + section.key, refreshed);
        section.enabled = refreshed.value.trim().toLowerCase() !== 'false';
        originalSectionValues.value.set(section.key, section.enabled);
      }
    }

    isDirty.value = false;
    showSuccessModal.value = true;
  } catch (err: unknown) {
    console.error('Save failed:', err);
    errorMessage.value = (err as Error).message || 'Failed to save your settings. Please try again.';
    showErrorModal.value = true;
  } finally {
    isSaving.value = false;
  }
};

/** Resets the form back to the values as loaded from the server. */
const resetChanges = () => {
  selectedMode.value = originalMode.value;
  for (const section of sections.value) {
    section.enabled = originalSectionValues.value.get(section.key) ?? true;
  }
  isDirty.value = false;
};
</script>

<style scoped>
.user-export-settings-page h1 {
  font-weight: 300;
}

.form-text {
  font-size: 0.9rem;
}
</style>
