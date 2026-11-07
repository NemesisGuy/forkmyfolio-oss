<template>
  <div class="user-messages-page py-5 flex-grow-1">
    <div class="container">
      <div class="d-flex justify-content-between align-items-center mb-4 animate-fade-in-up">
        <h2 class="mb-0 glass-text">My Messages</h2>
        <div aria-label="Message view toggle" class="btn-group" role="group">
          <button :class="!showArchived ? 'btn-primary' : 'btn-outline-primary'" class="btn"
                  type="button"
                  @click="showArchived = false">Inbox
          </button>
          <button :class="showArchived ? 'btn-primary' : 'btn-outline-primary'" class="btn"
                  type="button"
                  @click="showArchived = true">Archived
          </button>
        </div>
      </div>

      <LoadingModal :visible="isLoading"/>

      <div v-if="error" class="alert alert-danger glass-card-dark animate-fade-in-up">
        <strong>Error:</strong> {{ error.message || 'Could not load your messages.' }}
      </div>

      <div v-else-if="!isLoading && messages.length > 0" class="card glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover glass-table mb-0">
              <thead>
              <tr>
                <th class="status-col"></th>
                <th>From</th>
                <th>Message Preview</th>
                <th>Priority</th>
                <th>Received</th>
                <th class="action-sub-col">Read</th>
                <th class="action-sub-col">Replied</th>
                <th class="action-sub-col">Priority</th>
                <th class="action-sub-col">Archive</th>
                <th class="action-sub-col">Delete</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="message in messages" :key="message.uuid"
                  :class="{ 'unread-message': !message.isRead, 'replied-message': message.isReplied }"
                  class="message-row"
                  @click="viewMessage(message)">
                <td class="status-col text-center">
                  <span :class="statusDotClass(message)" class="status-dot"
                        :title="statusDotTitle(message)"></span>
                </td>
                <td>
                  <div class="fw-bold">{{ message.name }}</div>
                  <small class="text-muted">{{ message.email }}</small>
                </td>
                <td class="message-preview">{{ message.message }}</td>
                <td>
                  <span :class="priorityClass(message.priority)" class="badge">
                    {{ message.priority }}
                  </span>
                </td>
                <td>{{ formatDateTime(message.createdAt) }}</td>
                <td class="action-sub-col text-center">
                  <button v-if="message.isRead" class="btn btn-sm btn-outline-warning"
                          title="Mark as Unread" @click.stop="toggleReadStatus(message)">
                    <MailX />
                  </button>
                  <button v-else class="btn btn-sm btn-outline-success"
                          title="Mark as Read" @click.stop="toggleReadStatus(message)">
                    <MailCheck />
                  </button>
                </td>
                <td class="action-sub-col text-center">
                  <button v-if="message.isReplied" class="btn btn-sm btn-outline-secondary"
                          title="Mark as Not Replied" @click.stop="toggleRepliedStatus(message)">
                    <RotateCcw />
                  </button>
                  <button v-else class="btn btn-sm btn-outline-info"
                          title="Mark as Replied" @click.stop="toggleRepliedStatus(message)">
                    <CheckCircle />
                  </button>
                </td>
                <td class="action-sub-col text-center">
                  <div class="btn-group" role="group">
                    <button aria-expanded="false"
                            class="btn btn-sm btn-outline-primary"
                            data-bs-toggle="dropdown" title="Set Priority" type="button"
                            @click.stop>
                      <Flag />
                    </button>
                    <ul class="dropdown-menu dropdown-menu-dark">
                      <li><a class="dropdown-item" href="#"
                             @click.prevent.stop="changePriority(message, 'HIGH')">High</a></li>
                      <li><a class="dropdown-item" href="#"
                             @click.prevent.stop="changePriority(message, 'MEDIUM')">Medium</a></li>
                      <li><a class="dropdown-item" href="#"
                             @click.prevent.stop="changePriority(message, 'LOW')">Low</a></li>
                    </ul>
                  </div>
                </td>
                <td class="action-sub-col text-center">
                  <button v-if="!message.isArchived" class="btn btn-sm btn-outline-warning"
                          title="Archive Message" @click.stop="archiveMessage(message, true)">
                    <Archive />
                  </button>
                  <button v-else class="btn btn-sm btn-outline-info"
                          title="Move to Inbox" @click.stop="archiveMessage(message, false)">
                    <Upload />
                  </button>
                </td>
                <td class="action-sub-col text-center">
                  <button class="btn btn-sm btn-outline-danger"
                          title="Delete Message" @click.stop="confirmDelete(message)">
                    <Trash2 />
                  </button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div v-else-if="!isLoading" class="text-center p-5 glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <div class="empty-state-icon mb-4">
          <Archive />
        </div>
        <h4 class="glass-title">{{
            showArchived ? 'No Archived Messages' : 'Your Inbox is Empty'
          }}</h4>
        <p class="glass-subtitle">{{
            showArchived ? 'Messages you archive will appear here.' : 'Messages from your public contact form will appear here.'
          }}</p>
      </div>

      <div id="viewMessageModal" ref="viewModalRef" aria-hidden="true"
           aria-labelledby="viewMessageModalLabel" class="modal fade" tabindex="-1">
        <div class="modal-dialog modal-lg modal-dialog-centered">
          <div class="modal-content glass-modal">
            <div class="modal-header">
              <h5 id="viewMessageModalLabel" class="modal-title">Message from:
                {{ selectedMessage?.name }}</h5>
              <button aria-label="Close" class="btn-close" data-bs-dismiss="modal"
                      type="button"></button>
            </div>
            <div v-if="selectedMessage" class="modal-body">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <strong>Email:</strong>
                  <a :href="`mailto:${selectedMessage.email}`"
                     class="ms-2 text-decoration-underline">{{ selectedMessage.email }}</a>
                </div>
                <a :href="`mailto:${selectedMessage.email}`" class="btn btn-sm btn-primary">
                  <Reply class="me-2"/>Reply
                </a>
              </div>
              <hr class="my-3">
              <p class="message-body-text">{{ selectedMessage.message }}</p>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" data-bs-dismiss="modal" type="button">Close</button>
            </div>
          </div>
        </div>
      </div>

      <ConfirmModal
        ref="confirmModalRef"
        :message="`Are you sure you want to delete the message from ${messageToDelete?.name}? This action cannot be undone.`"
        title="Confirm Deletion"
        type="danger"
        @close="messageToDelete = null"
        @confirm="handleDelete"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  deleteMyContactMessage,
  getMyContactMessages,
  updateMyContactMessage,
} from '@/services/api/user.api';
import { messageNotificationService } from '@/services/messageNotificationService';
import { notificationService } from '@/services/notificationService';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';
import { Modal } from 'bootstrap';
import { MailX, MailCheck, RotateCcw, CheckCircle, Flag, Archive, Upload, Trash2, Reply } from 'lucide-vue-next';
import type { ContactMessage } from '@/types';

