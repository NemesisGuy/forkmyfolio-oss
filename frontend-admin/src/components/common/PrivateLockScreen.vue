<template>
  <div class="container py-5 mt-5">
    <div class="glass-card mx-auto animate-fade-in-up" style="max-width: 600px;">
      <div class="card-body text-center p-5">
        <div class="lock-icon-container mb-4">
          <Lock class="text-danger shadow-sm" />
        </div>
        <h2 class="display-6 glass-title mb-3">Portfolio Restricted</h2>
        <p class="lead glass-description mb-4">
          This portfolio does not exist or is not currently available to the public.
        </p>

        <hr class="my-4 opacity-25">

        <div v-if="!isAuthenticated" class="mt-4">
          <p class="small text-muted mb-3">Are you the owner?</p>
          <router-link to="/login" class="btn glass-btn-primary rounded-pill px-4">
            <LogIn class="me-2" />Sign In to View
          </router-link>
        </div>
        <div v-else class="mt-4">
          <p class="small text-muted mb-3">You are logged in, but this portfolio is not available to your account.</p>
          <router-link to="/" class="btn glass-btn-outline rounded-pill px-4">
            <Home class="me-2" />Back to Home
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Lock, LogIn, Home } from 'lucide-vue-next';
import { authService } from '@/services/authService';
const isAuthenticated = authService.isAuthenticated;
</script>

<style scoped>
.lock-icon-container {
  font-size: 5rem;
  line-height: 1;
}
</style>
