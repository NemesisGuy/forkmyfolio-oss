import { ref, type Ref } from 'vue';

export interface Notification {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
}

interface AddNotificationParams {
  message: string;
  type?: 'success' | 'error' | 'info' | 'warning';
  duration?: number;
}

const notifications: Ref<Notification[]> = ref([]);

/**
 * Adds a new notification to the list.
 */
const add = ({ message, type = 'info', duration = 5000 }: AddNotificationParams): void => {
  const id = Date.now() + Math.random();
  notifications.value.push({ id, message, type });

  // Automatically remove the notification after the duration
  setTimeout(() => {
    remove(id);
  }, duration);
};

/**
 * Removes a notification from the list by its ID.
 */
const remove = (id: number): void => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index !== -1) {
    notifications.value.splice(index, 1);
  }
};

export const notificationService = {
  notifications,
  add,
  remove,
};
