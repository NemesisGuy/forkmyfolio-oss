/**
 * @file src/services/messageNotificationService.js
 * @description A reactive service for managing the unread message count for an authenticated user.
 * It automatically fetches the count on login and when the browser tab becomes visible.
 */
import {ref, watch} from 'vue';
import {getUnreadMessageCount} from '@/services/api/user.api.js';
import {authService} from '@/services/authService';

/**
 * A reactive number representing the count of unread messages.
 * @type {import('vue').Ref<number>}
 */
const unreadCount = ref(0);

/**
 * Fetches the latest unread message count from the API if the user is authenticated.
 * Resets the count to 0 on error or if the user is not authenticated.
 * @returns {Promise<void>}
 */
const fetchUnreadCount = async () => {
  if (!authService.isAuthenticated.value) {
    unreadCount.value = 0;
    return;
  }
  try {
    unreadCount.value = await getUnreadMessageCount();
    console.log(`[MessageService] Unread count updated: ${unreadCount.value}`);
  } catch (error) {
    console.error('Failed to fetch unread message count:', error);
    unreadCount.value = 0; // Reset on error to avoid a stale, incorrect count
  }
};

/**
 * Decrements the count locally when a message is read, providing instant UI feedback.

 */
const decrementUnreadCount = () => {
  if (unreadCount.value > 0) {
    unreadCount.value--;
  }
};

/**
 * Increments the count locally when a message is marked as unread.
 */
const incrementUnreadCount = () => {
  unreadCount.value++;
};


// --- Automatic Refresh Logic ---
const handleVisibilityChange = () => {
  if (!document.hidden) {
    console.log('[MessageService] Tab is visible, fetching latest unread count.');
    fetchUnreadCount();
  }
};
document.addEventListener('visibilitychange', handleVisibilityChange);


// Watch for login/logout to automatically fetch or reset the count.
watch(authService.isAuthenticated, (isAuth) => {
  if (isAuth) {
    fetchUnreadCount();
  } else {
    unreadCount.value = 0;
  }
}, {immediate: true});


export const messageNotificationService = {
  unreadCount,
  fetchUnreadCount,
  decrementUnreadCount,
  incrementUnreadCount, // Export the new function
};
