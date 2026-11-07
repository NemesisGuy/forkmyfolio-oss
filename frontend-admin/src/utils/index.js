/**
 * @file src/utils/index.js
 * @description Utility functions for the application.
 */

/**
 * Formats a date string into a more readable format.
 *
 * @param {string | Date} dateInput - The date string (parsable by Date constructor) or Date object.
 * @param {Intl.DateTimeFormatOptions} [options] - Optional formatting options for Intl.DateTimeFormat.
 * @returns {string} The formatted date string, or an empty string if input is invalid.
 *
 * @example
 * formatDate('2024-07-04T10:30:00Z'); // "July 4, 2024" (depending on locale and default options)
 * formatDate(new Date(), { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
 */
export function formatDate(dateInput, options = {year: 'numeric', month: 'long', day: 'numeric'}) {
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
    return 'Invalid Date'; // Or return the original input, or an empty string
  }
}

/**
 * A simple utility to capitalize the first letter of a string.
 * @param {string} str - The input string.
 * @returns {string} The string with the first letter capitalized, or an empty string if input is not a string or empty.
 *
 * @example
 * capitalizeFirstLetter('hello'); // "Hello"
 */
export function capitalizeFirstLetter(str) {
  if (typeof str !== 'string' || str.length === 0) {
    return '';
  }
  return str.charAt(0).toUpperCase() + str.slice(1);
}

// Add other utility functions here as needed.
// For example:
// export function truncateText(text, maxLength) { ... }
// export function isValidEmail(email) { ... }
