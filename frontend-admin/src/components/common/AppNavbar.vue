<template>
  <nav :class="[
    'navbar',
    'navbar-expand-lg',
    'glass-nav',
    currentTheme === 'dark' ? 'navbar-dark' : 'navbar-light'
  ]">
    <div class="container-fluid">
      <router-link class="navbar-brand d-flex align-items-center" to="/">
        <img alt="ForkMyFolio Logo" class="d-inline-block align-text-top me-2 rounded-circle"
             height="45"
             src="../../assets/forkmyfolio_logo_icon.png" width="45">
        ForkMyFolio
      </router-link>

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
            <router-link
              :class="{ 'active': isHomeActive }"
              :to="homeLinkTarget"
              class="nav-link"
              @click="collapseNavbar">
              Home
            </router-link>
          </li>
          <!-- Public portfolio links that appear based on the current context -->
          <li v-if="settingsService.isEnabled.value('portfolio.projects.show') && effectiveSlug"
              class="nav-item">
            <router-link :to="{ name: 'projects-public', params: { slug: effectiveSlug } }"
                         active-class="active"
                         class="nav-link"
                         @click="collapseNavbar">
              Projects
            </router-link>
          </li>
          <li v-if="settingsService.isEnabled.value('portfolio.skills.show') && effectiveSlug"
              class="nav-item">
            <router-link :to="{ name: 'skills-public', params: { slug: effectiveSlug } }"
                         active-class="active"
                         class="nav-link"
                         @click="collapseNavbar">
              Skills
            </router-link>
          </li>
          <li v-if="settingsService.isEnabled.value('portfolio.experience.show') && effectiveSlug"
              class="nav-item">
            <router-link :to="{ name: 'experience-public', params: { slug: effectiveSlug } }"
                         active-class="active"
                         class="nav-link"
                         @click="collapseNavbar">
              Experience
            </router-link>
          </li>
          <li v-if="settingsService.isEnabled.value('portfolio.testimonials.show') && effectiveSlug"
              class="nav-item">
            <router-link :to="{ name: 'testimonials-public', params: { slug: effectiveSlug } }"
                         active-class="active"
                         class="nav-link"
                         @click="collapseNavbar">
              Testimonials
            </router-link>
          </li>
          <li v-if="settingsService.isEnabled.value('portfolio.qualifications.show') && effectiveSlug"
              class="nav-item">
            <router-link :to="{ name: 'qualifications-public', params: { slug: effectiveSlug } }"
                         active-class="active"
                         class="nav-link"
                         @click="collapseNavbar">
              Qualifications
            </router-link>
          </li>
          <li v-if="settingsService.isEnabled.value('portfolio.contact.show') && effectiveSlug"
              class="nav-item">
            <router-link :to="{ name: 'contact', params: { slug: effectiveSlug } }"
                         active-class="active"
                         class="nav-link"
                         @click="collapseNavbar">
              Contact
            </router-link>
          </li>

          <li class="nav-item d-flex align-items-center ms-lg-2">
            <ThemeToggle/>
          </li>

          <li class="nav-item d-none d-lg-block mx-1">
            <div class="vr"></div>
          </li>

          <template v-if="authService.isAuthenticated.value && currentUser">
            <li v-if="isAdmin" class="nav-item">
              <router-link active-class="active" class="nav-link" to="/admin"
                           @click="handleUserMenuClick(currentUser.slug)">
                Admin
              </router-link>
            </li>

            <!-- ANNOUNCEMENTS DROPDOWN -->
            <AnnouncementDropdown v-if="authService.isAuthenticated.value" />

            <li class="nav-item">
              <router-link
                :to="{ name: 'user-messages', params: { slug: currentUser.slug } }"
                class="nav-link position-relative" title="My Messages" @click="collapseNavbar">
                <MessageCircle class="fs-5" />
                <!-- Badge now uses the live count from our new service -->
                <span v-if="unreadCount > 0"
                      class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
                  {{ unreadCount }}
                  <span class="visually-hidden">unread messages</span>
                </span>
              </router-link>
            </li>

            <li class="nav-item dropdown">
              <a id="navbarUserDropdown" aria-expanded="false"
                 class="nav-link dropdown-toggle d-flex align-items-center"
                 data-bs-toggle="dropdown"
                 href="#" role="button">
                <img v-if="currentUser?.profileImageUrl"
                     :src="currentUser.profileImageUrl" alt="Avatar"
                     class="navbar-avatar ms-2 me-2">
                <UserCircle v-else class="navbar-avatar-placeholder ms-2 me-2" />
                {{ currentUser?.firstName || 'User' }}
              </a>
              <ul aria-labelledby="navbarUserDropdown"
                  class="dropdown-menu dropdown-menu-end glass-dropdown">
                <li v-if="!isGuest">
                  <router-link
                    :to="{ name: 'dashboard', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <LayoutDashboard class="me-2" />My Dashboard
                  </router-link>
                </li>
                <li v-if="!isGuest">
                  <router-link
                    :to="{ name: 'profile', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <IdCard class="me-2" />My Profile
                  </router-link>
                </li>
                <li v-if="!isGuest">
                  <router-link
                    :to="{ name: 'my-guest-passes', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <Ticket class="me-2" />Guest Passes
                  </router-link>
                </li>
                <li v-if="currentUser?.slug">
                  <router-link
                    :to="{ name: 'portfolio-home', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <Home class="me-2" />My Public Page
                  </router-link>
                </li>
                <li>
                  <hr class="dropdown-divider">
                </li>
                <li>
                  <router-link
                    :to="{ name: 'display-settings', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <SlidersHorizontal class="me-2" />Display Settings
                  </router-link>
                </li>
                <li v-if="!isGuest">
                  <router-link
                    :to="{ name: 'pdf-settings', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <FileText class="me-2" />PDF Settings
                  </router-link>
                </li>
                <li v-if="!isGuest">
                  <router-link
                    :to="{ name: 'export-settings', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <Download class="me-2" />Export Settings
                  </router-link>
                </li>
                <li v-if="!isGuest">
                  <router-link
                    :to="{ name: 'job-tracker', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <Briefcase class="me-2" />Job Applications
                  </router-link>
                </li>
                <li v-if="!isGuest">
                  <router-link
                    :to="{ name: 'analytics', params: { slug: currentUser.slug } }"
                    class="dropdown-item"
                    @click="handleUserMenuClick(currentUser.slug)">
                    <BarChart3 class="me-2" />Analytics
                  </router-link>
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
              <router-link active-class="active" class="nav-link" to="/login"
                           @click="collapseNavbar">
                Login
              </router-link>
            </li>
            <li class="nav-item">
              <router-link active-class="active" class="nav-link" to="/register"
                           @click="collapseNavbar">
                Register
              </router-link>
            </li>
          </template>
        </ul>
      </div>
    </div>

    <ConfirmModal
      ref="logoutConfirmModalRef"
      :title="logoutConfirmTitle"
      :message="logoutConfirmMessage"
      cancelText="Cancel"
      confirmText="Logout"
      @confirm="executeLogout"
    />
  </nav>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {MessageCircle, UserCircle, LayoutDashboard, IdCard, Ticket, Home, SlidersHorizontal, FileText, Download, Briefcase, BarChart3, LogOut} from 'lucide-vue-next';
