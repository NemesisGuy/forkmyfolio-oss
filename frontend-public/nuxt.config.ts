// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  components: [
    {
      path: '~/components',
      pathPrefix: false,
    },
  ],
  app: {
    head: {
      title: 'ForkMyFolio',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' }
      ],
      link: [
        // UAT-3 FIX: SSR pages emitted no favicon link at all; browsers fell back
        // to /favicon.ico which nginx routes to the Vue SPA container — an explicit
        // link makes the tab icon deterministic for every page.
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'stylesheet', href: 'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css' },
        { rel: 'stylesheet', href: 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css', integrity: 'sha512-SnH5WK+bZxgPHs44uWIX+LLJAJ9/2PkPKZ5QiAj6Ta86w+fsb2TkcmfRyVX3pBnMFcV7oQPJkl9QevSCWr3W6A==', crossorigin: 'anonymous', referrerpolicy: 'no-referrer' },
        { rel: 'stylesheet', href: 'https://cdn.jsdelivr.net/gh/devicons/devicon@v2.15.1/devicon.min.css' }
      ]
    }
  },
  css: [
    'bootstrap/dist/css/bootstrap.min.css',
    '@/assets/css/common.css',
    '@/assets/css/glass.css'
  ],
  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || '/api/v1',
      // SEO v3.1.2: deny-by-default crawler switch. Only an explicit "true"
      // (set in the PRODUCTION compose) allows indexing; dev/staging compose
      // sets "false" and gets Disallow-all robots.txt, noindex meta,
      // X-Robots-Tag headers and a 404ing sitemap.
      indexable: process.env.NUXT_PUBLIC_INDEXABLE || 'false'
    },
    apiBaseUrl: process.env.NUXT_API_BASE_URL || 'http://app:8080/api/v1'
  }
})
