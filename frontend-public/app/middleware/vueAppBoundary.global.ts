/**
 * @file app/middleware/vueAppBoundary.global.ts
 * @description UAT-6 FIX (site-wide "Login shows Portfolio Not Available" defect).
 *
 * The authentication / admin screens live in the Vue SPA
 * (ForkMyFolio-frontend-vue) and are routed there by nginx proxy rules:
 *   /login, /register, /force-password-change, /accept-terms,
 *   /guest-access, /admin*
 *
 * Inside the *running* Nuxt SPA, those same paths match the `[slug]`
 * catch-all route, so an internal navigation (NuxtLink, navigateTo,
 * router.push) never reaches nginx: the [slug] page renders, fetches
 * /api/v1/portfolios/login -> 404 -> "Portfolio Not Available" for slug
 * "login". Every boundary navigation must therefore be a FULL PAGE LOAD
 * so nginx can split the traffic to the right app.
 *
 * This global middleware intercepts any internal navigation to those
 * reserved paths and converts it into an external (document-level)
 * navigation. The navbar now uses plain anchors for these links; this
 * middleware guarantees no other code path can swallow them again.
 *
 * Server-side: direct SSR hits of these paths are already intercepted by
 * nginx (never served by Nuxt in staging), so we bail out there to avoid
 * redirect loops in bare-Nuxt dev setups (localhost:3000).
 */
export default defineNuxtRouteMiddleware((to) => {
  if (import.meta.server) return;

  const vueAppBoundary = new Set([
    '/login',
    '/register',
    '/force-password-change',
    '/accept-terms',
    '/guest-access',
  ]);
  if (vueAppBoundary.has(to.path) || to.path.startsWith('/admin')) {
    return navigateTo(to.fullPath, { external: true });
  }
});
