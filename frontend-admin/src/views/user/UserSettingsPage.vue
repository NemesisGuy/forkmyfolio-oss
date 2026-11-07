<template>
  <LoadingModal :visible="isLoading"/>

  <div v-if="!isLoading" class="user-settings-page py-5 flex-grow-1">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">
          <h1 class="display-5 mb-4 glass-text animate-fade-in-up">Display Settings</h1>
          <p class="lead glass-subtitle mb-5 animate-fade-in-up" style="animation-delay: 0.1s;">
            Use the master switch to make your entire portfolio public or private. Then, fine-tune
            which sections are visible.
          </p>

          <!-- Error State -->
          <div v-if="error" class="alert alert-danger glass-card-dark animate-fade-in-up">
            <h4 class="alert-heading"><Ban class="me-1" aria-hidden="true"/> Error</h4>
            <p>Could not load your display settings. Please try again later.</p>
            <pre class="small">{{ error.message }}</pre>
          </div>

          <!-- Settings Form -->
          <div v-else class="animate-fade-in-up" style="animation-delay: 0.2s;">
            <!-- Master Visibility Toggle -->
            <div class="card glass-card mb-4">
              <div class="card-header">
                <h5 class="mb-0">Master Portfolio Visibility</h5>
              </div>
              <div class="card-body p-4 d-flex justify-content-between align-items-center">
                <div>
                  <h6 class="mb-0">
                    <component :is="isPortfolioPublic ? Unlock : Lock" class="me-2" :class="isPortfolioPublic ? 'text-success' : 'text-danger'" />
                    Portfolio is {{ isPortfolioPublic ? 'Public' : 'Private' }}
                  </h6>
                  <small class="text-muted">This is the main on/off switch for your entire public
                    portfolio.</small>
                </div>
                <div class="form-check form-switch form-switch-lg">
                  <input
                    id="visibilityToggle"
                    v-model="isPortfolioPublic"
                    :disabled="isVisibilityLoading"
                    class="form-check-input"
                    role="switch"
                    type="checkbox"
                    @change="handleVisibilityChange"
                  >
                  <label class="form-check-label" for="visibilityToggle">
                    <span v-if="isVisibilityLoading"
                          class="spinner-border spinner-border-sm"></span>
                  </label>
                </div>
              </div>
            </div>

            <!-- Search Engine Indexing Preference (Phase 10, Workstream 2.5) -->
            <div class="card glass-card mb-4">
              <div class="card-header">
                <h5 class="mb-0">Search Engine Indexing</h5>
              </div>
              <div class="card-body p-4 d-flex justify-content-between align-items-center">
                <div>
                  <h6 class="mb-0">
                    <component :is="isSearchIndexable ? Search : SearchX" class="me-2"
                               :class="isSearchIndexable ? 'text-success' : 'text-danger'" />
                    Search engines can {{ isSearchIndexable ? 'index' : 'not index' }} your portfolio
                  </h6>
                  <small class="text-muted">
                    Allow search engines to index my portfolio. This affects <strong>search-engine
                    discoverability only</strong> — your portfolio remains publicly accessible through its
                    direct link either way. Your portfolio must also be public, and the platform must permit
                    indexing, for this to take effect.
                  </small>
                </div>
                <div class="form-check form-switch form-switch-lg">
                  <input
                    id="indexingToggle"
                    v-model="isSearchIndexable"
                    :disabled="isIndexingLoading"
                    class="form-check-input"
                    role="switch"
                    type="checkbox"
                    @change="handleIndexingChange"
                  >
                  <label class="form-check-label" for="indexingToggle">
                    <span v-if="isIndexingLoading"
                          class="spinner-border spinner-border-sm"></span>
                  </label>
                </div>
              </div>
            </div>

            <!-- Section Visibility Form -->
            <form @submit.prevent="handleSaveSettings">
              <div class="card glass-card">
                <div class="card-header">
                  <h5 class="mb-0">Section Visibility</h5>
                </div>
                <div class="card-body p-4">
                  <ul class="list-group list-group-flush">
                    <li v-for="setting in displaySettings" :key="setting.key"
                        class="list-group-item px-0 d-flex justify-content-between align-items-center">
                      <div>
                        <h6 class="mb-0">{{ setting.label }}</h6>
                        <small class="text-muted">{{ setting.description }}</small>
                      </div>
                      <div class="form-check form-switch">
                        <input
                          :id="`switch-${setting.key}`"
                          v-model="setting.value"
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
    </div>

    <SuccessModal
      :visible="showSuccessModal"
      message="Your display settings have been updated successfully."
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

