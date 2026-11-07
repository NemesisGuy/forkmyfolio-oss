/**
 * @file server/middleware/seo-headers.ts
 * @description Belt-and-braces crawler control: when the deployment is
 * non-indexable (NUXT_PUBLIC_INDEXABLE unset/false — the default), every SSR
 * response carries `X-Robots-Tag: noindex, nofollow`. This protects URLs even
 * if a crawler ignores robots.txt (robots.txt is advisory; the header is a
 * directive).
 */
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const indexable = String(config.public.indexable ?? 'false').toLowerCase() === 'true';
  if (!indexable) {
    setResponseHeader(event, 'X-Robots-Tag', 'noindex, nofollow');
  }
});
