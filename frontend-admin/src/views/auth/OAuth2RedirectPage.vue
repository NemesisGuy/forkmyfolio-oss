<template>
  <div class="oauth2-redirect-page container py-5">
    <div class="card glass-card animate-fade-in-up mx-auto" style="max-width: 420px;">
      <div class="card-body p-4">
        <h1 class="card-title h4 text-center mb-4">Social sign-in</h1>
        <div v-if="status === 'error'" class="alert alert-danger" role="alert">
          {{ message }}
        </div>
        <div v-else class="alert alert-info" role="status">
          Completing sign-in, please wait...
        </div>
        <div v-if="status === 'error'" class="text-center">
          <router-link to="/login">Back to login</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * @file src/views/auth/OAuth2RedirectPage.vue
 * @description SPA half of the social/OIDC login. The backend redirects here
 * with `?token=<app JWT>` after a successful callback (or `?error=...` on
 * failure). The token is handed to authService exactly like a password login,
 * then the user lands on their dashboard. No token handling happens in Nuxt.
 */
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authService } from '@/services/authService';
import { settingsService } from '@/services/settingsService.js';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore.js';
import {
  extractOAuth2RedirectParams,
  oauth2ErrorMessage,
} from '@/services/oauth2Redirect';

const route = useRoute();
const router = useRouter();

const status = ref<'loading' | 'error'>('loading');
const message = ref('Completing sign-in, please wait...');

onMounted(async () => {
  const { token, error } = extractOAuth2RedirectParams(route.query);

  if (error || !token) {
    status.value = 'error';
    message.value = oauth2ErrorMessage(error);
    return;
  }

  try {
    await authService.loginWithToken(token);
    const slug = authService.user.value?.slug;
    if (!slug) {
      throw new Error('Social sign-in returned no account slug.');
    }

    // Match LoginPage: refresh settings/store context before the guard runs.
    await settingsService.initialize();
    usePublicPortfolioStore().currentSlug.value = slug;

    // replace() removes the token-bearing URL from browser history.
    await router.replace({ name: 'dashboard', params: { slug } });
  } catch {
    status.value = 'error';
    message.value = oauth2ErrorMessage(null);
  }
});
</script>
