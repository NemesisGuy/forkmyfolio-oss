<template>
  <div class="admin-system-backup-page py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <!-- Header -->
      <div class="text-center mb-5 animate-fade-in-up">
        <h2 class="display-5 glass-text">System Backup & Restore</h2>
        <p class="lead glass-subtitle" style="animation-delay: 0.1s;">
          Perform system-wide data management operations. Use with extreme caution.
        </p>
      </div>

      <!-- Modals for feedback -->
      <LoadingModal :visible="isLoading"/>
      <SuccessModal :message="successMessage" :visible="!!successMessage" title="Operation Successful"
                    @close="successMessage = null"/>
      <ErrorModal :message="errorMessage" :visible="!!errorMessage" title="Operation Failed"
                  @close="errorMessage = null"/>
      <PasswordVerificationModal
        :visible="passwordModalVisible"
        @close="onPasswordModalClosed"
        @confirm="onPasswordVerified"
      />
      <ConfirmModal
        ref="confirmModalRef"
        :message="confirmMessage"
        title="Confirm Destructive Action"
        type="danger"
        @close="actionToConfirm = null"
        @confirm="executeConfirmedAction"
      />

      <div class="row g-4 justify-content-center">
        <!-- System Backup Card -->
        <div class="col-md-6 col-lg-5 animate-fade-in-up" style="animation-delay: 0.2s;">
          <div class="card h-100 glass-card interactive-card-lift">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3"><CloudDownload class="display-4 text-primary" />
              </div>
              <h5 class="card-title glass-title">System Backup</h5>
              <p class="card-text glass-subtitle small flex-grow-1">
                Download a single JSON file containing all data for all users.
              </p>
              <button :disabled="isLoading" class="btn btn-primary mt-auto"
                      @click="handleDownloadSystemBackup">
                Download System Backup
              </button>
            </div>
          </div>
        </div>

        <!-- System Restore Card -->
        <div class="col-md-6 col-lg-5 animate-fade-in-up" style="animation-delay: 0.3s;">
          <div class="card h-100 glass-card interactive-card-lift">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3"><CloudUpload class="display-4 text-warning" /></div>
              <h5 class="card-title glass-title">System Restore</h5>
              <p class="card-text glass-subtitle small flex-grow-1">
                <strong class="text-danger">Wipes all data</strong> and restores from a system
                backup file.
              </p>
              <div class="mt-auto">
                <input ref="systemRestoreInput" accept="application/json" class="form-control"
                       type="file" @change="e => systemRestoreFile = e.target.files[0]">
                <button :disabled="!systemRestoreFile || isLoading"
                        class="btn btn-warning mt-3 w-100" @click="confirmAction('restoreSystem')">
                  Restore Entire System
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Single User Restore Card -->
        <div class="col-md-6 col-lg-5 animate-fade-in-up" style="animation-delay: 0.4s;">
          <div class="card h-100 glass-card interactive-card-lift">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3"><UserPlus class="display-4 text-info" /></div>
              <h5 class="card-title glass-title">Restore Single User</h5>
              <p class="card-text glass-subtitle small flex-grow-1">
                <strong class="text-danger">Wipes one user's portfolio</strong> and restores it
                from a standard user backup file.
              </p>
              <div class="mt-auto">
                <select v-model="selectedUserUuid" class="form-select mb-2">
                  <option disabled value="">Select a user to restore...</option>
                  <option v-for="user in allUsers" :key="user.uuid" :value="user.uuid">
                    {{ user.firstName }} {{ user.lastName }} ({{ user.email }})
                  </option>
                </select>
                <input ref="userRestoreInput" accept="application/json" class="form-control"
                       type="file" @change="e => userRestoreFile = e.target.files[0]">
                <button :disabled="!userRestoreFile || !selectedUserUuid || isLoading"
                        class="btn btn-info mt-3 w-100" @click="confirmAction('restoreUser')">
                  Restore User
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Wipe Data Card -->
        <div class="col-md-6 col-lg-5 animate-fade-in-up" style="animation-delay: 0.5s;">
          <div class="card h-100 glass-card interactive-card-lift border-danger">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3"><AlertOctagon class="display-4 text-danger" />
              </div>
              <h5 class="card-title text-danger">Wipe All Data</h5>
              <p class="card-text glass-subtitle small flex-grow-1">
                <strong class="text-danger">Irreversibly deletes all users, portfolios, and data
                  from the system.</strong>
              </p>
              <button :disabled="isLoading" class="btn btn-danger mt-auto"
                      @click="confirmAction('wipeSystem')">
                Wipe System Data
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import { CloudDownload, CloudUpload, UserPlus, AlertOctagon } from 'lucide-vue-next';
import {
  downloadSystemBackup,
  getAdminUsers,
  restoreSingleUserBackup,
  restoreSystemBackup,
  wipeSystemData
} from '@/services/api/admin.api.js';
import { verifyBackupPassword } from '@/services/api/auth.api.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';
import PasswordVerificationModal from '@/components/common/modals/PasswordVerificationModal.vue';

// --- State ---
const isLoading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const backupToken = ref<string | null>(localStorage.getItem('backupToken') || null);
const passwordModalVisible = ref(false);
const pendingTokenResolver = ref<null | { resolve: (t:string|null)=>void; reject: (e:any)=>void }>(null);

// --- User Selection State ---
const allUsers = ref([]);
const selectedUserUuid = ref('');

