<script setup lang="ts">
import {RouterView, useRoute} from 'vue-router';
import AppNavbar from '@/components/common/AppNavbar.vue';
import AppFooter from '@/components/common/AppFooter.vue';
import NotificationContainer from '@/components/common/NotificationContainer.vue';
import PrivateLockScreen from '@/components/common/PrivateLockScreen.vue';
import {useTheme} from '@/services/themeService.js';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {authService} from '@/services/authService.js';
import {PUBLIC_PORTFOLIO_ROUTE_NAMES} from '@/router';
import {computed} from 'vue';
import type { User } from '@/types';

/**
 * @file src/App.vue
 * @description Main application component. Sets up the global layout including AppNavbar,
 *              the main content area for routes, and an AppFooter.
 */
useTheme();

const route = useRoute();
const {isPrivate} = usePublicPortfolioStore();

// UAT-1 FIX: the global "private/missing portfolio" lock screen is only allowed
// to gate PUBLIC portfolio routes. Previously it rendered on every route where
// the shared store happened to hold `isPrivate=true` (e.g. after a failed
// portfolio fetch), so it covered the login/register forms and dashboards.
const isOnPublicPortfolioRoute = computed(() =>
  PUBLIC_PORTFOLIO_ROUTE_NAMES.has(String(route.name ?? ''))
);

// Owner is derived from the ROUTE slug, not the store's currentSlug: on a
// failed fetch the store clears currentSlug, which used to make the owner of
// a private portfolio appear anonymous to this check.
const isOwner = computed(() => {
  const user = authService.user.value as User | null;
  const slug = route.params?.slug as string | undefined;
  if (!authService.isAuthenticated.value || !user || !slug) return false;
  return user.slug === slug;
});

const showLockScreen = computed(() => isOnPublicPortfolioRoute.value && isPrivate.value && !isOwner.value);
</script>

<template>
  <div id="app-layout">
    <AppNavbar/>

    <!--
      GLOBAL LAYOUT FIX:
      'flex-grow-1' ensures the main content area fills all available vertical space.
      'animated-gradient-background' is now here so it covers the entire app background
      consistently, preventing white gaps or excessive scrolling.
    -->
    <main class="main-content flex-grow-1 animated-gradient-background">
      <PrivateLockScreen v-if="showLockScreen" />
      <RouterView v-else />
    </main>

    <NotificationContainer />
    <AppFooter/>
  </div>
</template>

<style scoped>
/* Scoped styles for App.vue if needed. Global styles are in main.css or imported via Bootstrap. */
/* Ensure container pushes footer down */
#app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh; /* Primary fallback */
  min-height: 100dvh; /* Dynamic viewport height for mobile browsers */
  margin: 0;
  overflow-x: hidden;
}

main.main-content {
  flex: 1 0 auto; /* Ensures main takes remaining space and doesn't shrink inappropriately */
  display: flex;
  flex-direction: column;
  width: 100%;
}
</style>
