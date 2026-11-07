<template>
  <div class="admin-messages-page py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="glass-text mb-0">All Contact Messages</h2>
        <button class="btn btn-primary interactive-lift" @click="showBroadcastModal = true">
          <Megaphone class="me-2" />Send Broadcast
        </button>
      </div>

      <!-- Broadcast Modal -->
      <div v-if="showBroadcastModal" class="modal-backdrop fade show"></div>
      <div v-if="showBroadcastModal" class="modal fade show d-block" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content glass-card border-0">
            <div class="modal-header border-white border-opacity-10">
              <h5 class="modal-title glass-text">System Announcement</h5>
              <button class="btn-close btn-close-white" type="button" @click="showBroadcastModal = false"></button>
            </div>
            <div class="modal-body p-4">
              <form @submit.prevent="handleSendBroadcast">
                <div class="mb-3">
                  <label class="form-label glass-text">Title</label>
                  <input v-model="broadcastData.title" class="form-control" placeholder="Maintenance, Updates, etc." required type="text">
                </div>
                <div class="mb-3">
                  <label class="form-label glass-text">Target Role</label>
                  <select v-model="broadcastData.targetRole" class="form-select">
                    <option :value="null">Everyone</option>
                    <option value="USER">Standard Users Only</option>
                    <option value="ADMIN">Admins Only</option>
                  </select>
                </div>
                <div class="mb-3">
                  <label class="form-label glass-text">Message</label>
                  <textarea v-model="broadcastData.message" class="form-control" placeholder="What do you want to tell them?" required rows="4"></textarea>
                </div>
                <div class="text-end">
                  <button class="btn btn-outline-secondary me-2" type="button" @click="showBroadcastModal = false">Cancel</button>
                  <button :disabled="isBroadcasting" class="btn btn-primary" type="submit">
                    <span v-if="isBroadcasting" class="spinner-border spinner-border-sm me-1"></span>
                    {{ isBroadcasting ? 'Sending...' : 'Send Now' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
      <LoadingModal :visible="isLoading"/>
      <ErrorModal v-if="error" :message="error" :visible="true" @close="error = null"/>
      <div v-if="!isLoading && messages.length > 0" class="card glass-card">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover glass-table mb-0">
              <thead>
                <tr>
                  <th>From</th>
                  <th>To User</th>
                  <th>Message</th>
                  <th>Received</th>
                  <th class="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="message in messages" :key="message.uuid">
                  <td>{{ message.name }} <br> <small class="text-muted">{{ message.email }}</small></td>
                  <td>{{ message.user.firstName }} {{ message.user.lastName }} <br> <small class="text-muted">{{ message.user.slug }}</small></td>
                  <td class="message-preview">{{ message.message }}</td>
                  <td>{{ formatDateTime(message.createdAt) }}</td>
                  <td class="text-end">
                    <button class="btn btn-sm btn-outline-danger" @click="handleDelete(message)">
                      <Trash2 />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div v-else-if="!isLoading" class="text-center p-5 glass-card">
        <h4 class="glass-title">No messages found.</h4>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Megaphone, Trash2 } from 'lucide-vue-next';
import { ref, onMounted, reactive } from 'vue';
// FIX: Import the correctly named functions from the admin API file.
import { getAdminContactMessages, deleteAdminContactMessage } from '@/services/api/admin.api.js';
import { createAnnouncement } from '@/services/api/announcements.api.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';

const messages = ref([]);
const isLoading = ref(true);
const error = ref(null);

// Broadcast State
const showBroadcastModal = ref(false);
const isBroadcasting = ref(false);
const broadcastData = reactive({
  title: '',
  message: '',
  targetRole: null
});

onMounted(async () => {
  try {
    // FIX: Call the correctly named function.
    messages.value = await getAdminContactMessages();
  } catch (err) {
    error.value = err.message || 'Failed to load messages.';
  } finally {
    isLoading.value = false;
  }
});

const handleSendBroadcast = async () => {
  isBroadcasting.value = true;
  error.value = null;
  try {
    await createAnnouncement(broadcastData);
    showBroadcastModal.value = false;
    // Reset form
    broadcastData.title = '';
    broadcastData.message = '';
    broadcastData.targetRole = null;
    alert('Announcement broadcasted successfully!');
  } catch (err) {
    console.error("Failed to send broadcast:", err);
    error.value = err.message || 'Failed to send broadcast announcement.';
  } finally {
    isBroadcasting.value = false;
  }
};

const handleDelete = async (message) => {
  if (!confirm(`Are you sure you want to delete the message from ${message.name}?`)) return;
  try {
    // FIX: Call the correctly named function.
    await deleteAdminContactMessage(message.uuid);
    messages.value = messages.value.filter(m => m.uuid !== message.uuid);
  } catch (err) {
    error.value = err.message || 'Failed to delete message.';
  }
};

const formatDateTime = (isoString) => {
  if (!isoString) return 'N/A';
  return new Date(isoString).toLocaleString();
};
</script>

<style scoped>
.message-preview {
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.glass-table {
  color: var(--glass-text);
  --bs-table-hover-color: var(--glass-text);
  --bs-table-hover-bg: var(--glass-bg-hover);
}
.glass-table thead th {
  background-color: rgba(var(--bs-body-color-rgb), 0.05);
  border-bottom: 2px solid var(--glass-border-hover);
  color: var(--glass-text);
}
.glass-table td, .glass-table th {
  border-color: var(--glass-border);
  vertical-align: middle;
}
</style>
