/**
 * @file src/stores/notificationStore.js
 * @description A simple, self-contained service for managing global UI notifications (toasts).
 * It provides a reactive list of notifications and methods to add or remove them.
 */
import {ref} from 'vue';

/**
 * The reactive array holding the current list of notification objects.
 * @type {import('vue').Ref<Array<object>>}
 */
const notifications = ref([]);

/**
 * Adds a new notification to the list.
 * @param {object} notification - The notification object.
 * @param {string} notification.message - The message to display.
 * @param {string} [notification.type='info'] - The type of notification ('success', 'error', 'info', 'warning').
 * @param {number} [notification.duration=5000] - Duration in ms before auto-closing.
 */
const add = ({message, type = 'info', duration = 5000}) => {
  const id = Date.now() + Math.random();
  notifications.value.push({id, message, type});

  // Automatically remove the notification after the duration
  setTimeout(() => {
    remove(id);
  }, duration);
};

/**
 * Removes a notification from the list by its ID.
 * @param {number} id - The ID of the notification to remove.
 * @returns {void}
 */
const remove = (id) => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index !== -1) {
    notifications.value.splice(index, 1);
  }
};

/**
 * The notification service, providing state and methods for managing notifications.
 * @property {import('vue').Ref<Array<object>>} notifications - The reactive list of notifications.
 * @property {function} add - Function to add a new notification.
 * @property {function} remove - Function to remove a notification by its ID.
 */
export const notificationService = {
  notifications,
  add,
  remove,
};
