<template>
  <div class="admin-backup-page p-4">
    <h1 class="display-6 fw-bold glass-text mb-4">System Backup & Restore</h1>

    <!-- System Backup Card -->
    <div class="card glass-card glass-card-floating mb-4 interactive-card-lift">
      <div class="card-body">
        <div class="d-flex align-items-center">
          <div class="flex-shrink-0">
            <Database class="text-primary" style="font-size: 2.5rem;" />
          </div>
          <div class="flex-grow-1 ms-4">
            <h5 class="card-title glass-title">System Backup</h5>
            <p class="card-text glass-subtitle">
              Download a complete backup of the entire system. This includes all users, portfolios,
              settings, and messages in a single JSON file.
            </p>
            <button :disabled="isBackingUp" class="btn glass-btn-primary interactive-lift"
                    @click="handleBackup">
              <span v-if="isBackingUp" aria-hidden="true"
                    class="spinner-border spinner-border-sm me-2"
                    role="status"></span>
              <Download v-else class="me-2" />
              {{ isBackingUp ? 'Generating...' : 'Download Backup' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- System Restore Card -->
    <div class="card glass-card glass-card-floating border-danger interactive-card-lift">
      <div class="card-header bg-danger bg-opacity-10 border-bottom-0">
        <h5 class="mb-0 text-danger fw-bold"><AlertTriangle class="me-2" />Destructive
          Operation</h5>
      </div>
      <div class="card-body">
        <div class="d-flex align-items-center">
          <div class="flex-shrink-0">
            <Database class="text-danger" style="font-size: 2.5rem;" />
          </div>
          <div class="flex-grow-1 ms-4">
            <h5 class="card-title glass-title">System Restore</h5>
            <p class="card-text glass-subtitle">
              Restore the system from a previously generated backup file.
              <strong class="text-danger">WARNING:</strong> This will wipe all existing data and
              replace it with the contents of the backup file. This action cannot be undone.
            </p>
            <div class="input-group">
              <input ref="fileInput" accept=".json" class="form-control glass-input"
                     type="file" @change="handleFileSelect">
              <button :disabled="!selectedFile || isRestoring" class="btn btn-danger"
                      @click="handleRestore">
                <span v-if="isRestoring" aria-hidden="true"
                      class="spinner-border spinner-border-sm me-2"
                      role="status"></span>
                <Upload v-else class="me-2" />
                {{ isRestoring ? 'Restoring...' : 'Restore from File' }}
              </button>
            </div>
            <div v-if="fileName" class="form-text glass-text-secondary mt-2">
              Selected file: {{ fileName }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <ErrorModal :message="error.message" :title="error.title" :visible="showErrorModal"
                @close="showErrorModal = false"/>
    <SuccessModal :message="successMessage" :visible="showSuccessModal" title="Operation Successful"
                  @close="showSuccessModal = false"/>
    <ConfirmModal
      :visible="showConfirmModal"
      message="Are you absolutely sure you want to restore the system from this backup? This will permanently delete all current data. This action cannot be undone."
      title="Confirm System Restore"
      @close="showConfirmModal = false"
      @confirm="proceedWithRestore"
    />
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue';
import { Database, Download, Upload, AlertTriangle } from 'lucide-vue-next';
import {downloadSystemBackup, restoreSystemBackup} from '@/services/api/admin.api.js';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';

const isBackingUp = ref(false);
const isRestoring = ref(false);
const selectedFile = ref<File | null>(null);
const fileName = ref('');
const fileInput = ref<HTMLInputElement | null>(null);

const showErrorModal = ref(false);
const showSuccessModal = ref(false);
const showConfirmModal = ref(false);
const error = ref({title: '', message: ''});
const successMessage = ref('');

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file && file.type === 'application/json') {
    selectedFile.value = file;
    fileName.value = file.name;
  } else {
    selectedFile.value = null;
    fileName.value = '';
    error.value = {title: 'Invalid File', message: 'Please select a valid .json backup file.'};
    showErrorModal.value = true;
  }
};

const handleBackup = async () => {
  isBackingUp.value = true;
  try {
    await downloadSystemBackup();
    // The browser handles the download, so no success modal is needed unless we want to confirm the start.
  } catch (err: unknown) {
    console.error("Backup failed:", err);
    error.value = {
      title: 'Backup Failed',
      message: (err as Error).message || 'Could not generate the system backup. Please check the server logs.'
    };
    showErrorModal.value = true;
  } finally {
    isBackingUp.value = false;
  }
};

const handleRestore = () => {
  if (!selectedFile.value) {
    error.value = {title: 'No File Selected', message: 'Please select a backup file to restore.'};
    showErrorModal.value = true;
    return;
  }
  showConfirmModal.value = true;
};

const proceedWithRestore = async () => {
  showConfirmModal.value = false;
  isRestoring.value = true;

  if (!selectedFile.value) return;

  const formData = new FormData();
  formData.append('file', selectedFile.value);

  try {
    await restoreSystemBackup(formData);
    successMessage.value = 'System has been successfully restored from the backup file. It is recommended to log out and log back in.';
    showSuccessModal.value = true;
    // Clear the file input
    if (fileInput.value) {
      fileInput.value.value = '';
    }
    selectedFile.value = null;
    fileName.value = '';
  } catch (err: unknown) {
    console.error("Restore failed:", err);
    error.value = {
      title: 'Restore Failed',
      message: (err as Error).message || 'Could not restore the system from the backup. Please ensure the file is valid and check server logs.'
    };
    showErrorModal.value = true;
  } finally {
    isRestoring.value = false;
  }
};
</script>

<style scoped>
.admin-backup-page {
  max-width: 900px;
  margin: auto;
}

.card.border-danger {
  border-width: 2px !important;
  box-shadow: 0 8px 32px rgba(var(--bs-danger-rgb), 0.2);
}
</style>
