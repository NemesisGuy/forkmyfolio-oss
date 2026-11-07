<template>
  <!-- KEY CHANGE: The root element now uses the global animated background class -->
  <div class="signup-page flex-grow-1 d-flex align-items-center">
    <div class="container" style="max-width: 500px;">
      <!-- KEY CHANGE: The card now uses the global glass-card and interactive classes -->
      <div
        class="card glass-card animate-fade-in-up interactive-card-lift interactive-card-shadow-primary">
        <div class="card-body p-4 p-md-5">
          <h1 class="card-title text-center mb-4 fs-3">Create Account</h1>

          <div v-if="pageError" class="alert alert-danger" role="alert">
            <strong>Registration Unavailable</strong>
            <p class="mb-0 small">{{ pageError }}</p>
          </div>

          <div v-if="formMessage.text" :class="['alert', formMessage.type === 'success' ? 'alert-success' : 'alert-danger']" role="alert">
            {{ formMessage.text }} <ul v-if="formMessage.errors && formMessage.errors.length > 0" class="mb-0 mt-2">
            <li v-for="(err, index) in formMessage.errors" :key="index">{{ err.field ? `${err.field}: ` : '' }}{{ err.message }}</li>
          </ul>
          </div>

          <form novalidate @submit.prevent="handleSignup">
            <div class="mb-3">
              <label class="form-label" for="firstName">First Name</label>
              <input id="firstName" v-model="formData.firstName"
                     :class="{'is-invalid': fieldErrors.firstName}"
                     class="form-control" required type="text">
              <div v-if="fieldErrors.firstName" class="invalid-feedback">{{
                  fieldErrors.firstName
                }}
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label" for="lastName">Last Name</label>
              <input id="lastName" v-model="formData.lastName"
                     :class="{'is-invalid': fieldErrors.lastName}"
                     class="form-control" required type="text">
              <div v-if="fieldErrors.lastName" class="invalid-feedback">{{
                  fieldErrors.lastName
                }}
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label" for="email">Email address</label>
              <input id="email" v-model="formData.email" :class="{'is-invalid': fieldErrors.email}"
                     class="form-control" required type="email">
              <div v-if="fieldErrors.email" class="invalid-feedback">{{ fieldErrors.email }}</div>
            </div>
            <div class="mb-3">
              <label class="form-label" for="password">Password</label>
              <input id="password" v-model="formData.password"
                     :class="{'is-invalid': fieldErrors.password}" class="form-control"
                     required type="password">
              <div v-if="fieldErrors.password" class="invalid-feedback">{{
                  fieldErrors.password
                }}
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label" for="confirmPassword">Confirm Password</label>
              <input id="confirmPassword" v-model="formData.confirmPassword"
                     :class="{'is-invalid': fieldErrors.confirmPassword}" class="form-control"
                     required type="password">
              <div v-if="fieldErrors.confirmPassword" class="invalid-feedback">
                {{ fieldErrors.confirmPassword }}
              </div>
            </div>
            <!-- KEY CHANGE: The button now uses the global interactive classes -->
            <button :disabled="isLoading || !!pageError"
                    class="btn btn-primary w-100 interactive-lift interactive-shadow-primary"
                    type="submit">
              <span v-if="isLoading" aria-hidden="true" class="spinner-border spinner-border-sm"
                    role="status"></span>
              {{ isLoading ? 'Creating Account...' : 'Create Account' }}
            </button>
          </form>
          <p class="mt-4 text-center">
            Already have an account?
            <router-link to="/login">Login</router-link>
          </p>
        </div>
      </div>

      <SuccessModal
        v-if="signupSuccessMessage"
        :message="signupSuccessMessage"
        :visible="showSignupSuccessModal"
        title="Registration Successful"
        @close="closeSignupSuccessModal"
      />

      <ErrorModal
        :message="signupErrorMessage"
        :title="signupErrorTitle"
        :visible="showSignupErrorModal"
        @close="closeSignupErrorModal"
      />

      <!-- The new, blocking Terms & Conditions modal -->
      <TermsAgreementModal
        ref="termsModalRef"
        @confirm="executeRegistration"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {authService} from '@/services/authService';
import {settingsService} from '@/services/settingsService';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore';
import {ApiError} from '@/services/api/index';
import { getTermsOfService } from '@/services/api/policy.api';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import TermsAgreementModal from '@/components/auth/TermsAgreementModal.vue';
import type { User } from '@/types';

/**
 * @file src/views/SignupPage.vue
 * @description Signup page for new user registration.
 */

const router = useRouter();

const formData = reactive({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
});

const termsModalRef = ref<InstanceType<typeof TermsAgreementModal> | null>(null);
const termsVersion = ref<string | null>(null);
const pageError = ref<string | null>(null);

const fieldErrors = reactive<Record<string, string | null>>({
  firstName: null,
  lastName: null,
  email: null,
  password: null,
  confirmPassword: null,
});

const formMessage = reactive<{
  text: string | null,
  type: string | null,
  errors: { field?: string; message: string }[]
}>({text: null, type: null, errors: []});

const isLoading = ref(false);

// State for SuccessModal
const signupSuccessMessage = ref<string | null>(null);
const showSignupSuccessModal = ref(false);