import {authService} from '@/services/authService.js';
import {useTheme} from '@/services/themeService.js';
import {settingsService} from '@/services/settingsService.js';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {PUBLIC_PORTFOLIO_ROUTE_NAMES} from '@/router';
import ConfirmModal from './modals/ConfirmModal.vue';
import ThemeToggle from './ThemeToggle.vue';
import AnnouncementDropdown from './AnnouncementDropdown.vue';
import type { User } from '@/types';
// Import the new service to make it available
import {messageNotificationService} from '@/services/messageNotificationService.js';

// Use the reactive count from the service, replacing the placeholder
const {unreadCount} = messageNotificationService;

const {currentTheme} = useTheme();
const router = useRouter();
const route = useRoute();
const portfolioStore = usePublicPortfolioStore();
const {currentSlug, fetchPortfolio, clearPortfolio} = portfolioStore;

const currentUser = computed(() => authService.user.value as User | null);

const navbarToggler = ref<HTMLElement | null>(null);
const navbarNavCollapsible = ref<HTMLElement | null>(null);

const collapseNavbar = () => {
  if (navbarNavCollapsible.value && navbarNavCollapsible.value.classList.contains('show')) {
    navbarToggler.value?.click();
  }
};

const handleUserMenuClick = async (slug: string | null | undefined) => {
  if (slug) {
    await fetchPortfolio(slug);
  }
  collapseNavbar();
};

