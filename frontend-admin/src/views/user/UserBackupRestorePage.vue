<template>
  <div class="backup-restore-page py-5 flex-grow-1">
    <div class="container">
      <!-- Header -->
      <div class="text-center mb-5 animate-fade-in-up">
        <h2 class="display-5 glass-text">Backup & Restore</h2>
        <p class="lead glass-subtitle" style="animation-delay: 0.1s;">
          Download a full backup of your portfolio or restore from a previously saved file.
        </p>
      </div>

      <!-- Loading and Error Modals -->
      <LoadingModal :visible="isLoading"/>
      <ErrorModal :message="error || ''" :visible="!!error" title="An Error Occurred"
                  @close="error = null"/>
      <SuccessModal :message="successMessage || ''" :visible="!!successMessage" title="Success"
                    @close="handleSuccessClose"/>
      <PasswordVerificationModal
        :visible="passwordModalVisible"
        @close="onPasswordModalClosed"
        @confirm="onPasswordVerified"
      />

      <div class="row g-4 justify-content-center">
        <!-- Backup Card -->
        <div class="col-md-6 col-lg-5 animate-fade-in-up" style="animation-delay: 0.2s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3">
                <Database class="display-4 text-primary" />
              </div>
              <h5 class="card-title glass-title">Download Backup</h5>
              <p class="card-text glass-subtitle small flex-grow-1">
                Generate and download a full backup of your projects, skills, experience, and other
                portfolio data as a single JSON file.
              </p>
              <button :disabled="isLoading" class="btn btn-primary mt-auto interactive-lift"
                      @click="handleDownloadBackup">
                Download My Data
              </button>
            </div>
          </div>
        </div>

        <!-- Restore Card -->
        <div class="col-md-6 col-lg-5 animate-fade-in-up" style="animation-delay: 0.3s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3">
                <Database class="display-4 text-warning" />
              </div>
              <h5 class="card-title glass-title">Restore from Backup</h5>
              <p class="card-text glass-subtitle small flex-grow-1">
                Select a valid portfolio backup JSON file to restore your data.
                <strong class="text-danger d-block mt-2">Warning: This will overwrite your current
                  portfolio data.</strong>
              </p>
              <div class="mt-auto">
                <input
                  ref="fileInput"
                  :disabled="isLoading"
                  accept="application/json"
                  class="form-control"
                  type="file"
                  @change="handleFileSelect"
                />
                <button :disabled="!selectedFile || isLoading"
                        class="btn btn-warning mt-3 w-100 interactive-lift"
                        @click="handleRestoreBackup">
                  Restore from File
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * @file src/views/user/UserBackupRestorePage.vue
 * @description A page that allows an authenticated user to download a full backup of their
 * portfolio data or restore their portfolio from a previously downloaded backup file.
 */
import {ref} from 'vue';
import { Database } from 'lucide-vue-next';
import {useRouter} from 'vue-router';
import {downloadMyBackup, restoreMyBackup} from '@/services/api/user.api';
import { verifyBackupPassword } from '@/services/api/auth.api';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import PasswordVerificationModal from '@/components/common/modals/PasswordVerificationModal.vue';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore';
import {notificationService} from '@/services/notificationService';

const props = defineProps<{
  slug: string;
}>();

const router = useRouter();
const isLoading = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);
const backupToken = ref<string | null>(localStorage.getItem('myBackupToken') || null);
const passwordModalVisible = ref(false);
const pendingTokenResolver = ref<null | { resolve: (t:string|null)=>void; reject: (e:any)=>void }>(null);

const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const portfolioStore = usePublicPortfolioStore();

const ensureBackupToken = async (): Promise<string|null> => {
  const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  if (isLocal) {
    try {
      const { valid, backupToken: token } = await verifyBackupPassword('');
      if (valid && token) {
        backupToken.value = token;
        localStorage.setItem('myBackupToken', token);
        return token;
      }
    } catch {}
  }
  if (backupToken.value) return backupToken.value;
  return new Promise((resolve, reject) => {
    pendingTokenResolver.value = { resolve, reject };
    passwordModalVisible.value = true;
  });
};