interface ContactMessageInternal extends ContactMessage {
  isRead: boolean;
  isReplied: boolean;
  isArchived: boolean;
}

const allMessages = ref<ContactMessageInternal[]>([]);
const isLoading = ref(true);
const error = ref<Error | null>(null);
const selectedMessage = ref<ContactMessageInternal | null>(null);
const messageToDelete = ref<ContactMessageInternal | null>(null);
const showArchived = ref(false);

const messages = computed(() => allMessages.value.filter(m => m.isArchived === showArchived.value));

let viewModalInstance: Modal | null = null;
const viewModalRef = ref<HTMLElement | null>(null);
const confirmModalRef = ref<InstanceType<typeof ConfirmModal> | null>(null);

const mapMessage = (backendMessage: Record<string, unknown> | null): ContactMessageInternal | null => {
  if (!backendMessage) return null;
  return {
    ...backendMessage,
    isRead: !!backendMessage.read,
    isReplied: !!backendMessage.replied,
    isArchived: !!backendMessage.archived,
  } as unknown as ContactMessageInternal;
};

onMounted(async () => {
  try {
    const rawMessages = await getMyContactMessages() || [];
    allMessages.value = (rawMessages as Record<string, unknown>[]).map(mapMessage).filter((m): m is ContactMessageInternal => m !== null);
  } catch (err: unknown) {
    console.error("Failed to fetch contact messages:", err);
    error.value = err instanceof Error ? err : new Error('An unexpected error occurred.');
  } finally {
    isLoading.value = false;
  }

  if (viewModalRef.value) {
    viewModalInstance = new Modal(viewModalRef.value);
  }
});

