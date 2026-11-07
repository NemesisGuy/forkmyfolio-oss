<template>
  <div class="admin-dashboard-page py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <div class="mb-5 text-center">
        <h1 class="display-4 fw-light glass-text animate-fade-in-up">Admin Dashboard</h1>
        <p class="lead glass-subtitle animate-fade-in-up" style="animation-delay: 0.1s;">
          Manage users, content, and application-wide settings.
        </p>
      </div>

      <div class="row g-4 justify-content-center">

        <!-- User Management -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.2s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><Users class="me-2" />Manage Users</h5>
              <p class="card-text small text-muted">View, create, edit, and manage all user
                accounts.</p>
              <router-link :to="{ name: 'admin-users' }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Go to User Management
              </router-link>
            </div>
          </div>
        </div>

        <!-- Application Settings -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.3s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><Settings2 class="me-2" />Application
                Settings</h5>
              <p class="card-text small text-muted">Control global settings like public section
                visibility.</p>
              <router-link :to="{ name: 'admin-settings' }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Configure Settings
              </router-link>
            </div>
          </div>
        </div>

        <!-- Statistics -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.4s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><BarChart3 class="me-2" />View Statistics
              </h5>
              <p class="card-text small text-muted">View detailed statistics on site engagement.</p>
              <router-link :to="{ name: 'admin-stats' }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Go to Statistics
              </router-link>
            </div>
          </div>
        </div>

        <!-- Messages -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.5s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><Mail class="me-2" />Manage Messages
              </h5>
              <p class="card-text small text-muted">View messages submitted through all contact
                forms.</p>
              <router-link :to="{ name: 'admin-messages' }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Go to Messages
              </router-link>
            </div>
          </div>
        </div>

        <!-- PDF Settings -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.6s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><FileText class="me-2" />PDF Settings
              </h5>
              <p class="card-text small text-muted">Select the global default PDF template for
                downloads.</p>
              <router-link :to="{ name: 'admin-pdf-settings' }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Configure PDF
              </router-link>
            </div>
          </div>
        </div>

        <!-- Backup & Restore -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.7s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column">
              <h5 class="card-title"><ShieldCheck class="me-2" />System Tools</h5>
              <p class="card-text small text-muted">Manage system-wide backups, restores, and other
                destructive data operations.</p>
              <router-link :to="{ name: 'admin-system-backup' }"
                           class="btn btn-outline-primary mt-auto align-self-start">
                Go to System Tools
              </router-link>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Users, Settings2, BarChart3, Mail, FileText, ShieldCheck } from 'lucide-vue-next';
// No script changes needed for this refactor
</script>

<style scoped>
.admin-dashboard-page .display-4 {
  font-weight: 300;
}

.card-title i {
  vertical-align: -0.125em;
  opacity: 0.8;
}

/* Responsive styles for mobile-first design */
@media (max-width: 576px) {
  .admin-dashboard-page {
    padding: 1rem 0;
  }

  .admin-dashboard-page .container {
    padding: 0 1rem;
  }

  .admin-dashboard-page .display-4 {
    font-size: 2rem;
  }

  .admin-dashboard-page .lead {
    font-size: 1rem;
  }

  .admin-dashboard-page .card-body {
    padding: 1rem;
  }

  .admin-dashboard-page .card-title {
    font-size: 1.1rem;
  }

  .admin-dashboard-page .card-text {
    font-size: 0.9rem;
  }

  .admin-dashboard-page .btn {
    padding: 0.5rem 1rem;
    font-size: 0.9rem;
    min-height: 44px; /* Touch-friendly */
  }
}

/* Tablet enhancements */
@media (min-width: 768px) {
  .admin-dashboard-page .row {
    --bs-gutter-x: 2rem; /* Increase gap on tablet */
  }
}

/* Desktop enhancements */
@media (min-width: 1024px) {
  .admin-dashboard-page .container {
    max-width: 1200px;
  }
}
</style>
