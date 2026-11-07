/**
 * @file src/services/api/ApiError.js
 * @description Custom error class for handling API-specific errors.
 */

export class ApiError extends Error {
  /**
   * @param {string} message The primary error message.
   * @param {number} httpStatus The HTTP status code from the response.
   * @param {Array<{field?: string, message: string}>} [errors=[]] An array of detailed validation errors.
   */
  constructor(message, httpStatus, errors = []) {
    super(message);
    this.name = 'ApiError';
    this.httpStatus = httpStatus;
    this.errors = errors; // Array of { field, message } from the backend
  }
}
