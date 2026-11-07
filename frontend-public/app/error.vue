<script setup lang="ts">
/**
 * @file app/error.vue
 * @description Branded error page (SEO v3.1.2). Nuxt renders this for
 * fatal errors; it guarantees correct HTTP status codes for crawlers and a
 * glass-styled recovery path for humans.
 */
import { AlertTriangle, Home } from 'lucide-vue-next';

const props = defineProps({
  error: {
    type: Object as () => { statusCode?: number; statusMessage?: string; message?: string } | null,
    default: () => null,
  },
});

const status = computed(() => props.error?.statusCode || 500);
const isNotFound = computed(() => status.value === 404);

const title = computed(() => (isNotFound.value ? 'Page not found' : 'Something went wrong'));
const detail = computed(() =>
  isNotFound.value
    ? 'The page you are looking for does not exist or is no longer available.'
    : 'An unexpected error occurred. Please try again in a moment.'
);

// Correct crawler-facing status (Nuxt defaults unexpected errors to 500 but
// some paths can leak 200s; this makes the status deterministic).
if (import.meta.server) {
  setResponseStatus(status.value === 404 ? 404 : 500);
}

useSeoMeta({
  title: () => `${title.value} | ForkMyFolio`,
  robots: 'noindex, nofollow',
});

const goHome = () => {
  clearError({ redirect: '/' });
};
</script>

<template>
  <div class="error-page d-flex align-items-center justify-content-center flex-grow-1 py-5">
    <div class="container text-center">
      <div class="glass-card mx-auto p-5" style="max-width: 560px;">
        <AlertTriangle :size="56" class="mb-3 text-warning" />
        <h1 class="display-5 glass-text mb-2">{{ status }}</h1>
        <h2 class="h4 glass-text mb-3">{{ title }}</h2>
        <p class="glass-subtitle mb-4">{{ detail }}</p>
        <button class="btn btn-primary px-4 interactive-lift" @click="goHome">
          <Home :size="18" class="me-2" /> Back to ForkMyFolio
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  min-height: 70vh;
}
</style>
