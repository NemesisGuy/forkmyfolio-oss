<template>
  <div class="user-account-page py-5 flex-grow-1">
    <div class="container">
      <!-- Header -->
      <div v-if="user" class="mb-5 text-center">
        <h1 class="display-4 fw-light glass-text animate-fade-in-up">Profile & Account</h1>
        <p class="lead glass-subtitle animate-fade-in-up" style="animation-delay: 0.1s;">
          Manage your account details and public-facing portfolio profile.
        </p>
      </div>

      <!-- Navigation Cards -->
      <div class="row g-4 justify-content-center">
        <!-- Edit Account Card -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.2s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><Settings class="me-2" />Edit Account Details</h5>
              <p class="card-text small text-muted">Manage your private account information like
                your name, email, and profile picture.</p>
              <router-link :to="{ name: 'edit-account', params: { slug: user.slug } }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Manage Account
              </router-link>
            </div>
          </div>
        </div>

        <!-- Edit Public Profile Card -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.3s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><LayoutPanelLeft class="me-2" />Edit
                Public Profile</h5>
              <p class="card-text small text-muted">Edit the public information on your portfolio,
                like your headline and summary.</p>
              <router-link :to="{ name: 'edit-public-profile', params: { slug: user.slug } }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Manage Public Profile
              </router-link>
            </div>
          </div>
        </div>

        <!-- View Public Page Card -->
        <div v-if="user && user.slug" class="col-md-6 col-lg-4 animate-fade-in-up"
             style="animation-delay: 0.4s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><ExternalLink class="me-2" />View My Public
                Page</h5>
              <p class="card-text small text-muted">See how your portfolio looks to visitors and
                share the link.</p>
              <router-link :to="{ name: 'portfolio-home', params: { slug: user.slug } }"
                           class="btn btn-outline-primary mt-auto align-self-start" target="_blank">
                View My Page
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {authService} from '@/services/authService.js';
import { Settings, LayoutPanelLeft, ExternalLink } from 'lucide-vue-next';
import type { User } from '@/types';

const user = computed(() => authService.user.value as User | null);
</script>

<style scoped>
.user-account-page .display-4 {
  font-weight: 300;
}

.card-title i {
  vertical-align: -0.125em;
  opacity: 0.8;
}

/* Responsive styles for mobile-first design */
@media (max-width: 576px) {
  .user-account-page {
    padding: 1rem 0;
  }

  .user-account-page .container {
    padding: 0 1rem;
  }

  .user-account-page .display-4 {
    font-size: 2rem;
  }

  .user-account-page .lead {
    font-size: 1rem;
  }

  .user-account-page .card-body {
    padding: 1rem;
  }

  .user-account-page .card-title {
    font-size: 1.1rem;
  }

  .user-account-page .card-text {
    font-size: 0.9rem;
  }

  .user-account-page .btn {
    padding: 0.5rem 1rem;
    font-size: 0.9rem;
    min-height: 44px; /* Touch-friendly */
  }
}

/* Tablet enhancements */
@media (min-width: 768px) {
  .user-account-page .row {
    --bs-gutter-x: 2rem; /* Increase gap on tablet */
  }
}

/* Desktop enhancements */
@media (min-width: 1024px) {
  .user-account-page .container {
    max-width: 1200px;
  }
}
</style>
