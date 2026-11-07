/**
 * @file app/utils/dateUtils.ts
 * @description Shared utility functions for date formatting.
 * Ported from ForkMyFolio-frontend-vue/src/utils/dateUtils.js and utils/index.js.
 * Nuxt auto-imports functions from `app/utils`, no manual import needed.
 */

/**
 * Formats a date string into a "Month Year" format for display.
 * It includes a fix to prevent timezone issues from showing the previous day.
 * @param dateString - The ISO date string to format.
 * @returns The formatted date string (e.g., "January 2023") or an empty string if input is invalid.
 */
export const formatDisplayDate = (dateString?: string | Date): string => {
  if (!dateString) return '';
  const date = new Date(dateString);
  // Add a day to the date to avoid timezone issues where it might show the previous day.
  date.setDate(date.getDate() + 1);
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', timeZone: 'UTC' };
  return date.toLocaleDateString(undefined, options);
};

/**
 * Formats a date string into a more readable format.
 * @param dateInput - The date string (parsable by Date constructor) or Date object.
 * @param options - Optional formatting options for Intl.DateTimeFormat.
 * @returns The formatted date string, or an empty string if input is invalid.
 */
export function formatDate(dateInput: string | Date | null | undefined,
  options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' }): string {
  if (!dateInput) {
    return '';
  }
  try {
    const date = new Date(dateInput);
    // Check if the date is valid
    if (isNaN(date.getTime())) {
      return 'Invalid Date';
    }
    return new Intl.DateTimeFormat(undefined, options).format(date);
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Invalid Date';
  }
}
