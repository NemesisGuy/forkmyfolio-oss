import { fetchWithAuth } from './apiClient';
import type {
  Skill,
  User,
  Profile,
  Project,
  Experience,
  Qualification,
  Testimonial,
  ContactMessage,
  AdminSetting
} from '@/types';

/**
 * @file src/services/api/user.api.ts
 * @description API functions for all authenticated user-specific operations.
 */

// --- User Account & Public Profile ---

/**
 * Fetches the private account details for the authenticated user.
 */
export const getMyAccount = (): Promise<User> => {
  return fetchWithAuth<User>('/me', { method: 'GET' });
};

/**
 * Updates the private account details for the authenticated user.
 */
export const updateMyAccount = (accountData: Partial<User>): Promise<User> => {
  return fetchWithAuth<User>('/me', { method: 'PUT', body: accountData });
};

/**
 * Fetches the public-facing profile details for the authenticated user to edit.
 */
export const getMyPublicProfile = (): Promise<Profile> =>
  fetchWithAuth<Profile>('/me/profile', { method: 'GET' });

/**
 * Updates the public profile for the authenticated user.
 */
export const updateMyPublicProfile = (profileData: Partial<Profile>): Promise<Profile> => {
  return fetchWithAuth<Profile>('/me/profile', {
    method: 'PUT',
    body: profileData,
  });
};

/**
 * Updates the master public visibility of the authenticated user's portfolio.
 */
export const updateMyProfileVisibility = (visibilityData: { isPublic: boolean }): Promise<void> => {
  return fetchWithAuth<void>('/me/profile/visibility', {
    method: 'PUT',
    body: visibilityData,
  });
};

/**
 * Phase 10 (Workstream 2): updates the authenticated user's search-engine
 * indexing preference. Independent of visibility — a public portfolio can
 * opt out of search indexing while remaining reachable via its direct URL.
 */
export const updateMyProfileIndexing = (indexingData: { isSearchIndexable: boolean }): Promise<void> => {
  return fetchWithAuth<void>('/me/profile/indexing', {
    method: 'PUT',
    body: indexingData,
  });
};

/**
 * Changes the password for the currently authenticated user.
 */
export const changeMyPassword = (newPassword: string): Promise<void> => {
  return fetchWithAuth<void>('/me/password', {
    method: 'POST',
    body: { newPassword },
  });
};

/**
 * Records that the authenticated user has accepted the terms and conditions.
 */
export const acceptMyTerms = (): Promise<void> => {
  return fetchWithAuth<void>('/me/accept-terms', {
    method: 'POST',
  });
};

// --- Generic CRUD factory for /me resources ---
const createCrudFunctions = <T>(resource: string) => ({
  getAll: (): Promise<T[]> => fetchWithAuth<T[]>(`/me/${resource}`),
  getById: (uuid: string): Promise<T> => fetchWithAuth<T>(`/me/${resource}/${uuid}`),
  create: (data: Partial<T>): Promise<T> => fetchWithAuth<T>(`/me/${resource}`, { method: 'POST', body: data }),
  update: (uuid: string, data: Partial<T>): Promise<T> => fetchWithAuth<T>(`/me/${resource}/${uuid}`, { method: 'PUT', body: data }),
  remove: (uuid: string): Promise<void> => fetchWithAuth<void>(`/me/${resource}/${uuid}`, { method: 'DELETE' }),
});

// --- Export CRUD functions for each resource ---
export const projectsApi = createCrudFunctions<Project>('projects');
export const experiencesApi = createCrudFunctions<Experience>('experiences');
export const qualificationsApi = createCrudFunctions<Qualification>('qualifications');
export const testimonialsApi = createCrudFunctions<Testimonial>('testimonials');

// --- Skills API ---
const USER_SKILLS_RESOURCE = '/user/skills';
export const skillsApi = {
  getAll: (): Promise<Skill[]> => fetchWithAuth<Skill[]>(USER_SKILLS_RESOURCE),
  create: (data: Partial<Skill>): Promise<Skill> => fetchWithAuth<Skill>(USER_SKILLS_RESOURCE, { method: 'POST', body: data }),
  update: (uuid: string, data: Partial<Skill>): Promise<Skill> => fetchWithAuth<Skill>(`${USER_SKILLS_RESOURCE}/${uuid}`, {
    method: 'PUT',
    body: data
  }),
  remove: (uuid: string): Promise<void> => fetchWithAuth<void>(`${USER_SKILLS_RESOURCE}/${uuid}`, { method: 'DELETE' }),
};

// --- Contact Messages (User-specific) ---
export const getMyContactMessages = (): Promise<ContactMessage[]> => fetchWithAuth<ContactMessage[]>('/me/contact-messages');

export const deleteMyContactMessage = (uuid: string): Promise<void> => fetchWithAuth<void>(`/me/contact-messages/${uuid}`, { method: 'DELETE' });

/**
 * Fetches the count of unread messages for the authenticated user.
 */
export const getUnreadMessageCount = async (): Promise<number> => {
  const response = await fetchWithAuth<{ unreadCount: number }>('/me/contact-messages/unread-count');
  return response.unreadCount;
};

/**
 * Partially updates a contact message for the authenticated user.
 */
export const updateMyContactMessage = (uuid: string, updateData: Partial<ContactMessage>): Promise<ContactMessage> => {
  return fetchWithAuth<ContactMessage>(`/me/contact-messages/${uuid}`, {
    method: 'PUT',
    body: updateData,
  });
};

// --- Settings API ---
export const settingsApi = {
  getAll: (): Promise<AdminSetting[]> => fetchWithAuth<AdminSetting[]>('/me/settings'),
  update: (settingsList: Partial<AdminSetting>[]): Promise<AdminSetting[]> => fetchWithAuth<AdminSetting[]>('/me/settings', { method: 'PUT', body: settingsList }),
};

// --- Backup & Restore (User-specific) ---
export const downloadMyBackup = async (backupToken?: string): Promise<void> => {
  const headers: Record<string, string> = {};
  if (backupToken) headers['X-Backup-Token'] = backupToken;
  const blob = await fetchWithAuth<Blob>('/me/backup', {
    method: 'GET',
    responseType: 'blob',
    headers
  });

  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  const filename = `portfolio-backup-${new Date().toISOString().split('T')[0]}.json`;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

/**
 * Restores a user's portfolio from a backup file.
 */
export const restoreMyBackup = (file: File, backupToken?: string): Promise<void> => {
  const formData = new FormData();
  formData.append('file', file);
  const headers: Record<string, string> = {};
  if (backupToken) headers['X-Backup-Token'] = backupToken;
  return fetchWithAuth<void>('/me/backup/restore', { method: 'POST', body: formData, headers });
};
