<template>
  <div class="admin-user-edit-page py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-8 col-md-10">
          <LoadingModal :visible="isLoading || isSaving"/>
          <SuccessModal :visible="showSuccess"
                        message="The user's details have been updated successfully."
                        title="User Updated"
                        @close="closeSuccessModal"/>
          <ErrorModal v-if="error" :message="error" :visible="true" title="Update Failed"
                      @close="error = null"/>

          <div v-if="!isLoading && userData" class="card glass-card p-0 animate-fade-in-up">
            <div class="card-body p-4 p-md-5">
              <h1 class="card-title mb-4 text-center display-6">Edit User: {{
                  originalUserName
                }}</h1>
              <form @submit.prevent="handleSave">
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label" for="firstName">First Name</label>
                    <input id="firstName" v-model="userData.firstName" class="form-control"
                           required type="text">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="lastName">Last Name</label>
                    <input id="lastName" v-model="userData.lastName" class="form-control"
                           required type="text">
                  </div>
                  <div class="col-12">
                    <label class="form-label" for="email">Email</label>
                    <input id="email" v-model="userData.email" class="form-control" required
                           type="email">
                  </div>
                  <div class="col-12">
                    <label class="form-label" for="password">New Password</label>
                    <input id="password" v-model="userData.password" class="form-control"
                           type="password">
                    <div class="form-text">Leave blank to keep the current password.</div>
                  </div>
                  <div class="col-12">
                    <label class="form-label" for="slug">Public Slug</label>
                    <input id="slug" v-model="userData.slug" class="form-control" required
                           type="text">
                    <div class="form-text">A unique identifier for the user's public portfolio URL
                      (e.g., /john-doe).
                    </div>
                  </div>

                  <h5 class="mt-4 pt-2 fs-6 text-muted">Permissions & Status</h5>
                  <div class="col-md-6">
                    <label class="form-label">Roles</label>
                    <div class="form-check">
                      <input id="roleUser" v-model="userData.roles" class="form-check-input"
                             disabled
                             type="checkbox" value="USER">
                      <label class="form-check-label" for="roleUser">User (Default)</label>
                    </div>
                    <div class="form-check">
                      <input id="roleAdmin" v-model="userData.roles" class="form-check-input"
                             type="checkbox"
                             value="ADMIN">
                      <label class="form-check-label" for="roleAdmin">Admin</label>
                    </div>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label">Account Status</label>
                    <div class="form-check form-switch">
                      <input id="userActive" v-model="userData.active" class="form-check-input"
                             role="switch"
                             type="checkbox">
                      <label class="form-check-label"
                             for="userActive">{{ userData.active ? 'Active' : 'Inactive' }}</label>
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
import {getAdminUserById, updateAdminUser} from '@/services/api/admin.api.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';

const router = useRouter();

// Per the route definition in `router/index.js`, the user ID is passed as a prop.
const props = defineProps({
  id: {
    type: [String, Number],
    required: true,
  },
});

// The ID from the route is a UUID string.
const userId = props.id;

const userData = ref(null);
const originalUserName = ref('');

const isLoading = ref(true);
const isSaving = ref(false);
const error = ref(null);
const showSuccess = ref(false);

onMounted(async () => {
  if (!userId) {
    error.value = 'No user ID provided.';
    isLoading.value = false;
    return;
  }
  try {
    isLoading.value = true;
    // Data from getAdminUserById is now pre-normalized by the service layer.
    // No need to clean up roles here anymore.
    const data = await getAdminUserById(userId);

    userData.value = reactive({...data, password: ''}); // Don't pre-fill password
    originalUserName.value = `${data.firstName} ${data.lastName}`;
  } catch (err) {
    console.error("An error occurred during fetch.", err);
    error.value = err.message || 'Could not load user data.';
  } finally {
    isLoading.value = false;
  }
});

const handleSave = async () => {
  isSaving.value = true;
  error.value = null;
  try {
    // Create a payload and remove the password if it's empty
    const payload = {...userData.value};
    if (!payload.password) {
      delete payload.password;
    }
    // The payload.roles array is already in the correct format because the data was cleaned on load.
    await updateAdminUser(userId, payload);
    showSuccess.value = true;
  } catch (err) {
    console.error("Failed to update user:", err);
    error.value = err.message || 'An unexpected error occurred while updating the user.';
  } finally {
    isSaving.value = false;
  }
};

const closeSuccessModal = () => {
  showSuccess.value = false;
  router.push({name: 'admin-users'});
};

const cancel = () => {
  router.back();
};
</script>

<style scoped>
.display-6 {
  font-weight: 300;
}

.form-check-input[type="checkbox"] {
  border-radius: .25em;
}

.form-check-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
