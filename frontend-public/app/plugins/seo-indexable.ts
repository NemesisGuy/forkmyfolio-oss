/**
 * @file app/plugins/seo-indexable.ts
 * @description Global crawler meta tag. When the deployment is non-indexable
 * (NUXT_PUBLIC_INDEXABLE unset/false — deny-by-default), every page renders
 * `<meta name="robots" content="noindex, nofollow">` in the SSR HTML.
 *
 * When the deployment IS indexable this plugin does nothing — per-page robots
 * directives are then owned exclusively by `usePortfolioSeo` (single source
 * of truth, no duplicate meta tags).
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig();
  const indexable = String(config.public.indexable ?? 'false').toLowerCase() === 'true';
  if (!indexable) {
    useHead({
      meta: [{ name: 'robots', content: 'noindex, nofollow' }],
    });
  }
});