// State for ErrorModal
const showSignupErrorModal = ref(false);
const signupErrorTitle = ref('Error');
const signupErrorMessage = ref<string | string[]>('');

onMounted(async () => {
  try {
    const policy = await getTermsOfService();
    termsVersion.value = policy.version;
  } catch (error) {
    console.error('Failed to load Terms of Service:', error);
    pageError.value = 'Could not load registration requirements. Please try again later.';
    // The submit button will be disabled via the pageError ref
  }
});

/**
 * Closes the success modal and redirects to the user's account page.
 */
const closeSignupSuccessModal = () => {
  showSignupSuccessModal.value = false;
  signupSuccessMessage.value = null;
  const userSlug = (authService.user.value as User | null)?.slug;
  router.push({name: 'dashboard', params: {slug: userSlug}});
};

/**
 * Closes the error modal.
 */
const closeSignupErrorModal = () => {
  showSignupErrorModal.value = false;
  signupErrorTitle.value = 'Error';
  signupErrorMessage.value = '';
};

/**
 * Validates the signup form data.
 * @returns {boolean} True if form is valid, false otherwise.
 */
const validateForm = () => {
  // Clear previous errors
  for (const key in fieldErrors) {
    fieldErrors[key] = null;
  }
  formMessage.text = null;
  formMessage.type = null;
  formMessage.errors = [];

  let isValid = true;
  if (!formData.firstName.trim()) {
    fieldErrors.firstName = "First name is required.";
    isValid = false;
  }
  if (!formData.lastName.trim()) {
    fieldErrors.lastName = "Last name is required.";
    isValid = false;
  }
  if (!formData.email.trim()) {
    fieldErrors.email = "Email is required.";
    isValid = false;
  } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
    fieldErrors.email = "Email address is invalid.";
    isValid = false;
  }
  if (!formData.password) {
    fieldErrors.password = "Password is required.";
    isValid = false;
  } else if (formData.password.length < 6) { // Example: min 6 chars
    fieldErrors.password = "Password must be at least 6 characters.";
    isValid = false;
  }
  if (formData.password !== formData.confirmPassword) {
    fieldErrors.confirmPassword = "Passwords do not match.";
    isValid = false;
  }

  if (!isValid) {
    formMessage.text = "Please correct the errors in the form.";
    formMessage.type = "error";
  }
  return isValid;
};

/**
 * Handles signup form submission.
 */
const handleSignup = async () => {
  closeSignupErrorModal();

  if (!validateForm()) {
    return;
  }

  // Show the blocking terms modal instead of immediately submitting
  termsModalRef.value?.show();
};

/**
 * Executes the actual registration after the user has confirmed the terms in the modal.
 */
const executeRegistration = async () => {
  isLoading.value = true;
  formMessage.text = null;
  formMessage.type = null;
  formMessage.errors = [];

  if (!termsVersion.value) {
    pageError.value = "Cannot register because the terms version could not be loaded.";
    return;
  }

  try {
    // The form data is already validated, so we can proceed.
    const apiData = {
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      password: formData.password,
      termsVersion: termsVersion.value,
    };
    await authService.register(apiData);

    // --- THIS IS THE FIX ---
    // After registration, the user is automatically logged in. We must initialize
    // their settings and set the public store's slug so the navbar is correct.
    await settingsService.initialize();
    await settingsService.initialize();
    usePublicPortfolioStore().currentSlug.value = (authService.user.value as User | null)?.slug || null;
    // --- END OF FIX ---

    signupSuccessMessage.value = 'Registration successful! Redirecting to your account...';
    showSignupSuccessModal.value = true;

    Object.keys(formData).forEach(key => (formData as Record<string, string>)[key] = '');

  } catch (err: unknown) {
    console.error("Signup error:", err);

    if (err instanceof ApiError) {
      signupErrorTitle.value = 'Registration Failed';
      if (err.errors && err.errors.length > 0) {
        signupErrorMessage.value = err.errors.map(e => e.message);
      } else {
        signupErrorMessage.value = err.message || 'An API error occurred during registration.';
      }
      err.errors?.forEach(e => {
        if (e.field && fieldErrors.hasOwnProperty(e.field)) {
          fieldErrors[e.field] = e.message;
        }
      });
    } else {
      signupErrorTitle.value = 'Unexpected Error';
      signupErrorMessage.value = 'An unexpected error occurred. Please try again.';
    }
    showSignupErrorModal.value = true;
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.signup-page {
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

/* Animations */
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

/* REMOVED: .card styling. Handled by .glass-card and .shimmering */

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

/* REMOVED: .btn-primary and .btn-primary:hover. Handled by global interactive classes. */

/* Ensure button visibility in light mode */
.btn-primary {
  background-color: var(--bs-primary);
  color: var(--bs-white);
  border-color: var(--bs-primary);
}

/* Responsive styles for small screens like iPhone SE */
@media (max-width: 576px) {
  .signup-page {
    padding: 1rem 0;
    align-items: flex-start;
  }

  .signup-page .container {
    max-width: 100%;
    padding: 0 1rem;
    margin-top: 5vh;
  }

  .signup-page .card-body {
    padding: 1.5rem;
  }

  .signup-page .fs-3 {
    font-size: 1.5rem !important;
  }
}
</style>
