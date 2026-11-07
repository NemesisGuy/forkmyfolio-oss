import { ref, watch } from 'vue';
import { useHead } from '#app';

/**
 * @file app/composables/useTheme.ts
 * @description Theme service for the application. Singleton state shared across components.
 * Ported from ForkMyFolio-frontend-vue/src/services/themeService.ts.
 *
 * The current theme ('light' | 'dark') is persisted in localStorage and applied
 * by setting `data-bs-theme` on <html> (Bootstrap 5.3 dark mode convention).
 * An inline script (registered once via app.vue) applies the stored theme before
 * first paint to avoid a flash of the wrong theme.
 */

export const currentTheme = ref<'light' | 'dark'>('light');

let initialized = false;

/**
 * Applies the theme attribute to <html> (client-side only).
 */
const applyTheme = (theme: string) => {
  if (process.client) {
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem('theme', theme);
  }
};

export function useTheme() {
  if (!initialized) {
    initialized = true;

    // Restore persisted preference immediately on client start.
    if (process.client) {
      const stored = localStorage.getItem('theme');
      currentTheme.value = stored === 'dark' ? 'dark' : 'light';
      applyTheme(currentTheme.value);
    }

    // Keep <html data-bs-theme> in sync with the service state.
    watch(currentTheme, (newTheme) => applyTheme(newTheme));

    if (process.server) {
      // Respected by the inline pre-paint script emitted once in app.vue.
      useHead({
        script: [
          {
            innerHTML:
              'try{var t=localStorage.getItem("theme");document.documentElement.setAttribute("data-bs-theme",(t==="dark")?"dark":"light");}catch(e){document.documentElement.setAttribute("data-bs-theme","light");}',
            tagPosition: 'head',
          },
        ],
      });
    }
  }

  const toggleTheme = () => {
    currentTheme.value = currentTheme.value === 'dark' ? 'light' : 'dark';
  };

  return {
    currentTheme,
    toggleTheme,
  };
}