const onPasswordVerified = async (password: string) => {
  passwordModalVisible.value = false;
  try {
    const { valid, backupToken: token } = await verifyBackupPassword(password);
    if (!valid || !token) {
      throw new Error('Invalid password for backup verification');
    }
    backupToken.value = token;
    localStorage.setItem('myBackupToken', token);
    pendingTokenResolver.value?.resolve(token);
    pendingTokenResolver.value = null;
  } catch (err) {
    pendingTokenResolver.value?.reject(err);
    pendingTokenResolver.value = null;
    error.value = err.message || 'Password verification failed';
  }
};

/**
 * Cancel path for the password modal. The pending ensureBackupToken() promise
 * MUST be settled here — otherwise the awaiting operation never resumes and
 * the LoadingModal stays open forever (page bricked until reload).
 */
const onPasswordModalClosed = () => {
  passwordModalVisible.value = false;
  pendingTokenResolver.value?.reject(new Error('Password verification cancelled.'));
  pendingTokenResolver.value = null;
};

const handleDownloadBackup = async () => {
  error.value = null;
  // Interactive phase FIRST: the password dialog must never be shown while
  // the LoadingModal is open — the LoadingModal's overlay (z-index 1060)
  // stacks above the password dialog (z-index 1050) and blocks all input.
  let token: string | null;
  try {
    token = await ensureBackupToken();
  } catch (err: unknown) {
    console.error('Backup password verification failed:', err);
    error.value = (err as Error).message || 'Password verification failed.';
    return;
  }
  isLoading.value = true;
  try {
    await downloadMyBackup(token || undefined);
    // UX Improvement: Add a non-blocking toast notification to confirm the download has started.
    notificationService.add({
      message: 'Backup download has started.',
      type: 'success'
    });
  } catch (err: unknown) {
    console.error('Backup download failed:', err);
    error.value = (err as Error).message || 'An unexpected error occurred during the backup download.';
  } finally {
    isLoading.value = false;
  }
};

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  selectedFile.value = target.files?.[0] || null;
};

const handleRestoreBackup = async () => {
  if (!selectedFile.value) {
    error.value = 'Please select a backup file first.';
    return;
  }

  error.value = null;

  // Acquire the backup token BEFORE the LoadingModal opens (same overlay
  // stacking reason as the download handler above).
  let token: string | null;
  try {
    token = await ensureBackupToken();
  } catch (err: unknown) {
    console.error('Backup password verification failed:', err);
    error.value = (err as Error).message || 'Password verification failed.';
    return;
  }

  isLoading.value = true;
  try {
    await restoreMyBackup(selectedFile.value, token || undefined);
    // THIS IS THE FIX: After a successful restore, the local data in the store
    // is now stale. By clearing the portfolio store, we ensure that the next
    // navigation to a portfolio page (like the dashboard) will trigger a full
    // re-fetch of the new, restored data from the backend.
    portfolioStore.clearPortfolio();

    successMessage.value = 'Your portfolio has been successfully restored! You will now be redirected to the dashboard.';
  } catch (err: unknown) {
    console.error('Backup restore failed:', err);
    error.value = (err as Error).message || 'An unexpected error occurred during the restore process. Please ensure the file is a valid backup.';
  } finally {
    isLoading.value = false;
    // Clear the file input
    if (fileInput.value) {
      fileInput.value.value = '';
    }
    selectedFile.value = null;
  }
};

const handleSuccessClose = () => {
  successMessage.value = null;
  // Redirect to dashboard to see the restored data
  router.push({name: 'dashboard', params: {slug: props.slug}});
};
</script>

<style scoped>
.backup-restore-page .display-5 {
  font-weight: 300;
}

.card-title i {
  vertical-align: -0.125em;
}

.form-control {
  background-color: rgba(var(--bs-body-bg-rgb), 0.5);
  border: 1px solid rgba(var(--bs-body-color-rgb), 0.1);
  color: var(--bs-body-color);
}
</style>
