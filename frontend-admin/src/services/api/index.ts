/**
 * @file src/services/api/index.ts
 * @description Barrel file to export all API functions for easy importing.
 * This makes it simple to import any API function from a single location.
 */

export * from './public.api';
export * from './admin.api';
export * from './auth.api';
export * from './user.api';
export * from './skill.api';
export * from './announcements.api';
export * from './guestPass.api';
export * from './policy.api';
export * from './analytics.api';

// Also export the custom error class for convenience
export { ApiError } from './ApiError';
