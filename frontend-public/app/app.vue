<script setup lang="ts">
import { useTheme } from '~/composables/useTheme';

/**
 * @file app/app.vue
 * @description Root app component. Mirrors the original App.vue responsibilities:
 * theme service initialization (which registers the pre-paint theme script on SSR).
 */
useTheme();
</script>

<template>
  <div>
    <NuxtRouteAnnouncer />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>
