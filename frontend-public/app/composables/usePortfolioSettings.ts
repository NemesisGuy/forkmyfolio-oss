import { useState, computed } from '#imports';

/**
 * @file app/composables/usePortfolioSettings.ts
 * @description Settings service for the public portfolio. Singleton via useState.
 * Ported from ForkMyFolio-frontend-vue/src/services/settingsService.ts.
 *
 * `isEnabled` mirrors the original implementation: a function that checks the
 * normalized settings map for the literal string 'true'.
 */

export interface PortfolioSettings {
  settings: Record<string, string>;
  loading: boolean;
  loadedKey: string;
}

/**
 * Reads the shared settings state (one instance per app).
 */
export function usePortfolioSettings() {
  const state = useState<PortfolioSettings>('portfolio-settings', () => ({
    settings: {},
    loading: false,
    loadedKey: '',
  }));

  const { fetchJson } = useApi();

  const isEnabled = computed((): ((featureName: string) => boolean) =>
    (featureName) => state.value.settings[featureName] === 'true'
  );

  const loading = computed(() => state.value.loading);

  /**
   * Initializes settings for the given slug (or global settings when absent).
   * Mirrors settingsService.initialize: fetches, normalizes [{name,value}] to a
   * Record, caches per key, and never throws.
   */
  const initialize = async (slug?: string): Promise<void> => {
    const key = slug || 'global';
    if (state.value.loading || state.value.loadedKey === key) return;
    state.value.loading = true;
    try {
      const path = slug ? `/portfolios/${slug}/settings` : '/settings';
      const data = await fetchJson(path);
      const arr = Array.isArray(data) ? data : [];
      const map: Record<string, string> = {};
      arr.forEach((s: { name?: string; value?: string }) => {
        if (s && typeof s.name === 'string') map[s.name] = s.value === undefined ? '' : String(s.value);
      });
      // Only commit if the key has not changed while we were fetching.
      state.value.settings = map;
      state.value.loadedKey = key;
    } catch {
      // Navigation resilience: failed settings never crash the page — items stay hidden.
      state.value.settings = {};
      state.value.loadedKey = key;
    } finally {
      state.value.loading = false;
    }
  };

  return {
    settings: computed(() => state.value.settings),
    isEnabled,
    loading,
    initialize,
  };
}
