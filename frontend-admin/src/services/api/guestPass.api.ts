import { fetchWithAuth } from './apiClient';
import type { GuestPass } from '@/types';

interface AuthResponse {
  accessToken: string;
  user: import('@/types').User;
}

/**
 * @file src/services/api/guestPass.api.ts
 * @description API functions for managing and using guest passes.
 */

/**
 * Creates a new guest pass.
 * @param label - A descriptive label for the pass.
 * @param durationInHours - How long the pass should be valid for.
 * @returns The created guest pass.
 */
export async function createGuestPass(label: string, durationInHours: number): Promise<GuestPass> {
    const params = new URLSearchParams({
        label,
        durationInHours: durationInHours.toString()
    });
    return fetchWithAuth(`/management/guest-passes?${params.toString()}`, {
        method: 'POST'
    });
}

/**
 * Retrieves all guest passes owned by the current user.
 * @returns A list of guest passes.
 */
export async function getMyGuestPasses(): Promise<GuestPass[]> {
    return fetchWithAuth('/management/guest-passes', {
        method: 'GET'
    });
}

/**
 * Revokes a guest pass.
 * @param uuid - The UUID of the pass to revoke.
 */
export async function revokeGuestPass(uuid: string): Promise<void> {
    return fetchWithAuth(`/management/guest-passes/${uuid}`, {
        method: 'DELETE'
    });
}

/**
 * Exchanges a guest token for a JWT session.
 * @param token - The secure guest token.
 * @returns The authentication response containing the JWT and owner profile.
 */
export async function loginAsGuest(token: string): Promise<AuthResponse> {
    return fetchWithAuth('/auth/guest-login', {
        method: 'POST',
        body: { token }
    }, false); // Public endpoint, no initial auth required
}
