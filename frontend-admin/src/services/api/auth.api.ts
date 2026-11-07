import { fetchWithAuth } from './apiClient';

/**
 * @file src/services/api/auth.api.ts
 * @description API functions for authentication-related endpoints.
 */

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
}

/**
 * Register a new user.
 */
export const register = (userData: Record<string, unknown>): Promise<AuthResponse> =>
  fetchWithAuth<AuthResponse>('/auth/register', {
    method: 'POST',
    body: userData,
  }, false);

/**
 * Login an existing user.
 */
export const login = (credentials: Record<string, unknown>): Promise<AuthResponse> =>
  fetchWithAuth<AuthResponse>('/auth/login', {
    method: 'POST',
    body: credentials,
  }, false);

/**
 * Refresh the access token using the refresh token cookie.
 */
export const refreshToken = (): Promise<AuthResponse> =>
  fetchWithAuth<AuthResponse>('/auth/refresh-token', { method: 'POST' }, true, true);

/**
 * Logout the current user.
 * UAT-2 hardening: logout carries its own short 5s abort signal (the apiClient
 * honours caller-owned signals; its own global cap is 15s). A slow or hung
 * server therefore cannot stall the client for long — the navbar navigates to
 * /login in `finally`, and the request still reaches the backend so the refresh
 * cookie is cleared even when we stop waiting for the response.
 */
export const logout = (): Promise<void> => {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 5000);
  return fetchWithAuth<void>('/auth/logout', { method: 'POST', signal: controller.signal }, false)
    .finally(() => clearTimeout(timer));
};

/**
 * Verify current user's password and obtain a backup-scoped token.
 */
export const verifyBackupPassword = (password: string): Promise<{ valid: boolean; backupToken?: string }> =>
  fetchWithAuth<{ valid: boolean; backupToken?: string }>('/auth/verify-password', {
    method: 'POST',
    body: { password },
  });
