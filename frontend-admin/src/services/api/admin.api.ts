import { fetchWithAuth } from './apiClient';
import { getFilenameFromResponse, triggerDownload } from '@/utils/downloadUtils';
import type {
  User,
  Project,
  AdminSetting,
  ContactMessage,
  PaginatedResponse
} from '@/types';

/**
 * @file src/services/api/admin.api.ts
 * @description API functions for all admin-only operations.
 */

// --- Helper to normalize user data from the backend ---
const normalizeUser = (user: User): User => {
  if (user && user.roles && Array.isArray(user.roles)) {
    // Ensure all roles are clean (e.g., "ADMIN" instead of "ROLE_ADMIN")
    user.roles = user.roles.map(role => role.replace('ROLE_', ''));
  }
  return user;
};

// --- User Management (Admin) ---

/**
 * Creates a new user as an admin.
 */
export const createAdminUser = (userData: Partial<User>): Promise<User> =>
  fetchWithAuth<User>('/admin/users', {
    method: 'POST',
    body: userData
  });

/**
 * Fetches a list of all users.
 */
export const getAdminUsers = async (pageable = { page: 0, size: 20 }): Promise<PaginatedResponse<User>> => {
  const params = new URLSearchParams(pageable as unknown as Record<string, string>);
  const page = await fetchWithAuth<PaginatedResponse<User>>(`/admin/users?${params.toString()}`, { method: 'GET' });
  if (page && page.content) {
    page.content = page.content.map(normalizeUser);
    return page;
  }
  return { content: [], totalElements: 0, totalPages: 0 };
};

/**
 * Fetches a single user by their ID and normalizes the data.
 */
export const getAdminUserById = async (userId: string): Promise<User> => {
  const user = await fetchWithAuth<User>(`/admin/users/${userId}`, { method: 'GET' });
  return normalizeUser(user);
};

/**
 * Updates an existing user's details as an admin.
 */
export const updateAdminUser = (userId: string, userData: Partial<User>): Promise<User> =>
  fetchWithAuth<User>(`/admin/users/${userId}`, {
    method: 'PUT',
    body: userData
  });

/**
 * Deactivates (soft deletes) a user.
 */
export const deleteAdminUser = (userId: string): Promise<void> =>
  fetchWithAuth<void>(`/admin/users/${userId}`, { method: 'DELETE' });

/**
 * Fetches a list of all projects in the system.
 */
export const getAdminProjects = (): Promise<Project[]> =>
  fetchWithAuth<Project[]>('/admin/projects', { method: 'GET' });

// --- Application Settings ---

/**
 * Fetches all global application settings from the backend.
 */
export const getAdminSettings = (): Promise<AdminSetting[]> =>
  fetchWithAuth<AdminSetting[]>('/admin/settings', { method: 'GET' });

/**
 * Updates multiple application settings at once.
 */
export const updateAdminSettings = (settings: Partial<AdminSetting>[]): Promise<AdminSetting[]> => {
  return fetchWithAuth<AdminSetting[]>('/admin/settings', {
    method: 'PUT',
    body: settings
  });
};

// --- Site-wide Statistics ---
export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  totalProjects: number;
  totalExperiences: number;
  totalQualifications: number;
  totalTestimonials: number;
  totalMessages: number;
  unreadMessages: number;
  activeGuestPasses: number;
}

/**
 * Fetches the consolidated visitor and authentication statistics.
 */
export const getAdminStats = (): Promise<AdminStats> =>
  fetchWithAuth<AdminStats>('/admin/stats', { method: 'GET' });

// --- Site-wide Contact Messages ---

/**
 * Fetches all received contact messages from all users.
 */
export const getAdminContactMessages = (): Promise<ContactMessage[]> =>
  fetchWithAuth<ContactMessage[]>('/admin/contact-messages', { method: 'GET' });

/**
 * Deletes a specific contact message by its UUID.
 */
export const deleteAdminContactMessage = (uuid: string): Promise<void> =>
  fetchWithAuth<void>(`/admin/contact-messages/${uuid}`, { method: 'DELETE' });

// --- System Backup & Restore ---

/**
 * Initiates a download of the full system backup JSON file.
 */
export const downloadSystemBackup = async (backupToken?: string): Promise<void> => {
  const headers: Record<string, string> = {};
  if (backupToken) headers['X-Backup-Token'] = backupToken;
  const response = await fetchWithAuth<Response>('/admin/backup', {
    method: 'GET',
    responseType: 'raw',
    headers
  }) as unknown as Response;
  const blob = await response.blob();
  const filename = getFilenameFromResponse(response, `forkmyfolio-system-backup-${new Date().toISOString().split('T')[0]}.json`);
  triggerDownload(blob, filename);
};

/**
 * Uploads a backup file to restore the entire system.
 */
export const restoreSystemBackup = (formData: FormData, backupToken?: string): Promise<void> => {
  const headers: Record<string, string> = {};
  if (backupToken) headers['X-Backup-Token'] = backupToken;
  return fetchWithAuth<void>('/admin/backup/restore/system', {
    method: 'POST',
    body: formData,
    headers
  });
};

/**
 * Uploads a user backup file to restore a single user's portfolio.
 */
export const restoreSingleUserBackup = (userUuid: string, formData: FormData): Promise<void> => {
  return fetchWithAuth<void>(`/admin/backup/restore/user/${userUuid}`, {
    method: 'POST',
    body: formData,
  });
};

/**
 * Wipes all data from the system.
 */
export const wipeSystemData = (): Promise<void> => {
  return fetchWithAuth<void>('/admin/backup/wipe', {
    method: 'DELETE',
  });
};
