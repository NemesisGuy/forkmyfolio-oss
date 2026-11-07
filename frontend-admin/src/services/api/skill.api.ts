import { fetchWithAuth } from './apiClient';
import type { Skill } from '@/types';

const PLATFORM_SKILLS_URL = '/skills';

/**
 * API service for fetching the global pool of skills.
 */
export const platformSkillApi = {
  /**
   * Retrieves the list of all skills available on the platform.
   * This is an authenticated endpoint.
   */
  getAll(): Promise<Skill[]> {
    // Endpoint: GET /api/v1/skills
    return fetchWithAuth<Skill[]>(PLATFORM_SKILLS_URL, { method: 'GET' });
  },
};
