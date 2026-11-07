import { computed, ref } from 'vue';
import { publicApi } from '@/services/api/public.api';
import { settingsApi } from '@/services/api/user.api';
import { authService } from './authService';
import type { AdminSetting } from '@/types';

/**
 * @file src/services/settingsService.ts
 * @description A reactive, centralized service for managing application and user settings.
 */

// --- Reactive State ---
const settings = ref<Record<string, string>>({});
const isLoading = ref(true);
const currentSlug = ref<string | null>(null);

/**
 * Fetches settings from the backend based on the target slug and authentication state.
 */
async function _fetchAndApplySettings(slug?: string, force = false): Promise<void> {
  const isAuth = authService.isAuthenticated.value;
  const currentUserSlug = authService.user.value?.slug;
  const normalizedSlug = slug || (isAuth ? currentUserSlug : 'default') || 'default';

  // CACHE CHECK
  if (!force && currentSlug.value === normalizedSlug && Object.keys(settings.value).length > 0) {
    isLoading.value = false;
    return;
  }

  isLoading.value = true;

  try {
    let settingsArray: AdminSetting[];
    if (slug && slug !== 'default') {
      if (isAuth && slug === currentUserSlug) {
        settingsArray = await settingsApi.getAll();
      } else {
        settingsArray = await publicApi.getPortfolioSettings(slug);
      }
    } else if (isAuth && !slug) {
      settingsArray = await settingsApi.getAll();
    } else {
      settingsArray = await publicApi.getGlobalSettings();
    }

    updateSettings(settingsArray);
    currentSlug.value = normalizedSlug;
  } catch (err) {
    console.error('[SettingsService] Failed to fetch settings:', err);
    settings.value = {};
    currentSlug.value = null;
  } finally {
    isLoading.value = false;
  }
}

/**
 * Updates the central reactive settings state from an array of setting objects.
 */
function updateSettings(settingsArray: AdminSetting[]): void {
  if (!Array.isArray(settingsArray)) {
    console.error('[SettingsService] updateSettings received invalid data:', settingsArray);
    return;
  }
  // Normalize the array into a key-value map.
  const newSettingsMap = settingsArray.reduce((acc, setting) => {
    acc[setting.name] = setting.value;
    return acc;
  }, {} as Record<string, string>);

  settings.value = newSettingsMap;
}

/**
 * The settings service, providing reactive state and methods for managing application settings.
 */
export const settingsService = {
  isLoading,
  settings,
  initialize: _fetchAndApplySettings,
  updateSettings,
  isEnabled: computed(() => (featureName: string) => {
    return settings.value[featureName] === 'true';
  }),
};
