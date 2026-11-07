import { defineVitestConfig } from '@nuxt/test-utils/config'

// NOTE (Phase 7.3): the Nuxt frontend previously had ZERO tests. This config
// enables the `nuxt` environment (real Nuxt context + auto-imports +
// composables), so pages can be rendered exactly the way SSR renders them.
export default defineVitestConfig({
  test: {
    environment: 'nuxt',
    environmentOptions: {
      nuxt: {
        // Don't spin up the real SSR dev server; render in-process instead.
        domEnvironment: 'happy-dom',
        mock: {
          intersectionObserver: true,
        },
      },
    },
    testTimeout: 120_000,
    hookTimeout: 120_000,
    include: ['tests/**/*.spec.ts'],
  },
})
