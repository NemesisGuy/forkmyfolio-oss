import {
    createGuestPass as apiCreate,
    getMyGuestPasses as apiGetMy,
    revokeGuestPass as apiRevoke,
    loginAsGuest as apiLoginAsGuest
} from './api/guestPass.api';
import type { User, GuestPass } from '@/types';

/**
 * @file src/services/guestPassService.ts
 * @description Higher-level service for handling guest pass operations.
 */


export const guestPassService = {
    /**
     * Creates a new guest pass for the current user's portfolio.
     */
    async create(label: string, durationInHours: number): Promise<GuestPass> {
        return await apiCreate(label, durationInHours);
    },

    /**
     * Lists all guest passes for the current user.
     */
    async getMyPasses(): Promise<GuestPass[]> {
        return await apiGetMy();
    },

    /**
     * Revokes (deletes) a specific guest pass.
     */
    async revoke(uuid: string): Promise<void> {
        return await apiRevoke(uuid);
    },

    /**
     * Public access: Login using a guest token.
     * This will return the JWT and the owner's profile.
     */
    async login(token: string): Promise<{ accessToken: string, user: User }> {
        return await apiLoginAsGuest(token);
    }
};
