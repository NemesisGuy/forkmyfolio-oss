/**
 * @file src/utils/dateUtils.js
 * @description Shared utility functions for date formatting.
 */

/**
 * Formats a date string into a "Month Year" format for display.
 * It includes a fix to prevent timezone issues from showing the previous day.
 * @param {string} dateString - The ISO date string to format.
 * @returns {string} The formatted date string (e.g., "January 2023") or an empty string if input is invalid.
 */
export const formatDisplayDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  // Add a day to the date to avoid timezone issues where it might show the previous day.
  date.setDate(date.getDate() + 1);
  const options = {year: 'numeric', month: 'long', timeZone: 'UTC'};
  return date.toLocaleDateString(undefined, options);
};