const formatDateTime = (isoString: string | undefined) => {
  if (!isoString) return 'N/A';
  return new Date(isoString).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  });
};

const priorityClass = (priority: string | undefined) => {
  switch (priority) {
    case 'HIGH':
      return 'text-bg-danger';
    case 'MEDIUM':
      return 'text-bg-warning';
    case 'LOW':
      return 'text-bg-info';
    default:
      return 'text-bg-secondary';
  }
};

const statusDotClass = (message: ContactMessageInternal) => {
  if (!message.isRead) return 'status-dot-unread';
  if (message.isReplied) return 'status-dot-replied';
  return 'status-dot-read';
};

const statusDotTitle = (message: ContactMessageInternal) => {
  if (!message.isRead) return 'Unread';
  if (message.isReplied) return 'Read and Replied';
  return 'Read';
};

const viewMessage = async (message: ContactMessageInternal) => {
  selectedMessage.value = message;
  viewModalInstance?.show();

  if (!message.isRead) {
    message.isRead = true;
    messageNotificationService.decrementUnreadCount();
    try {
      const updatedMessageFromServer = await updateMyContactMessage(message.uuid as string, { read: true });
      const mapped = mapMessage(updatedMessageFromServer as unknown as Record<string, unknown>);
      if (mapped) Object.assign(message, mapped);
    } catch (err: unknown) {
      console.error("Failed to mark message as read:", err);
      message.isRead = false;
      messageNotificationService.incrementUnreadCount();
      notificationService.add({ type: 'error', message: 'Could not update message status.' });
    }
  }
};

const toggleReadStatus = async (message: ContactMessageInternal) => {
  const originalState = { ...message };
  const newStatus = !message.isRead;
  message.isRead = newStatus;

  try {
    const updatedMessageFromServer = await updateMyContactMessage(message.uuid as string, { read: newStatus });
    const mapped = mapMessage(updatedMessageFromServer as unknown as Record<string, unknown>);
    if (mapped) Object.assign(message, mapped);

    if (newStatus) {
      messageNotificationService.decrementUnreadCount();
      notificationService.add({ type: 'info', message: 'Message marked as read.' });
    } else {
      messageNotificationService.incrementUnreadCount();
      notificationService.add({ type: 'info', message: 'Message marked as unread.' });
    }
  } catch (err: unknown) {
    Object.assign(message, originalState);
    console.error("Failed to toggle read status:", err);
    notificationService.add({ type: 'error', message: 'Could not update message status.' });
  }
};

const toggleRepliedStatus = async (message: ContactMessageInternal) => {
  const originalState = { ...message };
  const newStatus = !message.isReplied;
  message.isReplied = newStatus;

  try {
    const updatedMessageFromServer = await updateMyContactMessage(message.uuid as string, { replied: newStatus });
    const mapped = mapMessage(updatedMessageFromServer as unknown as Record<string, unknown>);
    if (mapped) Object.assign(message, mapped);
    notificationService.add({
      type: 'info',
      message: `Message marked as ${newStatus ? 'replied' : 'not replied'}.`
    });
  } catch (err: unknown) {
    Object.assign(message, originalState);
    console.error("Failed to toggle replied status:", err);
    notificationService.add({ type: 'error', message: 'Could not update replied status.' });
  }
};

const changePriority = async (message: ContactMessageInternal, priority: 'LOW' | 'MEDIUM' | 'HIGH') => {
  const originalState = { ...message };
  message.priority = priority;

  try {
    const updatedMessageFromServer = await updateMyContactMessage(message.uuid as string, { priority });
    const mapped = mapMessage(updatedMessageFromServer as unknown as Record<string, unknown>);
    if (mapped) Object.assign(message, mapped);
    notificationService.add({ type: 'info', message: `Priority set to ${priority}.` });
  } catch (err: unknown) {
    Object.assign(message, originalState);
    console.error("Failed to change priority:", err);
    notificationService.add({ type: 'error', message: 'Could not update priority.' });
  }
};