// --- File Input State ---
const systemRestoreFile = ref(null);
const userRestoreFile = ref(null);
const systemRestoreInput = ref(null);
const userRestoreInput = ref(null);

// --- Confirmation Modal State ---
const confirmModalRef = ref(null);
const actionToConfirm = ref(null);
const confirmMessage = ref('');

// --- Lifecycle ---
onMounted(async () => {
  isLoading.value = true;
  try {
    allUsers.value = await getAdminUsers({size: 1000}); // Fetch a large number of users
  } catch (err) {
    errorMessage.value = err.message || 'Failed to load the list of users.';
  } finally {
    isLoading.value = false;
  }
});

const ensureBackupToken = async (): Promise<string|null> => {
  // Always refresh on localhost to avoid stale tokens
  const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  if (isLocal) {
    try {
      const { valid, backupToken: token } = await verifyBackupPassword('');
      if (valid && token) {
        backupToken.value = token;
        localStorage.setItem('backupToken', token);
        return token;
      }
    } catch {}
  }
  // Use cached token as fallback for non-local or if verify failed
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
    localStorage.setItem('backupToken', token);
    pendingTokenResolver.value?.resolve(token);
    pendingTokenResolver.value = null;
  } catch (err) {
    pendingTokenResolver.value?.reject(err);
    pendingTokenResolver.value = null;
    errorMessage.value = err.message || 'Password verification failed';
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

// --- Handlers ---
const handleDownloadSystemBackup = async () => {
  errorMessage.value = '';
  // Interactive phase FIRST: the password dialog must never be shown while
  // the LoadingModal is open — the LoadingModal's overlay (z-index 1060)
  // stacks above the password dialog (z-index 1050) and blocks all input.
  let token: string | null;
  try {
    token = await ensureBackupToken();
  } catch (err) {
    errorMessage.value = err.message || 'Password verification failed.';
    return;
  }
  isLoading.value = true;
  try {
    await downloadSystemBackup(token || undefined);
    // No success message needed as the browser handles the download prompt.
  } catch (err) {
    errorMessage.value = err.message || 'Failed to download system backup.';
  } finally {
    isLoading.value = false;
  }
};

const confirmAction = (action) => {
  actionToConfirm.value = action;
  switch (action) {
    case 'restoreSystem':
      confirmMessage.value = 'Are you sure you want to restore the entire system? This will WIPE ALL CURRENT DATA and replace it with the contents of the backup file.';
      break;
    case 'restoreUser':
      confirmMessage.value = `Are you sure you want to restore this user's portfolio? This will WIPE THEIR CURRENT PORTFOLIO DATA.`;
      break;
    case 'wipeSystem':
      confirmMessage.value = 'Are you absolutely sure you want to WIPE THE ENTIRE SYSTEM? This action is irreversible and will delete all users and data.';
      break;
  }
  confirmModalRef.value?.show();
};

const executeConfirmedAction = async () => {
  const action = actionToConfirm.value;
  if (!action) return;

  errorMessage.value = '';
  successMessage.value = '';

  // System restore needs a backup token. Acquire it BEFORE the LoadingModal
  // opens — password verification is interactive and must not be covered by
  // the loading overlay.
  let restoreToken: string | undefined = undefined;
  if (action === 'restoreSystem') {
    try {
      restoreToken = (await ensureBackupToken()) || undefined;
    } catch (err) {
      errorMessage.value = err.message || 'Password verification failed.';
      actionToConfirm.value = null;
      return;
    }
  }

  isLoading.value = true;
  try {
    switch (action) {
      case 'restoreSystem':
        await handleSystemRestore(restoreToken);
        break;
      case 'restoreUser':
        await handleUserRestore();
        break;
      case 'wipeSystem':
        await handleWipeSystem();
        break;
    }
  } catch (err) {
    errorMessage.value = err.message || 'An unexpected error occurred.';
  } finally {
    isLoading.value = false;
    actionToConfirm.value = null;
  }
};

const handleSystemRestore = async (token?: string) => {
  if (!systemRestoreFile.value) throw new Error('No system restore file selected.');
  const formData = new FormData();
  formData.append('file', systemRestoreFile.value);
  await restoreSystemBackup(formData, token);
  successMessage.value = 'System has been successfully restored.';
  systemRestoreFile.value = null;
  if (systemRestoreInput.value) systemRestoreInput.value.value = '';
};

const handleUserRestore = async () => {
  if (!userRestoreFile.value) throw new Error('No user restore file selected.');
  if (!selectedUserUuid.value) throw new Error('No user selected for restore.');
  const formData = new FormData();
  formData.append('file', userRestoreFile.value);
  await restoreSingleUserBackup(selectedUserUuid.value, formData);
  successMessage.value = 'User portfolio has been successfully restored.';
  userRestoreFile.value = null;
  if (userRestoreInput.value) userRestoreInput.value.value = '';
};

const handleWipeSystem = async () => {
  await wipeSystemData();
  successMessage.value = 'The system has been completely wiped.';
};

</script>

<style scoped>
.admin-system-backup-page .display-5 {
  font-weight: 300;
}

.card-body .display-4 {
  opacity: 0.8;
}

.border-danger {
  border-color: rgba(var(--bs-danger-rgb), 0.5) !important;
}

.border-danger:hover {
  border-color: rgba(var(--bs-danger-rgb), 0.8) !important;
  box-shadow: 0 8px 32px rgba(var(--bs-danger-rgb), 0.3) !important;
}
</style>
