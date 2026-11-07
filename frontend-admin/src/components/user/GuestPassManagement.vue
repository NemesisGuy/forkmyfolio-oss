<template>
  <div class="guest-pass-management card glass-card shadow-sm border-0 rounded-4 overflow-hidden">
    <div class="card-header bg-transparent border-bottom p-4">
      <div class="d-flex justify-content-between align-items-center">
        <h3 class="h5 mb-0 glass-text">Active Guest Passes</h3>
        <button class="btn btn-primary d-flex align-items-center gap-2 px-3" @click="showCreateModal">
          <Plus />
          <span>Create New Pass</span>
        </button>
      </div>
    </div>

    <div class="card-body p-0">
      <div v-if="loading" class="p-5 text-center">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>

      <div v-else-if="passes.length === 0" class="p-5 text-center text-muted">
        <Ticket class="display-4 mb-3 d-block opacity-25" />
        <p class="mb-0">You don't have any guest passes yet.</p>
        <p class="small">Generate one to share your portfolio with recruiters.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="bg-light-subtle">
            <tr>
              <th class="ps-4">Label</th>
              <th>Token</th>
              <th>Expires At</th>
              <th class="text-end pe-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="pass in passes" :key="pass.uuid" :class="{ 'opacity-50': pass.expired }">
              <td class="ps-4">
                <span class="fw-medium">{{ pass.label }}</span>
                <span v-if="pass.expired" class="badge bg-danger rounded-pill ms-2">Expired</span>
              </td>
              <td>
                <code class="text-primary small bg-light-subtle px-2 py-1 rounded border">{{ pass.token }}</code>
              </td>
              <td>
                <span :class="pass.expired ? 'text-danger' : 'text-muted'" class="small">
                  {{ formatDate(pass.expiresAt) }}
                </span>
              </td>
              <td class="text-end pe-4">
                <div class="btn-group">
                  <button class="btn btn-sm btn-outline-info" title="Copy Access Link"
                          :disabled="!pass.token"
                          @click="pass.token && copyLink(pass.token)">
                    <Clipboard class="me-1" /> Copy Link
                  </button>
                  <button class="btn btn-sm btn-outline-danger" title="Revoke Pass" @click="confirmRevoke(pass)">
                    <Trash2 />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create Pass Modal -->
    <Teleport to="body">
      <div class="modal fade" id="createPassModal" tabindex="-1" ref="createModalRef">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content glass-card shadow-lg">
            <div class="modal-header border-bottom">
              <h5 class="modal-title">Generate Guest Pass</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" @click="hideCreateModal"></button>
            </div>
            <div class="modal-body p-4">
              <div class="mb-3">
                <label class="form-label small fw-bold">Label</label>
                <input v-model="newPass.label" type="text" class="form-control" placeholder="e.g., Google Recruiter - Jan 2026">
                <div class="form-text">What is this pass for?</div>
              </div>
              <div class="mb-3">
                <label class="form-label small fw-bold">Duration</label>
                <select v-model="newPass.duration" class="form-select">
                  <option :value="24">24 Hours</option>
                  <option :value="72">3 Days</option>
                  <option :value="168">7 Days</option>
                  <option :value="720">30 Days</option>
                </select>
              </div>
            </div>
            <div class="modal-footer border-top">
              <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
              <button type="button" class="btn btn-primary px-4" @click="handleCreate" :disabled="!newPass.label">Create Pass</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Feedback Toasts / Notifications would go here -->
    <div v-if="notification.show"
         class="position-fixed bottom-0 end-0 p-3" style="z-index: 1060">
      <div class="toast show align-items-center text-white border-0" :class="notification.type === 'success' ? 'bg-success' : 'bg-danger'" role="alert">
        <div class="d-flex">
          <div class="toast-body">
            {{ notification.message }}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" @click="notification.show = false"></button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { guestPassService } from '@/services/guestPassService';
import type { GuestPass } from '@/types';
import { Modal } from 'bootstrap';
import { Plus, Ticket, Clipboard, Trash2 } from 'lucide-vue-next';

// props is defined but not used
defineProps<{
  slug: string;
}>();

const loading = ref(true);
const passes = ref<GuestPass[]>([]);
const createModalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;

const newPass = reactive({
  label: '',
  duration: 24
});

const notification = reactive({
  show: false,
  message: '',
  type: 'success'
});

