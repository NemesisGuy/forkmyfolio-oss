<template>
  <div class="admin-user-create-page py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-8 col-md-10">
          <LoadingModal :visible="isSaving"/>
          <SuccessModal :visible="showSuccess"
                        message="The new user has been created successfully."
                        title="User Created"
                        @close="closeSuccessModal"/>
          <ErrorModal v-if="error" :message="error" :visible="true" title="Creation Failed"
                      @close="error = null"/>

          <div class="card glass-card p-0 animate-fade-in-up">
            <div class="card-body p-4 p-md-5">
              <h1 class="card-title mb-4 text-center display-6">Create New User</h1>
              <form @submit.prevent="handleCreate">
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label" for="firstName">First Name</label>
                    <input id="firstName" v-model="userData.firstName" class="form-control"
                           placeholder="Enter first name" required type="text">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="lastName">Last Name</label>
                    <input id="lastName" v-model="userData.lastName" class="form-control"
                           placeholder="Enter last name" required type="text">
                  </div>
                  <div class="col-12">
                    <label class="form-label" for="email">Email</label>
                    <input id="email" v-model="userData.email" class="form-control"
                           placeholder="user@example.com" required type="email">
                  </div>
                  <div class="col-12">
                    <label class="form-label" for="password">Password</label>
                    <input id="password" v-model="userData.password" class="form-control"
                           placeholder="Minimum 8 characters" required type="password">
                  </div>

                  <h5 class="mt-4 pt-2 fs-6 text-muted">Permissions & Status</h5>
                  <div class="col-md-6">
                    <label class="form-label">Roles</label>
                    <div class="form-check">
                      <input id="roleUser" v-model="userData.roles" class="form-check-input"
                             checked disabled type="checkbox" value="USER">
                      <label class="form-check-label" for="roleUser">User (Default)</label>
                    </div>
                    <div class="form-check">
                      <input id="roleAdmin" v-model="userData.roles" class="form-check-input"
                             type="checkbox" value="ADMIN">
                      <label class="form-check-label" for="roleAdmin">Admin</label>
                    </div>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label">Account Status</label>
                    <div class="form-check form-switch">
                      <input id="userActive" v-model="userData.active" class="form-check-input"
                             role="switch" type="checkbox">
                      <label class="form-check-label"
                             for="userActive">{{ userData.active ? 'Active' : 'Inactive' }}</label>
                    </div>
                  </div>
                </div>

                <div class="d-flex justify-content-end mt-4 pt-4 border-top border-white border-opacity-10">
                  <button class="btn btn-outline-secondary me-2" type="button" @click="cancel">
                    Cancel
                  </button>
                  <button :disabled="isSaving" class="btn btn-primary" type="submit">
                    <span v-if="isSaving" aria-hidden="true"
                          class="spinner-border spinner-border-sm me-2" role="status"></span>
                    {{ isSaving ? 'Creating...' : 'Create User' }}
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
import {reactive, ref} from 'vue';
import {useRouter} from 'vue-router';
import {createAdminUser} from '@/services/api/admin.api.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';

const router = useRouter();

const userData = reactive({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  roles: ['USER'],
  active: true
});

const isSaving = ref(false);
const error = ref(null);
const showSuccess = ref(false);

const handleCreate = async () => {
  isSaving.value = true;
  error.value = null;
  try {
    await createAdminUser(userData);
    showSuccess.value = true;
  } catch (err) {
    console.error("Failed to create user:", err);
    error.value = err.message || 'An unexpected error occurred while creating the user.';
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
