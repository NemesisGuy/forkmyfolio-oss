import { fetchWithAuth } from './apiClient';

export interface Policy {
  id?: number;
  type: string;
  content: string;
  version: string;
  active: boolean;
  publishedAt?: string;
}

/**
 * Fetches the current active Terms of Service.
 */
export const getTermsOfService = (): Promise<Policy> =>
  fetchWithAuth<Policy>('/policy/terms', { method: 'GET' }, false);

/**
 * Fetches the current active Privacy Policy.
 */
export const getPrivacyPolicy = (): Promise<Policy> =>
  fetchWithAuth<Policy>('/policy/privacy', { method: 'GET' }, false);

/**
 * Fetches all available policies (Admin only).
 */
export const getAllPolicies = (): Promise<Policy[]> =>
  fetchWithAuth<Policy[]>('/admin/policies', { method: 'GET' });
