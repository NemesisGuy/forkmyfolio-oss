<template>
  <div class="user-account-page py-5 flex-grow-1">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-8">
          <LoadingModal :visible="isLoading"/>
          <ErrorModal v-if="error" :message="error" :visible="true" title="Error Loading Account"
                      @close="error = null"/>

          <div v-if="!isLoading && user">
            <div
              class="card glass-card shimmering p-0 animate-fade-in-up interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body p-4 p-md-5">
                <div class="text-center mb-4">
                  <img v-if="fullProfileImageUrl" :src="fullProfileImageUrl"
                       alt="Profile Picture"
                       class="profile-image img-fluid rounded-circle shadow-lg">
                  <div v-else
                       class="profile-image-placeholder rounded-circle shadow-lg d-flex align-items-center justify-content-center">
                    <UserCircle />
                  </div>
                </div>

                <h1 class="card-title mb-4 text-center display-6">My Account</h1>

                <div class="account-details">
                  <div class="detail-item">
                    <strong>Name:</strong>
                    <span>{{ user.firstName }} {{ user.lastName }}</span>
                  </div>
                  <div class="detail-item">
                    <strong>Email:</strong>
                    <span>{{ user.email }}</span>
                  </div>
                  <div class="detail-item">
                    <strong>Public Slug:</strong>
                    <span>/{{ user.slug }}</span>
                  </div>
                  <div class="detail-item">
                    <strong>Roles:</strong>
                    <div>
                      <span v-for="role in user.roles" :key="role"
                            class="badge me-1">
                        {{ role.replace('ROLE_', '') }}
                      </span>
                    </div>
                  </div>
                </div>

                <div
                  class="d-flex flex-wrap justify-content-center mt-4 pt-4 border-top border-white border-opacity-10">
                  <button
                    class="btn btn-primary me-2 mb-2 interactive-lift interactive-shadow-primary"
                    @click="editAccount">
                    <Settings class="me-1" /> Edit Account Details
                  </button>
                  <button class="btn btn-outline-secondary mb-2 interactive-lift"
                          @click="editPublicProfile">
                    <LayoutPanelLeft class="me-1" /> Edit Public Profile
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { UserCircle, Settings, LayoutPanelLeft } from 'lucide-vue-next';
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {getMyAccount} from '@/services/api/user.api.js'; // Use the correct user-specific API
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import type { User } from '@/types';

const router = useRouter();
const user = ref<User | null>(null);
const isLoading = ref(true);
const error = ref<string | null>(null);

const fullProfileImageUrl = computed(() => {
  const imageUrl = user.value?.profileImageUrl;
  if (!imageUrl) return null;
  if (/^(https?:\/\/|\/\/)/.test(imageUrl)) return imageUrl;

  const apiBaseUrl = (import.meta as unknown as { env: Record<string, string> }).env.VITE_API_BASE_URL;
  if (!apiBaseUrl) return imageUrl;

  try {
    const serverUrl = new URL(apiBaseUrl);
    const serverRoot = `${serverUrl.protocol}//${serverUrl.host}`;
    return `${serverRoot}${imageUrl.startsWith('/') ? imageUrl : '/' + imageUrl}`;
  } catch {
    return imageUrl;
  }
});

onMounted(async () => {
  try {
    isLoading.value = true;
    user.value = await getMyAccount() as User; // Fetch data for the authenticated user
  } catch (err: unknown) {
    console.error("Failed to fetch user account data:", err);
    error.value = (err as Error).message || 'An unexpected error occurred.';
  } finally {
    isLoading.value = false;
  }
});

// --- UPDATED: Navigate to the new editor pages ---
const editAccount = () => {
  router.push({name: 'edit-account'});
};

const editPublicProfile = () => {
  router.push({name: 'edit-public-profile'});
};
</script>

<style scoped>
.user-account-page {
  min-height: calc(100dvh - 57px);
  /* Background handled by main-content in App.vue */
  overflow-x: hidden;
}

.user-account-page .display-6 {
  font-weight: 300;
}

.card-body {
  position: relative;
  z-index: 1;
}

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

.profile-image,
.profile-image-placeholder {
  width: 150px;
  height: 150px;
  object-fit: cover;
  border: 4px solid var(--glass-bg);
  transition: transform 0.3s ease;
}

.profile-image:hover,
.profile-image-placeholder:hover {
  transform: scale(1.05);
}

.profile-image-placeholder {
  background-color: var(--bs-tertiary-bg);
}

.profile-image-placeholder .bi {
  font-size: 5rem;
  color: var(--bs-secondary-color);
}

.account-details {
  max-width: 450px;
  margin: 0 auto;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--bs-border-color-translucent);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item strong {
  color: var(--bs-emphasis-color);
}

.detail-item .badge {
  background-color: rgba(var(--bs-primary-rgb), 0.2) !important;
  color: var(--bs-primary) !important;
  font-weight: 500;
  padding: 0.4em 0.7em;
  text-transform: capitalize;
}

/* Responsive styles for mobile-first design */
@media (max-width: 576px) {
  .user-account-page {
    padding: 1rem 0;
  }

  .user-account-page .container {
    padding: 0 1rem;
  }

  .user-account-page .card-body {
    padding: 1.5rem;
  }

  .profile-image,
  .profile-image-placeholder {
    width: 100px;
    height: 100px;
  }

  .profile-image-placeholder .bi {
    font-size: 3rem;
  }

  .account-details {
    max-width: 100%;
  }

  .detail-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }

  .display-6 {
    font-size: 1.8rem;
  }
}

/* Tablet enhancements */
@media (min-width: 768px) {
  .profile-image,
  .profile-image-placeholder {
    width: 120px;
    height: 120px;
  }

  .profile-image-placeholder .bi {
    font-size: 4rem;
  }
}

/* Desktop enhancements */
@media (min-width: 1024px) {
  .user-account-page .container {
    max-width: 800px;
  }
}
</style>
