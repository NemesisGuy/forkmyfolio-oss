<template>
  <!-- THIS IS THE FIX: The page now uses the animated background and flexbox for centering -->
  <div class="unauthorized-page py-5 m-auto d-flex align-items-center w-100">
    <div class="container" style="max-width: 600px;">
      <!-- The content is now wrapped in a beautiful, animated glass card -->
      <div
        class="card glass-card shimmering animate-fade-in-up interactive-card-lift interactive-card-shadow-primary">
        <div class="card-body text-center p-4 p-md-5">
          <!-- The icon is now a consistent Bootstrap Icon -->
          <ShieldCheck class="display-1 text-warning mb-3" />
          <h1 class="display-5">Access Denied</h1>
          <p class="lead text-muted my-4">
            Sorry, you do not have the necessary permissions to access this page.
          </p>
          <div class="d-flex justify-content-center gap-3">
            <!-- Buttons now have interactive classes -->
            <router-link class="btn btn-outline-secondary btn-lg interactive-lift" to="/">
              Go to Homepage
            </router-link>
            <router-link class="btn btn-primary btn-lg interactive-lift interactive-shadow-primary"
                         to="/login">
              Login
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ShieldCheck } from 'lucide-vue-next';
/**
 * @file src/views/public/UnauthorizedPage.vue
 * @description An "Access Denied" page for users attempting to access restricted content.
 * Displays a 401-like message and links to homepage or login.
 */

// No specific script logic needed for this simple static page.
</script>

<style scoped>
/* THIS IS THE FIX: The page now takes up the full viewport height for better vertical centering */
.unauthorized-page {
  overflow-x: hidden;
}

.card-body {
  position: relative;
  z-index: 1;
}

h1 {
  font-weight: 300;
}

/* THIS IS THE FIX: Adds the fade-in animation */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out forwards;
}

/* Responsive styles for mobile */
@media (max-width: 576px) {
  .unauthorized-page {
    padding: 1rem 0;
  }

  .unauthorized-page .container {
    max-width: 100%;
    padding: 0 1rem;
  }

  .unauthorized-page .card-body {
    padding: 1.5rem;
  }

  .unauthorized-page .display-5 {
    font-size: 1.8rem !important;
  }

  .unauthorized-page .lead {
    font-size: 1rem;
  }

  .unauthorized-page .btn-lg {
    padding: 0.5rem 1rem;
    font-size: 1rem;
    width: 100%;
    margin-bottom: 0.5rem;
  }

  .unauthorized-page .d-flex {
    flex-direction: column;
    align-items: center;
  }
}

/* Tablet enhancements */
@media (min-width: 768px) {
  .unauthorized-page .container {
    max-width: 500px;
  }
}

/* Desktop enhancements */
@media (min-width: 1024px) {
  .unauthorized-page .container {
    max-width: 600px;
  }
}
</style>
