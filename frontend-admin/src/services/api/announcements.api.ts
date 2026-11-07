import { fetchWithAuth } from './apiClient';
import type { Announcement } from '@/types';

/**
 * Fetches all announcements relevant to the current user.
 */
export const getMyAnnouncements = (): Promise<Announcement[]> =>
  fetchWithAuth<Announcement[]>('/announcements', { method: 'GET' });

/**
 * Creates a new announcement (Admins only).
 */
export const createAnnouncement = (data: Partial<Announcement>): Promise<Announcement> =>
  fetchWithAuth<Announcement>('/admin/announcements', {
    method: 'POST',
    body: data
  });

/**
 * Fetches all announcements (Admins only).
 */
export const getAllAnnouncements = (): Promise<Announcement[]> =>
  fetchWithAuth<Announcement[]>('/admin/announcements', { method: 'GET' });

/**
 * Marks a specific announcement as read.
 */
export const markAnnouncementAsRead = (uuid: string): Promise<void> =>
  fetchWithAuth<void>(`/announcements/read/${uuid}`, { method: 'POST' });

/**
 * Marks all announcements as read for the current user.
 */
export const markAllAnnouncementsAsRead = (): Promise<void> =>
  fetchWithAuth<void>('/announcements/read/all', { method: 'POST' });

/**
 * Deletes an announcement (Admins only).
 */
export const deleteAnnouncement = (uuid: string): Promise<void> =>
  fetchWithAuth<void>(`/admin/announcements/${uuid}`, { method: 'DELETE' });
