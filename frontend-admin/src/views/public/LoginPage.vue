<template>
  <div class="login-page flex-grow-1 d-flex align-items-center">
    <div class="container" style="max-width: 400px;">
      <!-- The interactive classes are correct -->
      <!-- FIX: Removed the 'shimmering' class. It hardcodes a dark background which makes the theme-adaptive text unreadable in light mode. -->
      <div
        class="card glass-card animate-fade-in-up interactive-card-lift interactive-card-shadow-primary">
        <div class="card-body p-4 p-md-5">
          <h1 class="card-title text-center mb-4 fs-3">Login</h1>

          <div v-if="formMessage.text"
               :class="['alert', formMessage.type === 'success' ? 'alert-success' : 'alert-danger']"
               role="alert">
            {{ formMessage.text }}
            <ul v-if="formMessage.errors && formMessage.errors.length > 0" class="mb-0 mt-2">
              <li v-for="(err, index) in formMessage.errors" :key="index">
                {{ err.field ? `${err.field}: ` : '' }}{{ err.message }}
              </li>
            </ul>
          </div>

          <form novalidate @submit.prevent="handleLogin">
            <div class="mb-3">
              <label class="form-label" for="email">Email address</label>
              <input id="email" v-model="credentials.email"
                     :class="{'is-invalid': fieldErrors.email}"
                     class="form-control" required type="email">
              <div v-if="fieldErrors.email" class="invalid-feedback">{{ fieldErrors.email }}</div>
            </div>
            <div class="mb-3">
              <label class="form-label" for="password">Password</label>
              <input id="password" v-model="credentials.password"
                     :class="{'is-invalid': fieldErrors.password}" class="form-control"
                     required type="password">
              <div v-if="fieldErrors.password" class="invalid-feedback">{{
                  fieldErrors.password
                }}
              </div>
            </div>
            <button :disabled="isLoading"
                    class="btn btn-primary w-100 interactive-lift interactive-shadow-primary"
                    type="submit">
              <span v-if="isLoading" aria-hidden="true" class="spinner-border spinner-border-sm"
                    role="status"></span>
              {{ isLoading ? 'Logging in...' : 'Login' }}
            </button>
          </form>
          <p class="mt-4 text-center">
            Don't have an account?
            <router-link to="/register">Sign Up</router-link>
          </p>
        </div>
      </div>

      <SuccessModal
        v-if="loginSuccessMessage"
        :message="loginSuccessMessage"
        :visible="showLoginSuccessModal"
        title="Login Successful"
        @close="closeLoginSuccessModal"
      />

      <ErrorModal
        :message="loginErrorMessage"
        :title="loginErrorTitle"
        :visible="showLoginErrorModal"
        @close="closeLoginErrorModal"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * @file src/views/public/LoginPage.vue
 * @description The user login page. It handles user authentication, form validation,
 * and redirects the user upon successful login.
 */
import {onMounted, reactive, ref} from 'vue';
import {useRoute, useRouter, type RouteLocationRaw} from 'vue-router';
import { authService } from '@/services/authService';
import {settingsService} from '@/services/settingsService.js';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {ApiError} from '@/services/api/index.js';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import type { User } from '@/types';

const router = useRouter();
const route = useRoute();

const credentials = reactive({
  email: '',
  password: ''
});

const fieldErrors = reactive<Record<string, string | null>>({email: null, password: null});

const formMessage = reactive<{
  text: string | null,
  type: string | null,
  errors: { field?: string; message: string }[]
}>({ text: null, type: null, errors: [] });

const isLoading = ref(false);

const loginSuccessMessage = ref<string | null>(null);
const showLoginSuccessModal = ref(false);

const showLoginErrorModal = ref(false);
const loginErrorTitle = ref('Error');
const loginErrorMessage = ref<string | string[]>('');

let redirectPathOnSuccess: RouteLocationRaw | null = null;

onMounted(() => {
  if (authService.isAuthenticated.value) {
    const userSlug = (authService.user.value as User | null)?.slug;
    const redirect = route.query.redirect;
    const target = (Array.isArray(redirect) ? redirect[0] : redirect) || {name: 'dashboard', params: {slug: userSlug}};
    router.replace(target as RouteLocationRaw);
  }
});

const closeLoginSuccessModal = () => {
  showLoginSuccessModal.value = false;
  loginSuccessMessage.value = null;
  if (redirectPathOnSuccess) {
    router.replace(redirectPathOnSuccess);
  }
};

