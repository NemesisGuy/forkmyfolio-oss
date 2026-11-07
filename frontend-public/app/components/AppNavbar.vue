<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { UserCircle, LayoutDashboard, IdCard, Ticket, Home, SlidersHorizontal, FileText, LogOut, MessageCircle } from 'lucide-vue-next';
import ThemeToggle from '~/components/ThemeToggle.vue';
import ConfirmModal from '~/components/modals/ConfirmModal.vue';
import { useTheme } from '~/composables/useTheme';
import { usePortfolioSettings } from '~/composables/usePortfolioSettings';

/**
 * @file app/components/AppNavbar.vue
 * @description The shared application navbar (public context).
 * Ported from ForkMyFolio-frontend-vue/src/components/common/AppNavbar.vue.
 *
 * Public rendering is SSR'd. The authenticated account section is restored
 * client-side (token comes from localStorage/sessionStorage shared with the
 * Vue SPA) and wrapped in <ClientOnly> to avoid hydration mismatches.
 */

const RESERVED_SLUGS = new Set([
  'admin', 'api', 'auth', 'login', 'logout', 'register', 'dashboard',
  'settings', 'profile', 'projects', 'skills', 'experience', 'qualifications', 'contact',
  'swagger-ui', 'v3', 'api-docs', 'h2-console', 'assets', 'static', 'images', 'css', 'js',
  'me', 'system', 'backup', 'restore', 'error', '404', 'config', 'internal', 'null', 'undefined',
  'guest-access', 'accept-terms', 'force-password-change'
]);

const isReservedSlug = (s?: string) => !!s && RESERVED_SLUGS.has(s.toLowerCase());

const { currentTheme } = useTheme();
const route = useRoute();

interface NavUser {
  slug?: string | null;
  firstName?: string | null;
  profileImageUrl?: string | null;
  roles?: string[] | null;
}

const authUser = ref<NavUser | null>(null);
const unreadCount = ref(0);

// Reactive auth/session state, restored client-side only.
const isAuthenticated = ref(false);
const isClientReady = ref(false);

const readToken = (): string | null => {
  try {
    return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
  } catch {
    return null;
  }
};

const restoreSession = async () => {
  const token = readToken();
  if (!token) {
    isAuthenticated.value = false;
    authUser.value = null;
    return;
  }
  try {
    const { fetchJson } = useApi();
    const user = await fetchJson<NavUser>('/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (user) {
      authUser.value = user;
      isAuthenticated.value = true;
      // Unread message badge (mirrors messageNotificationService).
      try {
        const res = await $fetch<{ data?: { unreadCount?: number } | number }>(
          '/api/v1/me/contact-messages/unread-count',
          { headers: { Authorization: `Bearer ${token}` } }
        );
        const d: any = res as any;
        unreadCount.value = typeof d === 'number'
          ? d
          : (typeof d?.unreadCount === 'number'
            ? d.unreadCount
            : typeof d?.data?.unreadCount === 'number' ? d.data.unreadCount : 0);
      } catch {
        unreadCount.value = 0;
      }
    } else {
      isAuthenticated.value = false;
    }
  } catch {
    // Stale or invalid token — treat as logged out on public pages.
    isAuthenticated.value = false;
    authUser.value = null;
  }
};

// --- Portfolio settings are fetched server-side so the nav items render in SSR ---
const { isEnabled, initialize } = usePortfolioSettings();

const effectiveSlug = computed<string | null>(() => {
  const slug = route.params.slug as string | undefined;
  if (!slug || isReservedSlug(slug)) return null;
  return slug;
});

const { error: settingsError } = await useAsyncData(
  `nav-settings-${effectiveSlug.value || 'global'}`,
  () => initialize(effectiveSlug.value || undefined).then(() => true),
  { watch: [effectiveSlug] }
);

const navbarToggler = ref<HTMLElement | null>(null);
const navbarNavCollapsible = ref<HTMLElement | null>(null);

const collapseNavbar = () => {
  if (navbarNavCollapsible.value && navbarNavCollapsible.value.classList.contains('show')) {
    navbarToggler.value?.click();
  }
};

const homeLinkTarget = computed(() => {
  if (effectiveSlug.value) {
    return `/${effectiveSlug.value}`;
  }
  return '/';
});

const isHomeActive = computed(() => {
  if (effectiveSlug.value) {
    return route.path === `/${effectiveSlug.value}`;
  }
  return route.path === '/';
});

const isGuest = computed(() => {
  const user = authUser.value as NavUser | null;
  return !!(user && user.roles && user.roles.includes('GUEST'));
});

const isAdmin = computed(() => {
  const user = authUser.value as NavUser | null;
  return !!(user && user.roles && user.roles.includes('ADMIN'));
});

// --- Logout (confirm modal, mirroring the original) ---
const logoutConfirmModalRef = ref<{ show: () => void } | null>(null);
const logoutConfirmTitle = 'Confirm Logout';
const logoutConfirmMessage = 'Are you sure you want to logout?';

