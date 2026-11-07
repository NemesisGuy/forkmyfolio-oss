// C:/Users/Reign/IdeaProjects/ForkMyFolio-frontend-vue/src/router/index.js
import {createRouter, createWebHistory} from 'vue-router';
import {authService} from '@/services/authService.js';
import {settingsService} from '@/services/settingsService.js';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';

// Helper to check if a user is authenticated
const isAuthenticated = () => authService.isAuthenticated.value;

// Helper for admin-only routes
const isAdmin = () => isAuthenticated() && authService.user.value?.roles.includes('ADMIN');

// RESERVED SLUGS that cannot be used as portfolio identifiers
const RESERVED_SLUGS = new Set([
  'admin', 'manage', 'login', 'register', 'profile', 'settings', 'dashboard', 'home',
  'api', 'auth', 'public', 'user', 'users', 'account', 'terms', 'privacy', 'about',
  'contact', 'help', 'faq', 'blog', 'docs', 'support', 'sitemap', 'robots.txt',
  'favicon.ico', '404', '500', 'assets', 'static', 'uploads', 'images', 'css', 'js',
  'webfonts', 'fonts', 'data', 'exports', 'projects', 'skills', 'experience',
  'testimonials', 'qualifications', 'backup', 'display', 'pdf', 'stats', 'system-backup',
  'edit-account', 'edit-public-profile', 'messages', 'create', 'edit', 'force-password-change',
  'accept-terms', 'me', 'null', 'undefined', 'internal', 'error'
]);

// Public portfolio route names (UAT-1 fix). The ONLY routes where a
// "private/missing portfolio" state from publicPortfolioStore is meaningful.
// Exported so App.vue (lock-screen gating) and the navbar watcher share this
// exact list with the guard — a stale `isPrivate` flag must never leak onto
// login/register/not-found/dashboard pages.
export const PUBLIC_PORTFOLIO_ROUTE_NAMES = new Set([
  'portfolio-home', 'projects-public', 'skills-public', 'experience-public',
  'testimonials-public', 'qualifications-public', 'contact', 'project-details',
]);

