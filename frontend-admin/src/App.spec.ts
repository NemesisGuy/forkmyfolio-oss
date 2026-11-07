/**
 * @file src/App.spec.ts
 * @description Phase 7 UAT-fix regression tests for the GLOBAL lock-screen gating
 *              (UAT-1 bug: "Portfolio Not Available" covered the login form).
 *
 * Root cause: publicPortfolioStore is a module-level singleton. A failed public
 * portfolio fetch set isPrivate=true, and nothing cleared it on login/register/
 * not-found/dashboard routes, so App.vue rendered PrivateLockScreen OVER those
 * pages. Fix: (a) the lock screen is gated to public-portfolio route names only,
 * (b) the owner check derives from route.params.slug instead of the store's
 * currentSlug (which is nulled on fetch errors).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import App from './App.vue';
import { authService } from '@/services/authService.js';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore.js';

// --- Global chrome stubbed away: App.vue's layout is under test, not the navbar ---
vi.mock('@/components/common/AppNavbar.vue', () => ({ default: { template: '<nav data-stub="navbar"/>' } }));
vi.mock('@/components/common/AppFooter.vue', () => ({ default: { template: '<footer data-stub="footer"/>' } }));
vi.mock('@/components/common/NotificationContainer.vue', () => ({ default: { template: '<div data-stub="notifications"/>' } }));
vi.mock('@/services/themeService.js', () => ({ useTheme: vi.fn() }));

const StubPage = { template: '<div data-testid="routed-page">{{ routeName }}</div>', setup: () => ({ routeName: String(useTestRoute().name) }) };
import { useRoute as useTestRoute } from 'vue-router';

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/login', name: 'login', component: StubPage },
    { path: '/register', name: 'register', component: StubPage },
    { path: '/:slug/manage/dashboard', name: 'dashboard', component: StubPage, meta: { requiresAuth: true } },
    { path: '/:slug', name: 'portfolio-home', component: StubPage },
    { path: '/:slug/projects', name: 'projects-public', component: StubPage },
  ],
});

describe('App.vue global lock-screen gating (UAT-1 regression)', () => {
  beforeEach(async () => {
    const store = usePublicPortfolioStore();
    store.clearPortfolio();
    authService.user.value = null;
    authService.isAuthenticated.value = false;
    await router.push('/login');
    await router.isReady();
  });

  it('never shows the lock screen on the LOGIN page even when a stale isPrivate=true lingers', async () => {
    const store = usePublicPortfolioStore();
    // Simulate the poison state: a failed portfolio fetch left these behind.
    store.isPrivate.value = true;
    store.currentSlug.value = null;

    const wrapper = mount(App, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.findComponent({ name: 'PrivateLockScreen' }).exists()).toBe(false);
    expect(wrapper.find('[data-testid="routed-page"]').exists()).toBe(true);
    wrapper.unmount();
  });

  it('never shows the lock screen on the REGISTER page or the dashboard with stale isPrivate=true', async () => {
    const store = usePublicPortfolioStore();
    store.isPrivate.value = true;
    store.currentSlug.value = null;

    await router.push('/register');
    await flushPromises();
    let wrapper = mount(App, { global: { plugins: [router] } });
    await flushPromises();
    expect(wrapper.findComponent({ name: 'PrivateLockScreen' }).exists()).toBe(false);
    wrapper.unmount();

    authService.user.value = { slug: 'someuser' } as never;
    authService.isAuthenticated.value = true;
    await router.push('/someuser/manage/dashboard');
    await flushPromises();
    wrapper = mount(App, { global: { plugins: [router] } });
    await flushPromises();
    expect(wrapper.findComponent({ name: 'PrivateLockScreen' }).exists()).toBe(false);
    wrapper.unmount();
  });

  it('still shows the lock screen to an anonymous visitor on a PRIVATE portfolio page (intended UX)', async () => {
    const store = usePublicPortfolioStore();
    store.isPrivate.value = true;

    await router.push('/nemesis');
    await flushPromises();
    const wrapper = mount(App, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.findComponent({ name: 'PrivateLockScreen' }).exists()).toBe(true);
    wrapper.unmount();
  });

  it('owner of a private portfolio is recognised via the ROUTE slug (owner sees their own page, no lock)', async () => {
    const store = usePublicPortfolioStore();
    store.isPrivate.value = true;
    store.currentSlug.value = null; // store slug cleared by the failed fetch — previously masked ownership

    authService.user.value = { slug: 'nemesis' } as never;
    authService.isAuthenticated.value = true;

    await router.push('/nemesis');
    await flushPromises();
    const wrapper = mount(App, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.findComponent({ name: 'PrivateLockScreen' }).exists()).toBe(false);
    wrapper.unmount();
  });
});