<script lang="ts" setup>
import { nextTick, onMounted, ref } from 'vue';
import { Lock, Unlock, Ban, Search, SearchX } from 'lucide-vue-next';
import {
  getMyPublicProfile,
  settingsApi,
  updateMyProfileVisibility,
  updateMyProfileIndexing
} from '@/services/api/user.api.js';
import { settingsService } from '@/services/settingsService.js';
import { notificationService } from '@/services/notificationService.js';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore.js';
import { ApiError } from '@/services/api/index.js';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';

interface Setting {
  key: string;
  label: string;
  description: string;
  value: boolean;
  uuid?: string;
}

// --- State ---
const displaySettings = ref<Setting[]>([]);
const originalSettings = ref<Record<string, boolean>>({});
const isPortfolioPublic = ref(false);
// Phase 10 (Workstream 2.5): per-user search-engine indexing preference.
// Defaults to false (conservative) until the profile payload confirms otherwise.
const isSearchIndexable = ref(false);
const isIndexingLoading = ref(false);

const isLoading = ref(true);
const isSaving = ref(false);
const isVisibilityLoading = ref(false);
const isDirty = ref(false);
const error = ref<ApiError | { message: string } | null>(null);

const showSuccessModal = ref(false);
const showErrorModal = ref(false);
const errorMessage = ref('');

const settingDefinitions = [
  { key: 'portfolio.projects.show', label: 'Projects Section' },
  { key: 'portfolio.skills.show', label: 'Skills Section' },
  { key: 'portfolio.experience.show', label: 'Experience Section' },
  { key: 'portfolio.qualifications.show', label: 'Qualifications Section' },
  { key: 'portfolio.testimonials.show', label: 'Testimonials Section' },
  { key: 'portfolio.contact.show', label: 'Contact Form' },
];

const sleep = (ms: number) => new Promise((res) => setTimeout(res, ms));

onMounted(async () => {
  isLoading.value = true;
  error.value = null;
  const minDelay = sleep(500);

  try {
    // Fetch both sets of data in parallel for efficiency
    const [userEffectiveSettings, profileStatus] = await Promise.all([
      settingsApi.getAll(),
      getMyPublicProfile(),
    ]);

    // --- Process Profile Visibility ---
    // FIX: The backend DTO serializes the field as 'isPublic', not 'public'.
    // This ensures the toggle switch correctly reflects the state from the database.
    isPortfolioPublic.value = (profileStatus as { isPublic: boolean }).isPublic;
    // Phase 10: indexing preference (serialized as 'isSearchIndexable'). An
    // absent field (pre-V13 backend payload) conservatively maps to false.
    isSearchIndexable.value =
      (profileStatus as { isSearchIndexable?: boolean }).isSearchIndexable === true;

    // --- Process Section Settings ---
    const settingsMap = new Map((userEffectiveSettings as { name: string, description?: string, value: string, uuid: string }[]).map(s => [s.name, s]));
    displaySettings.value = settingDefinitions.map(def => {
      const apiSetting = settingsMap.get(def.key);
      return {
        key: def.key,
        label: def.label,
        description: apiSetting?.description || 'No description available.',
        value: apiSetting ? apiSetting.value === 'true' : true,
        uuid: apiSetting?.uuid,
      };
    });
    originalSettings.value = Object.fromEntries(
      displaySettings.value.map(s => [s.key, s.value])
    );

  } catch (err: unknown) {
    console.error('Error loading settings page data:', err);
    error.value = err instanceof ApiError ? err : { message: (err as Error).message || 'Unexpected error.' };
  } finally {
    await minDelay;
    isLoading.value = false;
    await nextTick();
  }
});

