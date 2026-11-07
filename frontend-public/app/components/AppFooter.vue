<script setup lang="ts">
import { Github } from 'lucide-vue-next';
import { formatDate } from '~/utils/dateUtils';

/**
 * @file app/components/AppFooter.vue
 * @description The main footer component for the application.
 * Ported from ForkMyFolio-frontend-vue/src/components/common/AppFooter.vue.
 */
</script>

<template>
  <!--
    The footer remains theme-aware using Bootstrap's utility classes.
    Styles are handled in the <style> block.
  -->
  <footer class="footer mt-auto py-3">
    <div class="container-fluid text-center">
      <div
        class="glass-subtitle d-flex flex-wrap justify-content-center align-items-center gap-1 gap-sm-3">
        <span>
          © {{ formatDate(new Date(), { month: 'long', year: 'numeric' }) }} ForkMyFolio
        </span>

        <span class="d-none d-sm-inline">|</span>

        <span>
          Built by
          <a
            class="glass-subtitle fw-semibold"
            href="https://github.com/NemesisGuy"
            rel="noopener noreferrer"
            target="_blank"
          >
            <Github class="me-1" />NemesisGuy
          </a>
        </span>

        <span class="d-none d-sm-inline">|</span>

        <span>
          Powered by
          <a
            class="glass-subtitle fw-semibold"
            href="https://nemesisnet.co.za"
            rel="noopener noreferrer"
            target="_blank"
          >
            Nemesisnet.co.za
          </a>
        </span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
/* Glassmorphic styles applied directly to the footer class */
.footer {
  background: var(--glass-bg);
  backdrop-filter: blur(15px) saturate(180%);
  -webkit-backdrop-filter: blur(15px) saturate(180%);
  border-top: 1px solid var(--glass-border);
  font-size: 0.9rem;
  /* Add a subtle transition for the background color change */
  transition: background-color 0.3s ease-in-out, border-color 0.3s ease-in-out;
}

.footer a {
  text-decoration: none;
}

.footer a:hover {
  text-decoration: underline !important;
  color: var(--bs-primary) !important;
}

/* On extra small screens, stack the items for better readability */
@media (max-width: 575.98px) {
  .footer .d-flex {
    flex-direction: column;
    gap: 0.25rem !important;
  }
}
</style>