const requestLogoutConfirmation = () => {
  collapseNavbar();
  logoutConfirmModalRef.value?.show();
};

const executeLogout = async () => {
  try {
    const token = readToken();
    if (token) {
      await $fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        ignoreResponseError: true,
      });
    }
  } catch (e) {
    console.error('Error during logout:', e);
  } finally {
    try {
      localStorage.removeItem('accessToken');
      sessionStorage.removeItem('accessToken');
      sessionStorage.removeItem('isGuestSession');
    } catch { /* storage unavailable */ }
    // UAT-6 FIX: /login is Vue-SPA territory (nginx splits it); leaving it to
    // Nuxt's internal routing makes the [slug] catch-all render a dead
    // "Portfolio Not Available" page for slug "login". External => full page
    // load, which routes through nginx to the Vue auth app.
    await navigateTo('/login', { external: true });
  }
};

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

// Keep settings in sync across client-side navigations (mirrors navbar watcher).
watch(effectiveSlug, (slug) => {
  if (process.server) return;
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    if (slug) {
      initialize(slug);
    }
  }, 300);
}, { immediate: false });

// Restore the shared (SPA) session after mount; initial SSR shows public links.
onMounted(async () => {
  isClientReady.value = true;
  await restoreSession();
});

// Re-check auth state when the tab becomes visible (mirrors original services).
const handleVisibilityChange = () => {
  if (!document.hidden) {
    restoreSession();
  }
};

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange);
});

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});
</script>

