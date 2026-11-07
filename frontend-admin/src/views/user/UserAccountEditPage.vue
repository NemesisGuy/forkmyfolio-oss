<template>
  <div class="user-account-edit-page py-5 flex-grow-1">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-8 col-md-10">
          <LoadingModal :visible="isLoading"/>
          <SuccessModal :visible="showSuccess" message="Your account details have been updated."
                        title="Success"
                        @close="closeSuccessModal"/>
          <ErrorModal v-if="error" :message="error" :visible="true" title="Update Failed"
                      @close="error = null"/>

          <div v-if="!isLoading && accountData" class="card glass-card p-0 animate-fade-in-up">
            <div class="card-body p-4 p-md-5">
              <h1 class="card-title mb-4 text-center display-6">Edit Account Details</h1>
              <form @submit.prevent="handleSave">
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label" for="firstName">First Name</label>
                    <input id="firstName" v-model="accountData.firstName" class="form-control"
                           required type="text">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="lastName">Last Name</label>
                    <input id="lastName" v-model="accountData.lastName" class="form-control"
                           required type="text">
                  </div>
                  <!-- CORRECTED: Added profileImageUrl field to match the DTO -->
                  <div class="col-12">
                    <label class="form-label" for="profileImageUrl">Profile Image URL</label>
                    <input id="profileImageUrl" v-model="accountData.profileImageUrl"
                           class="form-control"
                           placeholder="https://example.com/path/to/image.jpg"
                           type="url">
                    <div class="form-text">Provide a direct URL to your profile picture. This is not
                      a file upload.
                    </div>
                  </div>
                </div>

                <div
                  class="d-flex justify-content-end mt-4 pt-4 border-top border-white border-opacity-10">
                  <button class="btn btn-outline-secondary me-2" type="button" @click="cancel">
                    Cancel
                  </button>
                  <button :disabled="isSaving" class="btn btn-primary" type="submit">
                    <span v-if="isSaving" aria-hidden="true"
                          class="spinner-border spinner-border-sm me-2" role="status"></span>
                    {{ isSaving ? 'Saving...' : 'Save Changes' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue';
import {useRouter} from 'vue-router';
import {getMyAccount, updateMyAccount} from '@/services/api/user.api';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import type { User } from '@/types';

const props = defineProps<{
  slug: string;
}>();

const router = useRouter();
const accountData = ref<User | null>(null);
const isLoading = ref(true);
const isSaving = ref(false);
const error = ref<string | null>(null);
const showSuccess = ref(false);

onMounted(async () => {
  try {
    isLoading.value = true;
    const data = await getMyAccount();
    accountData.value = reactive({...data}) as User;
  } catch (err: unknown) {
    console.error("Failed to fetch user account data:", err);
    error.value = (err as Error).message || 'An unexpected error occurred.';
  } finally {
    isLoading.value = false;
  }
});

const handleSave = async () => {
  if (!accountData.value) return;
  isSaving.value = true;
  error.value = null;
  try {
    // The DTO expects firstName, lastName, and profileImageUrl.
    // We create a payload to ensure we only send the fields the backend expects.
    const payload = {
      firstName: accountData.value.firstName,
      lastName: accountData.value.lastName,
      profileImageUrl: accountData.value.profileImageUrl,
    };
    await updateMyAccount(payload);
    showSuccess.value = true;
  } catch (err: unknown) {
    console.error("Failed to update account:", err);
    error.value = (err as Error).message || 'Could not save your changes.';
  } finally {
    isSaving.value = false;
  }
};

const closeSuccessModal = () => {
  showSuccess.value = false;
  router.push({name: 'profile', params: {slug: props.slug}});
};

const cancel = () => {
  router.push({name: 'profile', params: {slug: props.slug}});
};
</script>

<style scoped>
.user-account-edit-page .display-6 {
  font-weight: 300;
}
</style>
