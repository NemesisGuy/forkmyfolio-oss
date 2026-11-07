<template>
  <div class="landing-page flex-grow-1">
    <div class="container text-center">
      <div class="animate-fade-in-up">
        <img alt="ForkMyFolio Logo" class="mb-4 rounded-circle shadow-lg" height="120"
             src="@/assets/forkmyfolio_logo_icon.png" width="120">
        <h1 class="display-3 fw-bold text-gradient">Welcome to ForkMyFolio</h1>
        <p class="lead glass-subtitle my-4 mx-auto" style="max-width: 700px;">
          The simplest way to create, manage, and share a professional developer portfolio. No
          coding required.
        </p>
        <div class="d-flex justify-content-center gap-3 flex-wrap">
          <router-link :to="{ name: 'register' }" class="btn btn-primary btn-lg interactive-lift">
            Get Started for Free
          </router-link>
          <router-link :to="{ name: 'login' }"
                       class="btn btn-outline-light btn-lg interactive-lift">Login
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// No script logic is needed for this simple static page.
</script>

<style scoped>
.landing-page {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-x: hidden;
}

/* On larger screens, maintain centered layout */
@media (min-width: 577px) {
  .landing-page {
    /* Removed min-height to prevent scrolling */
    padding: 0; /* Remove padding for tablets to prevent overflow */
  }
}

.text-gradient {
  background: linear-gradient(45deg, var(--bs-primary), var(--bs-info));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  color: var(--bs-primary); /* Fallback color */
  padding-bottom: 0.15em;
}

.btn-lg {
  padding: 0.75rem 1.5rem;
  font-size: 1.1rem;
  font-weight: 500;
}

/* Make outline-light button visible in light mode */
.btn-outline-light {
  border-color: var(--bs-dark);
  color: var(--bs-dark);
}

.btn-outline-light:hover {
  background-color: var(--bs-dark);
  color: var(--bs-light);
  border-color: var(--bs-dark);
}

/* Ensure visibility in dark mode */
[data-bs-theme="dark"] .btn-outline-light {
  border-color: var(--bs-light);
  color: var(--bs-light);
}

[data-bs-theme="dark"] .btn-outline-light:hover {
  background-color: var(--bs-light);
  color: var(--bs-dark);
  border-color: var(--bs-light);
}

.interactive-lift {
  will-change: transform;
}

/* Responsive styles for small screens like iPhone SE */
@media (max-width: 576px) {
  .landing-page .container {
    padding: 1rem 1rem 2rem 1rem; /* Padding for mobile */
  }

  .landing-page .display-3 {
    font-size: 2.5rem; /* Reduce from default display-3 */
    margin-bottom: 0.5rem; /* Reduce margin */
  }

  .landing-page .lead {
    font-size: 1rem; /* Reduce lead size */
    max-width: 100%;
    margin: 0.5rem auto 1rem auto; /* Reduce margins */
  }

  .landing-page .btn-lg {
    padding: 0.5rem 1rem;
    font-size: 1rem;
    width: 100%; /* Full width buttons on small screens */
    margin-bottom: 0.5rem;
  }

  .landing-page .d-flex {
    flex-direction: column;
    align-items: center;
  }

  .landing-page img {
    height: 80px;
    width: 80px;
    margin-bottom: 0.5rem; /* Reduce margin */
  }

  .landing-page {
    height: auto; /* Allow natural height on mobile */
    flex: none; /* Disable flex on mobile to allow natural flow */
  }
}

/* Tablet styles to prevent scrolling */
@media (min-width: 768px) and (max-width: 1023px) {
  .landing-page.flex-grow-1 {
    height: 100% !important; /* Fill the main-content height */
  }

  .landing-page .container {
    padding: 1rem; /* Minimal padding for tablets */
  }

  .landing-page .display-3 {
    font-size: 2.5rem; /* Smaller font */
    margin-bottom: 0 !important; /* Override Bootstrap */
  }

  .landing-page .lead {
    font-size: 1rem; /* Smaller lead */
    margin: 0 auto 0.5rem auto !important; /* Override Bootstrap my-4 */
  }

  .landing-page .btn-lg {
    padding: 0.6rem 1.2rem;
    font-size: 1rem;
  }

  .landing-page img {
    height: 100px;
    width: 100px;
    margin-bottom: 0 !important; /* Override Bootstrap mb-4 */
  }

  .landing-page .d-flex {
    gap: 0.5rem !important; /* Reduce gap between buttons */
  }
}

/* Desktop styles */
@media (min-width: 1024px) {
  .landing-page.flex-grow-1 {
    height: 100% !important; /* Fill the main-content height */
  }

  .landing-page .container {
    padding: 2rem; /* Moderate padding for desktop */
  }

  .landing-page .display-3 {
    font-size: 3.5rem; /* Slightly larger but still reasonable */
    margin-bottom: 0 !important; /* Override Bootstrap */
  }

  .landing-page .lead {
    margin: 0 auto 0.5rem auto !important; /* Override Bootstrap my-4 */
  }
}
</style>