const closeLoginErrorModal = () => {
  showLoginErrorModal.value = false;
  loginErrorTitle.value = 'Error';
  loginErrorMessage.value = '';
};

const validateForm = () => {
  fieldErrors.email = null;
  fieldErrors.password = null;
  formMessage.text = null;
  formMessage.type = null;
  formMessage.errors = [];

  let isValid = true;
  if (!credentials.email.trim()) {
    fieldErrors.email = "Email is required.";
    isValid = false;
  } else if (!/\S+@\S+\.\S+/.test(credentials.email)) {
    fieldErrors.email = "Email address is invalid.";
    isValid = false;
  }
  if (!credentials.password) {
    fieldErrors.password = "Password is required.";
    isValid = false;
  }

  if (!isValid) {
    formMessage.text = "Please correct the errors in the form.";
    formMessage.type = "error";
  }
  return isValid;
};

const handleLogin = async () => {
  closeLoginErrorModal();

  if (!validateForm()) {
    return;
  }

  isLoading.value = true;
  formMessage.text = null;
  formMessage.type = null;
  formMessage.errors = [];

  try {
    await authService.login(credentials);

    // --- THIS IS THE FIX ---
    // After a successful login, we must explicitly re-initialize the settings
    // and update the public portfolio store's slug to reflect the new
    // user's context. This ensures the navbar has the correct data before redirecting.
    await settingsService.initialize();
    usePublicPortfolioStore().currentSlug.value = (authService.user.value as User | null)?.slug || null;
    // --- END OF FIX ---

    const userSlug = (authService.user.value as User | null)?.slug;
    const redirect = route.query.redirect;
    redirectPathOnSuccess = (Array.isArray(redirect) ? redirect[0] : redirect) || {name: 'dashboard', params: {slug: userSlug}};
    loginSuccessMessage.value = "Login successful! Redirecting...";
    showLoginSuccessModal.value = true;

    // Automatically close the modal and redirect after a short delay for better UX.
    setTimeout(closeLoginSuccessModal, 1500);
  } catch (error) {
    console.error("Login error:", error);
    if (error instanceof ApiError) {
      loginErrorTitle.value = 'Login Failed';
      if (error.errors && error.errors.length > 0) {
        loginErrorMessage.value = error.errors.map(err => err.message);
      } else {
        loginErrorMessage.value = error.message || 'An API error occurred during login.';
      }
      error.errors?.forEach(err => {
        if (err.field && fieldErrors.hasOwnProperty(err.field)) {
          fieldErrors[err.field] = err.message;
        }
      });
    } else {
      loginErrorTitle.value = 'Unexpected Error';
      loginErrorMessage.value = 'An unexpected error occurred. Please try again.';
    }
    showLoginErrorModal.value = true;
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  padding: 2rem 0;
  /* Background handled by main-content in App.vue */
  overflow-x: hidden;
}

/*
  KEY FIX: This ensures the card's content is clickable by lifting it
  above the decorative ::before pseudo-element.
*/
.card-body {
  position: relative;
  z-index: 1;
}

.form-control,
.btn-primary {
  will-change: transform, box-shadow;
}

/* Form inputs on glass (remains page-specific) */
.form-control {
  background-color: rgba(var(--bs-body-bg-rgb), 0.5);
  border: 1px solid rgba(var(--bs-body-color-rgb), 0.1);
  color: var(--bs-body-color);
  transition: all 0.3s ease;
}

.form-control:focus {
  background-color: rgba(var(--bs-body-bg-rgb), 0.7);
  color: var(--bs-body-color);
  border-color: var(--bs-primary);
  box-shadow: 0 0 0 0.25rem rgba(var(--bs-primary-rgb), 0.25);
}

.form-control.is-invalid {
  border-color: var(--bs-danger);
  background-color: rgba(var(--bs-danger-rgb), 0.1);
}

.invalid-feedback {
  color: var(--bs-danger);
  font-weight: 500;
}

/* Ensure button visibility in light mode */
.btn-primary {
  background-color: var(--bs-primary);
  color: var(--bs-white);
  border-color: var(--bs-primary);
}

/* Responsive styles for small screens like iPhone SE */
@media (max-width: 576px) {
  .login-page {
    padding: 1rem 0;
    align-items: flex-start;
  }

  .login-page .container {
    max-width: 100%;
    padding: 0 1rem;
    margin-top: 10vh;
  }

  .login-page .card-body {
    padding: 1.5rem;
  }

  .login-page .fs-3 {
    font-size: 1.5rem !important;
  }
}
</style>
