<template>
  <div class="guest-access-page min-vh-100 d-flex align-items-center justify-content-center py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-6 col-lg-5">
          <div class="card glass-card text-center p-4 animate-fade-in">
            <div class="card-body">
              <div v-if="loading" class="py-4">
                <div class="spinner-border text-primary mb-3" role="status" style="width: 3rem; height: 3rem;">
                  <span class="visually-hidden">Loading...</span>
                </div>
                <h2 class="h4 glass-text">Verifying Guest Pass...</h2>
                <p class="text-muted">Please wait while we validate your secure access token.</p>
              </div>

              <div v-else-if="error" class="py-3">
                <AlertTriangle class="text-danger display-1 mb-3" />
                <h2 class="h4 text-danger mb-3" data-testid="error-title">{{ errorTitle }}</h2>
                <div class="alert alert-danger shadow-sm mb-4">
                  {{ error }}
                </div>
                <p class="text-muted mb-4">
                  This guest pass might have been revoked, expired, or typed incorrectly.
                  Please contact the portfolio owner for a new access link.
                </p>
                <router-link to="/" class="btn btn-outline-primary w-100 py-2">
                  <Home class="me-2" />Return to Home
                </router-link>
              </div>

              <div v-else class="py-4">
                <CheckCircle class="text-success display-1 mb-3" />
                <h2 class="h4 text-success mb-2">Access Granted!</h2>
                <p class="text-muted mb-0">Redirecting you to the portfolio...</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authService } from '@/services/authService';
import type { User } from '@/types';
import { AlertTriangle, Home, CheckCircle } from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref<string | null>(null);
const errorTitle = ref('Access Denied');

const verifyToken = async () => {
  const token = route.query.token as string | undefined;
  const errorQuery = route.query.error as string | undefined;

  if (!token) {
    if (errorQuery === 'invalid') {
      errorTitle.value = 'Invalid Token';
      error.value = 'Your guest session has ended or the pass was revoked.';
      loading.value = false;
      return;
    }
    error.value = 'No access token was provided in the URL.';
    errorTitle.value = 'Missing Token';
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    await authService.loginAsGuest(token);

    // Success: Redirect to the portfolio home
    const currentUser = authService.user.value as User | null;
    const slug = currentUser?.slug;
    if (slug) {
      router.replace({
        name: 'portfolio-home',
        params: { slug },
        query: { guest: 'true' }
      });
    } else {
      router.replace('/');
    }
  } catch (e: unknown) {
    console.error('[GuestAccess] Verification failed:', e);
    const message = e instanceof Error ? e.message : String(e);
    if (message.includes('expired')) {
      errorTitle.value = 'Pass Expired';
      error.value = 'This guest pass has expired. Please request a new one.';
    } else if (message.includes('Invalid')) {
      errorTitle.value = 'Invalid Token';
      error.value = 'The provided token is not recognized by our system.';
    } else {
      error.value = message || 'An unexpected error occurred during verification.';
    }
  } finally {
    loading.value = false;
  }
};

// Handle initial mount
onMounted(verifyToken);

// Handle query changes (for redirects back to this page)
watch(() => route.query, verifyToken);
</script>

<style scoped>
.guest-access-page {
  background: linear-gradient(135deg, rgba(var(--primary-rgb), 0.05) 0%, rgba(var(--secondary-rgb), 0.05) 100%);
}

.glass-card {
  border: 1px solid rgba(var(--bs-body-color-rgb), 0.2);
  backdrop-filter: blur(10px);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  border-radius: 1.5rem;
}

.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