const archiveMessage = async (message: ContactMessageInternal, archive = true) => {
  const originalState = { ...message };
  message.isArchived = archive;

  if (!originalState.isRead) {
    if (archive) {
      messageNotificationService.decrementUnreadCount();
    } else {
      messageNotificationService.incrementUnreadCount();
    }
  }

  try {
    await updateMyContactMessage(message.uuid as string, { archived: archive });
    notificationService.add({
      type: 'success',
      message: `Message ${archive ? 'archived' : 'moved to inbox'}.`
    });
  } catch (err: unknown) {
    const messageInList = allMessages.value.find(m => m.uuid === message.uuid);
    if (messageInList) {
      Object.assign(messageInList, originalState);
    }
    if (!originalState.isRead) {
      if (archive) {
        messageNotificationService.incrementUnreadCount();
      } else {
        messageNotificationService.decrementUnreadCount();
      }
    }
    console.error("Failed to archive message:", err);
    notificationService.add({
      type: 'error',
      message: `Could not ${archive ? 'archive' : 'unarchive'} message.`
    });
  }
};

const confirmDelete = (message: ContactMessageInternal) => {
  messageToDelete.value = message;
  confirmModalRef.value?.show();
};

const handleDelete = async () => {
  if (!messageToDelete.value) return;
  const messageToDeleteRef = messageToDelete.value;

  try {
    await deleteMyContactMessage(messageToDeleteRef.uuid as string);
    allMessages.value = allMessages.value.filter(m => m.uuid !== messageToDeleteRef.uuid);

    if (!messageToDeleteRef.isRead) {
      await messageNotificationService.fetchUnreadCount();
    }

    notificationService.add({
      message: `Message from '${messageToDeleteRef.name}' was deleted.`,
      type: 'success'
    });
  } catch (err: unknown) {
    console.error("Failed to delete message:", err);
    notificationService.add({
      message: err instanceof Error ? err.message : 'Could not delete the message.',
      type: 'error'
    });
  } finally {
    messageToDelete.value = null;
  }
};
</script>

<style scoped>
.status-col {
  width: 30px;
}

.action-sub-col {
  width: 60px;
  padding: 0.5rem;
}

.action-sub-col button {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-sub-col .btn-group {
  display: flex;
  justify-content: center;
}

.action-sub-col .btn-group > button {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot-unread {
  background-color: var(--bs-primary);
}

.status-dot-read {
  background-color: var(--bs-secondary);
}

.status-dot-replied {
  background-color: var(--bs-success);
}

.unread-message {
  font-weight: bold;
  --bs-table-hover-bg: rgba(var(--bs-primary-rgb), 0.15);
}

.replied-message td:not(.action-sub-col) {
  color: var(--glass-text-secondary) !important;
  opacity: 0.7;
}

.message-row {
  cursor: pointer;
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
  font-weight: 500;
  padding: 0.5rem;
  text-align: center;
}

.glass-table td {
  border-color: var(--glass-border);
  vertical-align: middle;
  padding: 0.5rem;
}

.message-row {
  transition: all 0.2s ease-in-out;
}

.message-row:hover {
  transform: scale(1.01);
  box-shadow: 0 8px 25px var(--glass-shadow);
  z-index: 2;
  position: relative;
}

.message-preview {
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-body-text {
  white-space: pre-wrap;
  font-size: 1.1rem;
  line-height: 1.6;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--glass-text);
}

.glass-modal {
  background-color: var(--glass-bg-modal);
  color: var(--glass-text);
  border: 1px solid var(--glass-border);
}

.glass-modal .modal-header,
.glass-modal .modal-footer {
  border-color: var(--glass-border-hover);
}

.modal-body a:not(.btn) {
  color: var(--bs-primary);
  text-decoration: none;
}

.modal-body a:not(.btn):hover {
  text-decoration: underline;
}
</style>
