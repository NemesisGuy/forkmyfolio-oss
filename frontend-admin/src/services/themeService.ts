import { ref, watch } from 'vue';

/**
 * @file src/services/themeService.ts
 * @description A reactive service to manage and persist the application's theme ('light' or 'dark').
 */

type Theme = 'light' | 'dark';

const currentTheme = ref<Theme>((localStorage.getItem('theme') as Theme) || 'light');

/**
 * A watcher that reacts to changes in `currentTheme`.
 */
watch(currentTheme, (newTheme) => {
  document.documentElement.setAttribute('data-bs-theme', newTheme);
  localStorage.setItem('theme', newTheme);
}, {
  immediate: true
});

/**
 * A composable function that provides access to the theme state and a function to toggle it.
 */
export function useTheme() {
  const toggleTheme = (): void => {
    currentTheme.value = currentTheme.value === 'light' ? 'dark' : 'light';
  };

  return {
    currentTheme,
    toggleTheme
  };
}
