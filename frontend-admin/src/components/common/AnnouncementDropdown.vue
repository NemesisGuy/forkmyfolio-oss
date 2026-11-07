<template>
  <li class="nav-item dropdown announcement-dropdown">
    <a id="announcementDropdown" aria-expanded="false" class="nav-link position-relative d-flex align-items-center"
       data-bs-auto-close="outside" data-bs-toggle="dropdown" href="#" role="button">
      <Megaphone class="fs-5" />
      <span v-if="unreadCount > 0" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger shadow-sm">
        {{ unreadCount }}
        <span class="visually-hidden">unread announcements</span>
      </span>
    </a>
    <ul aria-labelledby="announcementDropdown" class="dropdown-menu dropdown-menu-end glass-dropdown p-0 border-0 shadow-lg">
      <li class="dropdown-header glass-header d-flex justify-content-between align-items-center p-3">
        <span class="fw-bold">System Announcements</span>
        <span v-if="unreadCount > 0" class="badge bg-primary rounded-pill">{{ unreadCount }} New</span>
      </li>
      <div class="announcement-list thin-scrollbar">
        <div v-if="isLoading" class="p-4 text-center">
          <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
        </div>
        <div v-else-if="announcements.length === 0" class="p-4 text-center opacity-75">
          <MessageCircle class="mb-2 fs-3 d-block" />
          <p class="small mb-0">No announcements at this time.</p>
        </div>
        <div v-else v-for="item in announcements" :key="item.uuid"
             class="announcement-item p-3 border-bottom border-white border-opacity-10 pointer"
             @click="markRead(item)">
          <div class="d-flex justify-content-between align-items-start mb-1">
            <h6 class="mb-0 fs-6 glass-title d-flex align-items-center">
              <span v-if="!item.read" class="unread-dot me-2"></span>
              {{ item.title }}
            </h6>
            <small class="text-muted text-nowrap ms-2">{{ formatDate(item.createdAt) }}</small>
          </div>
          <p class="small mb-1 glass-description" :class="{'opacity-50': item.read}">{{ item.message }}</p>
          <div class="d-flex align-items-center">
            <span class="badge bg-info bg-opacity-25 text-info x-small me-2">{{ item.senderName }}</span>
            <span v-if="item.targetRole" class="badge bg-secondary bg-opacity-25 text-secondary x-small">{{ item.targetRole }}</span>
          </div>
        </div>
      </div>
      <li v-if="announcements.length > 0" class="p-2 text-center border-top border-white border-opacity-10">
        <a class="text-decoration-none small text-info hover-underline" href="#" @click.prevent="markAllRead">
          Mark all as read
        </a>
      </li>
    </ul>
  </li>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {Megaphone, MessageCircle} from 'lucide-vue-next';
import {getMyAnnouncements, markAllAnnouncementsAsRead, markAnnouncementAsRead} from '@/services/api/announcements.api.js';

interface Announcement {
  uuid: string;
  title: string;
  message: string;
  createdAt: string;
  senderName: string;
  targetRole?: string;
  read: boolean;
}

const announcements = ref<Announcement[]>([]);
const unreadCount = ref(0);
const isLoading = ref(true);

const fetchAnnouncements = async () => {
  try {
    isLoading.value = true;
    const data = await getMyAnnouncements() as unknown as Announcement[];
    if (Array.isArray(data)) {
      announcements.value = data;
      // Calculate unread count based on 'read' property from backend
      unreadCount.value = data.filter((item) => !item.read).length;
    } else {
      console.warn("Received unexpected data format for announcements:", data);
      announcements.value = [];
      unreadCount.value = 0;
    }
  } catch (err) {
    console.error("Failed to fetch announcements:", err);
  } finally {
    isLoading.value = false;
  }
};

const formatDate = (isoStr: string) => {
  const date = new Date(isoStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 3600000) return Math.floor(diff / 60000) + 'm ago';
  if (diff < 86400000) return Math.floor(diff / 3600000) + 'h ago';
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
};

const markRead = async (announcement: Announcement) => {
  if (announcement.read) return;

  try {
    await markAnnouncementAsRead(announcement.uuid);
    announcement.read = true;
    unreadCount.value = Math.max(0, unreadCount.value - 1);
  } catch (err) {
    console.error("Failed to mark as read:", err);
  }
};

const markAllRead = async () => {
  try {
    await markAllAnnouncementsAsRead();
    unreadCount.value = 0;
    // Update local state to show as read
    announcements.value = announcements.value.map(a => ({ ...a, read: true }));
  } catch (err) {
    console.error("Failed to mark all as read:", err);
  }
};

onMounted(fetchAnnouncements);
</script>

<style scoped>
/* No min-width on the container so it doesn't push items away in the navbar */
.announcement-dropdown {
  position: relative;
}

.dropdown-menu {
  width: 350px;
  max-width: 90vw;
  backdrop-filter: blur(20px);
  background: rgba(var(--bs-dark-rgb), 0.85);
}

.announcement-list {
  max-height: 400px;
  overflow-y: auto;
}

.announcement-item {
  transition: background-color 0.2s ease;
}

.announcement-item:hover {
  background: rgba(var(--bs-body-color-rgb), 0.05);
}

.x-small {
  font-size: 0.7rem;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: var(--bs-primary);
  border-radius: 50%;
  box-shadow: 0 0 5px var(--bs-primary);
}

.pointer {
  cursor: pointer;
}

.thin-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.thin-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(var(--bs-body-color-rgb), 0.2);
  border-radius: 4px;
}
</style>