// FIX: Computed property to reliably get the active slug for navigation links.
// This prioritizes the route param (if viewing a portfolio) over the logged-in user's slug.
const effectiveSlug = computed(() => {
  const user = authService.user.value as User | null;
  return route.params.slug || (authService.isAuthenticated.value ? user?.slug : null);
});

// This watcher keeps the store in sync but doesn't block the UI links.
// We debounce the fetch to prevent double-firing on route/auth changes.
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

watch(
  [() => route.params.slug, () => authService.isAuthenticated.value],
  ([routeSlug, isAuthenticated]) => {
    if (debounceTimer) clearTimeout(debounceTimer);

    debounceTimer = setTimeout(() => {
      // The current context is primarily defined by the slug in the URL.
      const user = authService.user.value as User | null;
      const activeSlug = (routeSlug as string) || (isAuthenticated ? user?.slug : null);

      console.log(`[Navbar Context Watcher] Route Slug: ${routeSlug}, Auth State: ${isAuthenticated}, Active Slug: ${activeSlug}, Route: ${String(route.name)}`);

      // UAT-1 FIX: portfolio fetching is scoped to PUBLIC portfolio routes ONLY.
      // Previously this fired on dashboards/login pages whenever the user was
      // authenticated; a failed public fetch left `isPrivate=true` in the shared
      // store and the lock screen then covered auth pages like the login form.
      const isPortfolioRoute = PUBLIC_PORTFOLIO_ROUTE_NAMES.has(String(route.name ?? ''));

      if (isPortfolioRoute && activeSlug) {
        // Ensure we have the correct portfolio and settings for this context.
        fetchPortfolio(activeSlug);
        // We also trigger settings initialization here to ensure the UI matches the context.
        settingsService.initialize(activeSlug);
      } else if (currentSlug.value || portfolioStore.isPrivate.value) {
        // On auth/management/global pages (or slugless portfolio routes), drop
        // any stale public-portfolio context so the lock screen can't appear.
        console.log(`[Navbar] Non-portfolio context. Clearing public portfolio state.`);
        clearPortfolio();
        // settingsService.initialize() is auth-aware: no argument resolves to the
        // current user's settings when authenticated, global defaults otherwise.
        settingsService.initialize();
      }
    }, 300); // 300ms delay to absorb rapid changes
  },
  {
    immediate: true,
    deep: true,
  }
);

const homeLinkTarget = computed(() => {
  if (effectiveSlug.value) {
    return {name: 'portfolio-home', params: {slug: effectiveSlug.value}};
  }
  return {name: 'home'};
});

const isHomeActive = computed(() => {
  if (effectiveSlug.value) {
    return route.name === 'portfolio-home';
  }
  return route.name === 'home';
});

const isGuest = computed(() => {
  const user = authService.user.value as User | null;
  return (
    user &&
    user.roles &&
    user.roles.includes('GUEST')
  );
});

const isAdmin = computed(() => {
  const user = authService.user.value as User | null;
  return (
    user &&
    user.roles &&
    user.roles.includes('ADMIN')
  );
});

const logoutConfirmModalRef = ref<{ show: () => void } | null>(null);
const logoutConfirmTitle = 'Confirm Logout';
const logoutConfirmMessage = 'Are you sure you want to logout?';

const requestLogoutConfirmation = () => {
  collapseNavbar();
  logoutConfirmModalRef.value?.show();
};

const executeLogout = async () => {
  try {
    await authService.logout();
  } catch (error) {
    console.error('Error during logout:', error);
  } finally {
    // UAT-2 FIX: always land on the login page, even if the backend logout call
    // failed; authService.logout() already clears local state in its finally.
    await router.push('/login');
  }
};
</script>

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