const routes = [
  // --- Static Public Routes ---
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/public/LandingPage.vue'),
  },

  // --- Auth Routes ---
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/public/LoginPage.vue'),
    // UPDATED: Redirect to the slug-based dashboard if already logged in.
    beforeEnter: (to, from, next) => (isAuthenticated() ? next({
      name: 'dashboard',
      params: {slug: authService.user.value.slug}
    }) : next()),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/public/SignupPage.vue'),
    // UPDATED: Redirect to the slug-based dashboard if already logged in.
    beforeEnter: (to, from, next) => (isAuthenticated() ? next({
      name: 'dashboard',
      params: {slug: authService.user.value.slug}
    }) : next()),
  },
  {
    path: '/force-password-change',
    name: 'force-password-change',
    component: () => import('@/views/auth/ForcePasswordChangePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/accept-terms',
    name: 'accept-terms',
    component: () => import('@/views/auth/AcceptTermsPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/guest-access',
    name: 'guest-access',
    component: () => import('@/views/public/GuestAccessPage.vue')
  },

  // --- Authenticated User Routes ---
  {
    path: '/:slug/manage/dashboard',
    name: 'dashboard',
    component: () => import('@/views/user/UserDashboardPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/messages',
    name: 'user-messages', // THIS IS THE FIX: Changed from 'my-messages' to match the Navbar link.
    component: () => import('@/views/user/UserMessagesPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/exports',
    name: 'my-data-exports',
    component: () => import('@/views/user/UserDataExportsPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/projects',
    name: 'my-projects',
    component: () => import('@/views/user/UserProjectsPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/skills',
    name: 'my-skills',
    component: () => import('@/views/user/UserSkillsPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/experience',
    name: 'my-experience',
    component: () => import('@/views/user/UserExperiencePage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/testimonials',
    name: 'my-testimonials',
    component: () => import('@/views/user/UserTestimonialsPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/qualifications',
    name: 'my-qualifications',
    component: () => import('@/views/user/UserQualificationsPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/backup',
    name: 'my-backup',
    component: () => import('@/views/user/UserBackupRestorePage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/profile',
    name: 'profile',
    component: () => import('@/views/user/UserAccountPage.vue'), // UPDATED
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/profile/edit-account',
    name: 'edit-account',
    component: () => import('@/views/user/UserAccountEditPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/profile/edit-public-profile',
    name: 'edit-public-profile',
    component: () => import('@/views/user/UserProfileEditPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/guest-passes',
    name: 'my-guest-passes',
    component: () => import('@/views/user/UserGuestPassesPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    // UAT-5 FIX: the "Job Tracking" dashboard card links to name 'job-tracker',
    // but this record was never registered — vue-router's matcher threw a
    // NO_MATCH RouterError (empty-message Error) inside the RouterLink computed
    // on EVERY dashboard render. The tracker page, its components and its
    // service (jobApplicationService.spec.ts) all exist; only the route was
    // missing.
    path: '/:slug/manage/job-tracker',
    name: 'job-tracker',
    component: () => import('@/views/JobApplicationTracker.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/settings/display',
    name: 'display-settings',
    component: () => import('@/views/user/UserSettingsPage.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/settings/pdf',
    name: 'pdf-settings',
    component: () => import('@/views/user/UserPdfSettings.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/settings/export',
    name: 'export-settings',
    component: () => import('@/views/user/UserExportSettings.vue'),
    props: true,
    meta: {requiresAuth: true},
  },
  {
    path: '/:slug/manage/analytics',
    name: 'analytics',
    component: () => import('@/views/user/UserAnalytics.vue'),
    props: true,
    meta: {requiresAuth: true},
  },

  // --- Admin Routes ---
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/admin/AdminDashboardPage.vue'),
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/messages',
    name: 'admin-messages',
    component: () => import('@/views/admin/AdminMessagesPage.vue'),
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/views/admin/AdminUsersPage.vue'),
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/users/create',
    name: 'admin-user-create',
    component: () => import('@/views/admin/AdminUserCreatePage.vue'),
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/users/edit/:id',
    name: 'admin-user-edit',
    component: () => import('@/views/admin/AdminUserEditPage.vue'),
    props: true,
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/settings',
    name: 'admin-settings',
    component: () => import('@/views/admin/AdminSettings.vue'),
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/pdf-settings',
    name: 'admin-pdf-settings',
    component: () => import('@/views/admin/AdminPdfSettings.vue'),
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/stats',
    name: 'admin-stats',
    component: () => import('@/views/admin/AdminStats.vue'),
    meta: {requiresAuth: true, requiresAdmin: true},
  },
  {
    path: '/admin/system-backup',
    name: 'admin-system-backup',
    component: () => import('@/views/admin/AdminSystemBackupPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },

  // --- Dynamic Public Portfolio Routes (MUST BE LAST before 404) ---
  // These routes are now nested under a slug parameter.
  {
    path: '/:slug',
    name: 'portfolio-home', // Renamed from portfolio-slug for clarity
    component: () => import('@/views/public/HomePage.vue'),
    props: true,
  },
  {
    path: '/:slug/projects',
    name: 'projects-public',
    component: () => import('@/views/public/ProjectsPage.vue'),
    props: true,
  },
  {
    path: '/:slug/skills',
    name: 'skills-public',
    component: () => import('@/views/public/SkillsPage.vue'),
    props: true,
  },
  {
    path: '/:slug/experience',
    name: 'experience-public',
    component: () => import('@/views/public/ExperiencePage.vue'),
    props: true,
  },
  {
    path: '/:slug/testimonials',
    name: 'testimonials-public',
    component: () => import('@/views/public/TestimonialsPage.vue'),
    props: true,
  },
  {
    path: '/:slug/qualifications',
    name: 'qualifications-public',
    component: () => import('@/views/public/QualificationsPage.vue'),
    props: true,
  },
  {
    path: '/:slug/contact',
    name: 'contact',
    component: () => import('@/views/public/ContactPage.vue'),
    props: true,
  },
  {
    path: '/:slug/project/:uuid', // Project details now also need the slug for context
    name: 'project-details',
    component: () => import('@/views/public/ProjectDetailsPage.vue'),
    props: true,
  },

  // --- Not Found Route ---
  {
    path: '/not-found',
    name: 'not-found',
    component: () => import('@/views/public/NotFoundPage.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: { name: 'not-found' }
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return {top: 0};
    }
  },
});

router.beforeEach(async (to, from, next) => {
  // --- 0. Global UI Cleanup (FIX for Mobile Back Button / Ghosting) ---
  // Ensure that all Bootstrap modals are closed and overlays are removed before any navigation occurs.
  // This prevents the "blurred overlay" issue when navigating back on mobile.
  try {
    const modals = document.querySelectorAll('.modal.show');
    modals.forEach(modalEl => {
      // Attempt graceful hide via Bootstrap if possible
      const b = window.bootstrap;
      const modalInstance = b?.Modal?.getInstance(modalEl);
      if (modalInstance) {
        try { modalInstance.hide(); } catch { }
      }

      // Mandatory Deep Manual Cleanup (Guarantees removal even if instances fail)
      modalEl.classList.remove('show');
      modalEl.classList.remove('fade');
      modalEl.style.display = 'none';
      modalEl.setAttribute('aria-hidden', 'true');
      modalEl.removeAttribute('aria-modal');
      modalEl.removeAttribute('role');
    });

    // Forced cleanup of backdrops and body state
    const backdrops = document.querySelectorAll('.modal-backdrop, .modal-stack-backdrop');
    backdrops.forEach(b => {
      b.classList.remove('show');
      b.remove();
    });

    document.body.classList.remove('modal-open');
    document.documentElement.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
    document.body.style.filter = ''; // Clear any manual blurs
  } catch (e) {
    console.error('[Router] Global UI cleanup failed:', e);
  }

  // --- 1. Slug Guard ---
  // If the route has a slug parameter, ensure it's not a reserved keyword.
  if (to.params.slug && RESERVED_SLUGS.has(to.params.slug.toLowerCase())) {
     console.warn(`[Router] Access to reserved slug intercepted: ${to.params.slug}`);
     return next({ name: 'not-found', replace: true });
  }

  const user = authService.user.value;
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

  // --- 1. Authentication Check ---
  if (requiresAuth && !isAuthenticated()) {
    return next({name: 'login', query: {redirect: to.fullPath}, replace: true});
  }

  // If the user is authenticated, perform the mandatory sequential checks.
  if (requiresAuth && user) {
    // --- 2. Force Terms Acceptance (Highest Priority) ---
    const needsToAcceptTerms = !user.termsAcceptedAt;
    if (needsToAcceptTerms && to.name !== 'accept-terms') {
      return next({ name: 'accept-terms', replace: true });
    }
    // If they are on the accept terms page, but don't need to be, send them away.
    if (!needsToAcceptTerms && to.name === 'accept-terms') {
      return next({ name: 'dashboard', params: { slug: user.slug } });
    }
    // If they are on the correct page to accept terms, let them pass and stop further checks.
    if (needsToAcceptTerms) return next();

    // --- 3. Force Password Change (Runs only after terms are accepted) ---
    const needsToChangePassword = !user.passwordLastChangedAt;
    if (needsToChangePassword && to.name !== 'force-password-change') {
      return next({ name: 'force-password-change', replace: true });
    }
    // If they are on the change password page, but don't need to be, send them away.
    if (!needsToChangePassword && to.name === 'force-password-change') {
      return next({ name: 'dashboard', params: { slug: user.slug } });
    }
  }

  const publicPortfolioPages = [...PUBLIC_PORTFOLIO_ROUTE_NAMES];

  if (publicPortfolioPages.includes(to.name)) {
    const store = usePublicPortfolioStore();
    const slugToFetch = to.params.slug;

    if (!slugToFetch) {
      console.error(`[Router Guard] Route '${to.name}' is missing the 'slug' parameter. Redirecting to home.`);
      return next({name: 'home'});
    }

    // TIGHTEN: Only await if the slug is different or data is missing.
    // The store's fetchPortfolio and settingsService.initialize now have internal cache checks.
    await Promise.all([
      store.fetchPortfolio(slugToFetch),
      settingsService.initialize(slugToFetch)
    ]);

    if (store.error.value) {
      const status = store.error.value.httpStatus || store.error.value.response?.status;
      if (status === 404) {
        return next({ name: 'not-found', replace: true });
      }

      // NEW: Handle Guest Pass Revocation/Expiration (401 or 403 on private portfolio)
      if (status === 401 || (status === 403 && store.isPrivate.value)) {
        // If they were trying to access a guest session, redirect to verification page
        const isGuest = sessionStorage.getItem('isGuestSession') === 'true';
        if (isGuest) {
           // We don't have the original token here easily, but we can redirect to the login page
           // or the guest-access page with a clear error.
           return next({ name: 'guest-access', query: { error: 'invalid' } });
        }
      }
    }
  } else {
    // UAT-1 FIX: clear public-portfolio state on EVERY non-portfolio destination
    // (login, register, guest-access, not-found, dashboards, admin...). Previously
    // only 'home'/'guest-access' cleared it, so a stale `isPrivate` from a failed
    // portfolio fetch survived and covered the LOGIN page with the lock screen.
    const store = usePublicPortfolioStore();
    store.clearPortfolio();

    if (to.name === 'home') {
      // For the main landing page, initialize with global defaults.
      await settingsService.initialize('default');
    } else if (requiresAuth) {
      // For authenticated user pages, initialize with their own settings.
      await settingsService.initialize();
    }
  }

  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin);

  // --- NEW: Ownership check for management routes ---
  // A management route has a slug parameter and requires auth, but is not an admin route.
  // This prevents a user from accessing /another-user/manage/dashboard
  if (to.params.slug && requiresAuth && !to.path.startsWith('/admin')) {
    const userSlug = authService.user.value?.slug;
    if (to.params.slug !== userSlug) {
      console.warn(`[Router Guard] Unauthorized access attempt to manage slug '${to.params.slug}' by user with slug '${userSlug}'.`);
      return next({name: 'not-found', replace: true});
    }
  }

  if (requiresAdmin && !isAdmin()) {
    return next({name: 'not-found', replace: true});
  }

  next();
});

export default router;