<template>
  <nav :class="[
    'navbar',
    'navbar-expand-lg',
    'glass-nav',
    currentTheme === 'dark' ? 'navbar-dark' : 'navbar-light'
  ]">
    <div class="container-fluid">
      <NuxtLink class="navbar-brand d-flex align-items-center" to="/">
        <img alt="ForkMyFolio Logo" class="d-inline-block align-text-top me-2 rounded-circle"
             height="45" src="/forkmyfolio_logo_icon.png" width="45">
        ForkMyFolio
      </NuxtLink>

      <button
        ref="navbarToggler"
        aria-controls="navbarNav"
        aria-expanded="false"
        aria-label="Toggle navigation"
        class="navbar-toggler"
        data-bs-target="#navbarNav"
        data-bs-toggle="collapse"
        type="button"
      >
        <span class="navbar-toggler-icon"></span>
      </button>

      <div id="navbarNav" ref="navbarNavCollapsible" class="collapse navbar-collapse">
        <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center">
          <li class="nav-item">
            <NuxtLink
              :class="{ 'active': isHomeActive }"
              :to="homeLinkTarget"
              class="nav-link"
              @click="collapseNavbar">
              Home
            </NuxtLink>
          </li>
          <!-- Public portfolio links that appear based on the current context -->
          <li v-if="isEnabled('portfolio.projects.show') && effectiveSlug" class="nav-item">
            <NuxtLink :to="`/${effectiveSlug}/projects`" active-class="active" class="nav-link" @click="collapseNavbar">
              Projects
            </NuxtLink>
          </li>
          <li v-if="isEnabled('portfolio.skills.show') && effectiveSlug" class="nav-item">
            <NuxtLink :to="`/${effectiveSlug}/skills`" active-class="active" class="nav-link" @click="collapseNavbar">
              Skills
            </NuxtLink>
          </li>
          <li v-if="isEnabled('portfolio.experience.show') && effectiveSlug" class="nav-item">
            <NuxtLink :to="`/${effectiveSlug}/experience`" active-class="active" class="nav-link" @click="collapseNavbar">
              Experience
            </NuxtLink>
          </li>
          <li v-if="isEnabled('portfolio.testimonials.show') && effectiveSlug" class="nav-item">
            <NuxtLink :to="`/${effectiveSlug}/testimonials`" active-class="active" class="nav-link" @click="collapseNavbar">
              Testimonials
            </NuxtLink>
          </li>
          <li v-if="isEnabled('portfolio.qualifications.show') && effectiveSlug" class="nav-item">
            <NuxtLink :to="`/${effectiveSlug}/qualifications`" active-class="active" class="nav-link" @click="collapseNavbar">
              Qualifications
            </NuxtLink>
          </li>
          <li v-if="isEnabled('portfolio.contact.show') && effectiveSlug" class="nav-item">
            <NuxtLink :to="`/${effectiveSlug}/contact`" active-class="active" class="nav-link" @click="collapseNavbar">
              Contact
            </NuxtLink>
          </li>

          <li class="nav-item d-flex align-items-center ms-lg-2">
            <ThemeToggle />
          </li>

          <li class="nav-item d-none d-lg-block mx-1">
            <div class="vr"></div>
          </li>

          <ClientOnly>
            <template v-if="isAuthenticated && authUser">
              <li v-if="isAdmin" class="nav-item">
                <a active-class="active" class="nav-link" href="/admin" @click="collapseNavbar">
                  Admin
                </a>
              </li>

              <li class="nav-item">
                <a :href="`/${authUser?.slug}/manage/messages`" class="nav-link position-relative"
                   title="My Messages" @click="collapseNavbar">
                  <MessageCircle class="fs-5" />
                  <span v-if="unreadCount > 0"
                        class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
                    {{ unreadCount }}
                    <span class="visually-hidden">unread messages</span>
                  </span>
                </a>
              </li>

              <li class="nav-item dropdown">
                <a id="navbarUserDropdown" aria-expanded="false"
                   class="nav-link dropdown-toggle d-flex align-items-center"
                   data-bs-toggle="dropdown"
                   href="#" role="button">
                  <img v-if="authUser?.profileImageUrl"
                       :src="authUser.profileImageUrl" alt="Avatar"
                       class="navbar-avatar ms-2 me-2">
                  <UserCircle v-else class="navbar-avatar-placeholder ms-2 me-2" />
                  {{ authUser?.firstName || 'User' }}
                </a>
                <ul aria-labelledby="navbarUserDropdown"
                    class="dropdown-menu dropdown-menu-end glass-dropdown">
                  <li v-if="!isGuest">
                    <a :href="`/${authUser?.slug}/manage/dashboard`" class="dropdown-item">
                      <LayoutDashboard class="me-2" />My Dashboard
                    </a>
                  </li>
                  <li v-if="!isGuest">
                    <a :href="`/${authUser?.slug}/manage/profile`" class="dropdown-item">
                      <IdCard class="me-2" />My Profile
                    </a>
                  </li>
                  <li v-if="!isGuest">
                    <a :href="`/${authUser?.slug}/manage/guest-passes`" class="dropdown-item">
                      <Ticket class="me-2" />Guest Passes
                    </a>
                  </li>
                  <li v-if="authUser?.slug">
                    <a :href="`/${authUser?.slug}`" class="dropdown-item">
                      <Home class="me-2" />My Public Page
                    </a>
                  </li>
                  <li>
                    <hr class="dropdown-divider">
                  </li>
                  <li>
                    <a :href="`/${authUser?.slug}/manage/settings/display`" class="dropdown-item">
                      <SlidersHorizontal class="me-2" />Display Settings
                    </a>
                  </li>
                  <li v-if="!isGuest">
                    <a :href="`/${authUser?.slug}/manage/settings/pdf`" class="dropdown-item">
                      <FileText class="me-2" />PDF Settings
                    </a>
                  </li>
                  <li>
                    <hr class="dropdown-divider">
                  </li>
                  <li>
                    <a class="dropdown-item" href="#" @click.prevent="requestLogoutConfirmation">
                      <LogOut class="me-2" />Logout
                    </a>
                  </li>
                </ul>
              </li>
            </template>
            <template v-else>
              <li class="nav-item">
                <!-- UAT-6 FIX: plain anchors, not NuxtLink. These screens belong
                     to the Vue SPA; a client-side NuxtLink would be swallowed by
                     this app's [slug] catch-all (slug "login" -> portfolio 404 ->
                     "Portfolio Not Available"). A full page navigation lets nginx
                     route /login and /register to the Vue app. -->
                <a href="/login" class="nav-link" @click="collapseNavbar">
                  Login
                </a>
              </li>
              <li class="nav-item">
                <a href="/register" class="nav-link" @click="collapseNavbar">
                  Register
                </a>
              </li>
            </template>
          </ClientOnly>
        </ul>
      </div>
    </div>

    <ConfirmModal
      ref="logoutConfirmModalRef"
      :title="logoutConfirmTitle"
      :message="logoutConfirmMessage"
      cancel-text="Cancel"
      confirm-text="Logout"
      @confirm="executeLogout"
    />
  </nav>
</template>

<style scoped>
.navbar-brand {
  font-weight: bold;
}

.nav-link.active {
  font-weight: 500;
}

.navbar-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.navbar-avatar-placeholder {
  font-size: 1.75rem; /* ~28px */
  line-height: 1;
  vertical-align: middle;
}

.dropdown-item i {
  width: 1.25em; /* Aligns text for items with and without icons */
}

.nav-link .badge {
  font-size: 0.6em;
  padding: 0.35em 0.55em;
}

@media (max-width: 991.98px) {
  .navbar-nav .btn-primary {
    margin-top: 0.5rem;
    display: block;
    width: fit-content;
  }

  .navbar-nav .nav-item:has(.theme-switch) {
    margin-top: 0.5rem;
    margin-bottom: 0.5rem;
  }
}

.vr {
  height: 25px;
}

/* Ensure dropdown appears above other content */
nav.navbar {
  z-index: 1030; /* Higher than Bootstrap's default */
}
</style>