const fetchPasses = async () => {
  loading.value = true;
  try {
    console.log('[GuestPassManagement] Fetching passes...');
    const data = await guestPassService.getMyPasses();
    passes.value = data || [];
    console.log(`[GuestPassManagement] Fetched ${passes.value.length} passes:`, JSON.stringify(passes.value.map((p: GuestPass) => p.label)));
  } catch (error) {
    console.error('[GuestPassManagement] Failed to fetch passes:', error);
    showNotification('Failed to load guest passes.', 'error');
  } finally {
    loading.value = false;
  }
};

const initModal = () => {
  if (createModalRef.value) {
    if (modalInstance) {
      modalInstance.dispose();
    }
    modalInstance = new Modal(createModalRef.value);
    console.log('[GuestPassManagement] Modal initialized');
  } else {
    console.warn('[GuestPassManagement] createModalRef is null');
  }
};

const showCreateModal = () => {
  if (!modalInstance) initModal();
  newPass.label = '';
  newPass.duration = 24;
  modalInstance?.show();
};

const hideCreateModal = () => {
  console.log('[GuestPassManagement] Hiding modal (manual cleanup)');
  if (modalInstance) {
    try {
      modalInstance.hide();
    } catch (e) {
      console.warn('[GuestPassManagement] Bootstrap hide() failed:', e);
    }
  }

  // Robust manual cleanup
  const modalEl = document.getElementById('createPassModal');
  if (modalEl) {
    modalEl.classList.remove('show');
    modalEl.setAttribute('aria-hidden', 'true');
    modalEl.setAttribute('style', 'display: none');
    // Force a re-render/reflow if needed
  }

  // Clean up backdrop and body
  document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
  document.body.classList.remove('modal-open');
  document.body.style.overflow = '';
  document.body.style.paddingRight = '';
};

const handleCreate = async () => {
  if (!newPass.label) return;
  console.log('[GuestPassManagement] Creating pass:', newPass.label);
  try {
    const created = await guestPassService.create(newPass.label, newPass.duration);
    console.log('[GuestPassManagement] Created pass successfully:', created.uuid);
    showNotification('Guest pass created successfully!', 'success');
    hideCreateModal();
    // Refresh the list immediately
    await fetchPasses();
  } catch (error) {
    console.error('[GuestPassManagement] Create failed:', error);
    showNotification('Failed to create guest pass.', 'error');
  }
};

const confirmRevoke = async (pass: GuestPass) => {
  console.log('[GuestPassManagement] Confirming revocation for:', pass.uuid);
  if (confirm(`Are you sure you want to revoke the pass "${pass.label}"? All active sessions using this link will be terminated immediately.`)) {
    try {
      console.log('[GuestPassManagement] Revoking pass:', pass.uuid);
      await guestPassService.revoke(pass.uuid);
      console.log('[GuestPassManagement] Revoked pass successfully');
      showNotification('Pass revoked.', 'success');
      await fetchPasses();
    } catch (error) {
      console.error('[GuestPassManagement] Revocation failed:', error);
      showNotification('Failed to revoke pass.', 'error');
    }
  }
};

const copyLink = (token: string) => {
  const baseUrl = window.location.origin;
  const link = `${baseUrl}/guest-access?token=${token}`;

  // Use the modern API if available
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(link).then(() => {
      showNotification('Access link copied to clipboard!', 'success');
    }).catch((err) => {
      console.error('Clipboard API failed:', err);
      fallbackCopyTextToClipboard(link);
    });
  } else {
    // Fallback for non-secure contexts (e.g. WSL IP access)
    fallbackCopyTextToClipboard(link);
  }
};

const fallbackCopyTextToClipboard = (text: string) => {
  const textArea = document.createElement("textarea");
  textArea.value = text;

  // Ensure it's not visible and doesn't scroll the page
  textArea.style.position = "fixed";
  textArea.style.left = "-9999px";
  textArea.style.top = "0";
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();

  try {
    const successful = document.execCommand('copy');
    if (successful) {
      showNotification('Access link copied to clipboard!', 'success');
    } else {
      showNotification('Failed to copy to clipboard.', 'error');
    }
  } catch (err) {
    console.error('Fallback: Oops, unable to copy', err);
    showNotification('Failed to copy. Please copy manually: ' + text, 'error');
  }

  document.body.removeChild(textArea);
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString();
};

const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  notification.message = message;
  notification.type = type;
  notification.show = true;
  setTimeout(() => {
    notification.show = false;
  }, 3000);
};

onMounted(() => {
  fetchPasses();
  initModal();
});
</script>

<style scoped>
.guest-pass-management {
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.table th {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6c757d;
  font-weight: 600;
}

.bg-light-subtle {
  background-color: rgba(0, 0, 0, 0.02) !important;
}

.btn-group .btn {
  padding: 0.25rem 0.75rem;
}
</style>
