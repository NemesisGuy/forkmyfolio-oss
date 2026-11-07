<template>
  <div class="force-password-change-page py-5 flex-grow-1">
    <div class="container" style="max-width: 500px;">
      <!--
        THIS IS THE FIX: Removed the 'shimmering' class. This class adds a complex animated overlay
        that was causing a stubborn rendering bug, making the content invisible. The other working modals
        do not use this class. The card will now be visible, just like the modals.
      -->
      <div class="card glass-card animate-fade-in-up">
        <div class="card-body p-4 p-md-5">
          <h1 class="card-title text-center mb-2 fs-3">Update Your Password</h1>
          <p class="text-center text-muted mb-4">
            For security, you must change your temporary password before proceeding.
          </p>

          <!-- Re-instating the error modal -->
          <ErrorModal :message="error || ''" :visible="!!error" title="Update Failed" @close="error = null"/>

          <form @submit.prevent="handleSubmit">
            <!-- Hidden username field for accessibility and password managers -->
            <input v-if="userEmail" :value="userEmail" autocomplete="username" type="text" class="visually-hidden">

            <div class="mb-3">
              <label class="form-label" for="newPassword">New Password</label>
              <input id="newPassword" v-model="newPassword" class="form-control" minlength="8"
                     required
                     type="password" autocomplete="new-password">
            </div>
            <div class="mb-4">
              <label class="form-label" for="confirmPassword">Confirm New Password</label>
              <input id="confirmPassword" v-model="confirmPassword" class="form-control" required
                     type="password" autocomplete="new-password">
            </div>
            <button :disabled="isLoading" class="btn btn-primary w-100 interactive-lift" type="submit">
              <span v-if="isLoading" aria-hidden="true" class="spinner-border spinner-border-sm me-2"
                    role="status"></span>
              {{ isLoading ? 'Updating...' : 'Update Password & Continue' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * @file src/views/auth/ForcePasswordChangePage.vue
 * @description A dedicated page that forces a user to change their temporary password
 * after their first login or a password reset. It is a blocking page controlled by the router guard.
 */
import {computed, ref} from 'vue';
import {useRouter} from 'vue-router';
import {changeMyPassword} from '@/services/api/user.api.js';
import {authService} from '@/services/authService.js';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import {notificationService} from '@/services/notificationService.js';
import type { User } from '@/types';

const router = useRouter();

const newPassword = ref('');
const confirmPassword = ref('');
const isLoading = ref(false);
const error = ref<string | null>(null);

const userEmail = computed(() => (authService.user.value as User | null)?.email);

const validateForm = (): string | null => {
  if (newPassword.value !== confirmPassword.value) {
    return 'Passwords do not match.';
  }
  if (newPassword.value.length < 8) {
    return 'Password must be at least 8 characters long.';
  }
  return null; // No errors
};

const handleSubmit = async () => {
  error.value = validateForm();
  if (error.value) return;

  isLoading.value = true;
  try {
    await changeMyPassword(newPassword.value);

    // Manually update the local user state to prevent the navigation guard from re-triggering
    authService.passwordHasBeenChanged();

    notificationService.add({
      type: 'success',
      message: 'Password updated successfully! Welcome to your dashboard.',
    });

    // Redirect to the user's dashboard
    const userSlug = authService.user.value?.slug;
    if (userSlug) {
      router.push({name: 'dashboard', params: {slug: userSlug}});
    } else {
      router.push('/'); // Fallback to home
    }
  } catch (err: unknown) {
    console.error('Failed to change password:', err);
    error.value = (err as Error).message || 'An unexpected error occurred. Please try again.';
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.force-password-change-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
}

/*
  This ensures the container sits above the page's animated background,
  which uses a pseudo-element that can cover content.
*/
.force-password-change-page .container {
  position: relative;
  z-index: 1;
}
</style>