const handleVisibilityChange = async () => {
  isVisibilityLoading.value = true;
  const newValue = isPortfolioPublic.value;
  try {
    // FIX: The update payload MUST send 'isPublic' to match the backend's validation requirement.
    // The error message you provided is the source of truth.
    await updateMyProfileVisibility({ isPublic: newValue });
    // Phase 7.4 (§5) FIX: the master switch changed on the backend, so ANY cached
    // public-portfolio state for this user is now stale. Without this, the router
    // guard's dedupe (publicPortfolioStore.fetchPortfolio) would serve the cached
    // success from BEFORE the toggle and the global PrivateLockScreen (App.vue,
    // `isPrivate && !isOwner`) would never react until a hard reload. Clearing the
    // singleton store forces the next public navigation to re-resolve from the
    // authoritative API (a concealed 404 / 403 for non-owners re-arms the lock
    // screen correctly — §403/404 policy). Section settings are unaffected:
    // they live in settingsService, not here.
    usePublicPortfolioStore().clearPortfolio();
    notificationService.add({
      type: 'success',
      message: `Your portfolio is now ${newValue ? 'public' : 'private'}.`
    });
  } catch (e: unknown) {
    notificationService.add({
      type: 'error',
      message: 'Failed to update visibility. Please try again.'
    });
    console.error('Visibility update failed:', e);
    // Revert the toggle on failure
    isPortfolioPublic.value = !newValue;
  } finally {
    isVisibilityLoading.value = false;
  }
};

const handleIndexingChange = async () => {
  isIndexingLoading.value = true;
  const newValue = isSearchIndexable.value;
  try {
    await updateMyProfileIndexing({ isSearchIndexable: newValue });
    // The public payload carries this preference (SSR derives per-user
    // noindex from it) — clear the cached store so the next public
    // navigation re-resolves, same as the visibility toggle above.
    usePublicPortfolioStore().clearPortfolio();
    notificationService.add({
      type: 'success',
      message: newValue
        ? 'Search engines may now index your portfolio (when public and the platform permits indexing).'
        : 'Search engines will no longer be offered your portfolio for indexing.'
    });
  } catch (e: unknown) {
    notificationService.add({
      type: 'error',
      message: 'Failed to update the indexing preference. Please try again.'
    });
    console.error('Indexing preference update failed:', e);
    // Revert the toggle on failure
    isSearchIndexable.value = !newValue;
  } finally {
    isIndexingLoading.value = false;
  }
};

const handleSaveSettings = async () => {
  if (!isDirty.value) return;
  isSaving.value = true;

  const payload = displaySettings.value
    .filter(setting => setting.value !== originalSettings.value[setting.key])
    .map(setting => ({
      uuid: setting.uuid as string,
      value: String(setting.value),
    }));

  if (payload.length === 0 || payload.some(p => !p.uuid)) {
    errorMessage.value = 'Could not save settings due to a data mismatch. Please refresh and try again.';
    showErrorModal.value = true;
    isSaving.value = false;
    return;
  }

  try {
    const updatedSettings = await settingsApi.update(payload) as { name: string, value: string }[];
    settingsService.updateSettings(updatedSettings);

    updatedSettings.forEach((updated: { name: string }) => {
      const settingInUI = displaySettings.value.find(s => s.key === updated.name);
      if (settingInUI) {
        originalSettings.value[updated.name] = settingInUI.value;
      }
    });

    isDirty.value = false;
    showSuccessModal.value = true;
  } catch (err: unknown) {
    errorMessage.value = (err as Error).message || 'Failed to save your settings. Please try again.';
    showErrorModal.value = true;
  } finally {
    isSaving.value = false;
  }
};

const resetChanges = () => {
  displaySettings.value.forEach(setting => {
    setting.value = originalSettings.value[setting.key];
  });
  isDirty.value = false;
};
</script>

<style scoped>
.user-settings-page h1 {
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

/* Styles for the new toggle switch */
.form-switch.form-switch-lg {
  padding-left: 3.5rem;
}

.form-switch.form-switch-lg .form-check-input {
  width: 3rem;
  height: 1.5rem;
}
</style>
