<template>
  <div class="admin-portfolio-profile-page py-4">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-8">
          <h1 class="display-5 mb-4">Manage Portfolio Profile</h1>

          <LoadingModal :visible="isLoading"/>
          <ErrorModal v-if="error.message" :message="error.message" :title="error.title"
                      :visible="showErrorModal"
                      @close="closeErrorModal"/>
          <SuccessModal :message="successMessage" :visible="showSuccessModal"
                        title="Update Successful"
                        @close="closeSuccessModal"/>

          <div v-if="!isLoading" class="card shadow-sm">
            <div class="card-body p-4">

              <div class="d-flex align-items-center mb-4 pb-3 border-bottom">
                <img v-if="profileImageUrl" :src="profileImageUrl" alt="Profile Picture"
                     class="profile-image rounded-circle me-3">
                <div v-else
                     class="profile-image-placeholder rounded-circle me-3 d-flex align-items-center justify-content-center">
                  <UserCircle />
                </div>
                <div>
                  <h5 class="mb-0">{{ fullName }}</h5>
                  <p class="text-muted small mb-0">
                    To change your name or profile image, please visit the
                    <router-link to="/admin/account">Account Settings</router-link>
                    page.
                  </p>
                </div>
              </div>

              <form @submit.prevent="handleSave">
                <!-- Main Info Section -->
                <h5 class="mb-3">Main Information</h5>
                <div class="row g-3">
                  <div class="col-12 py-3 border-bottom mb-3">
                    <div class="form-check form-switch ps-0 d-flex align-items-center justify-content-between">
                      <div>
                        <label class="form-check-label h6 mb-1" for="publiclyVisible">
                          Public Portfolio Visibility
                        </label>
                        <p class="text-muted small mb-0">
                          When enabled, anyone with your link can view your portfolio. When disabled, the page will be locked.
                        </p>
                      </div>
                      <input id="publiclyVisible" v-model="formState.publiclyVisible" class="form-check-input ms-3"
                             role="switch" type="checkbox">
                    </div>
                  </div>

                  <div class="col-12">
                    <label class="form-label" for="headline">Headline</label>
                    <input id="headline" v-model="formState.headline" class="form-control"
                           placeholder="e.g., Full-Stack Software Engineer"
                           required type="text">
                  </div>
                  <div class="col-12">
                    <label class="form-label" for="summary">Summary / Bio</label>
                    <textarea id="summary" v-model="formState.summary" class="form-control"
                              placeholder="A passionate developer with experience in..."
                              rows="6"></textarea>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="location">Location</label>
                    <input id="location" v-model="formState.location" class="form-control"
                           placeholder="e.g., San Francisco, CA"
                           type="text">
                  </div>
                </div>

                <hr class="my-4">

                <!-- Links Section -->
                <h5 class="mb-3">Contact & Links</h5>
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label" for="publicEmail">Public Email</label>
                    <input id="publicEmail" v-model="formState.publicEmail" class="form-control"
                           placeholder="contact.jane@example.com"
                           type="email">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="websiteUrl">Website URL</label>
                    <input id="websiteUrl" v-model="formState.websiteUrl" class="form-control"
                           placeholder="https://your-portfolio.com"
                           type="url">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="linkedinUrl">LinkedIn URL</label>
                    <input id="linkedinUrl" v-model="formState.linkedinUrl" class="form-control"
                           placeholder="https://linkedin.com/in/your-profile"
                           type="url">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="githubUrl">GitHub URL</label>
                    <input id="githubUrl" v-model="formState.githubUrl" class="form-control"
                           placeholder="https://github.com/your-username"
                           type="url">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="resumeUrl">Resume URL</label>
                    <input id="resumeUrl" v-model="formState.resumeUrl" class="form-control"
                           placeholder="Link to your canonical PDF resume"
                           type="url">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="resumeImageUrl">Resume Preview Image URL</label>
                    <input id="resumeImageUrl" v-model="formState.resumeImageUrl"
                           class="form-control"
                           placeholder="Link to a preview image of the resume"
                           type="url">
                  </div>
                </div>

                <hr class="my-4">

                <!-- Cover Letter Section -->
                <h5 class="mb-3">Cover Letter Template</h5>
                <div class="row g-3">
                  <div class="col-12">
                    <label class="form-label" for="coverLetterTemplate">Default Cover Letter</label>
                    <textarea id="coverLetterTemplate" v-model="formState.coverLetterTemplate"
                              class="form-control"
                              placeholder="Dear Hiring Manager, I am writing to express my keen interest in..."
                              rows="8"></textarea>
                    <div class="form-text">This can be displayed on your home page as a template for
                      visitors.
                    </div>
                  </div>
                </div>

                <!-- Action Buttons -->
                <div class="d-flex justify-content-end mt-4">
                  <button :disabled="isSaving" class="btn btn-primary" type="submit">
                    <span v-if="isSaving" class="spinner-border spinner-border-sm me-1"></span>
                    {{ isSaving ? 'Saving...' : 'Save Profile' }}
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
import {computed, onMounted, reactive, ref} from 'vue';
import { UserCircle } from 'lucide-vue-next';
import type { User, Profile } from '@/types';
import {
  getMyAccount,
  getMyPublicProfile,
  updateMyPublicProfile,
  ApiError
} from '@/services/api/index.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';

const isLoading = ref(true);
const isSaving = ref(false);
const profileExists = ref(false); // This flag determines whether to CREATE or UPDATE

const error = ref({title: '', message: ''});
const showErrorModal = ref(false);

const showSuccessModal = ref(false);
const successMessage = ref('');

// Data for display only (from the User Account)
const profileImageUrl = ref('');
const firstName = ref('');
const lastName = ref('');
const fullName = computed(() => `${firstName.value} ${lastName.value}`.trim());

// Data for the form (from the Portfolio Profile)
const formState = reactive({
  headline: '',
  summary: '',
  location: '',
  publicEmail: '',
  websiteUrl: '',
  linkedinUrl: '',
  githubUrl: '',
  resumeUrl: '',
  resumeImageUrl: '',
  coverLetterTemplate: '',
  publiclyVisible: false
});

const closeErrorModal = () => {
  showErrorModal.value = false;
  error.value = {title: '', message: ''};
};

const closeSuccessModal = () => {
  showSuccessModal.value = false;
  successMessage.value = '';
};

onMounted(async () => {
  isLoading.value = true;
  try {
    // --- STEP 1: Fetch account data for display ---
    // This should always succeed for a logged-in admin.
    const accountData = await getMyAccount() as User;
    profileImageUrl.value = accountData.profileImageUrl || '';
    firstName.value = accountData.firstName;
    lastName.value = accountData.lastName;

    // --- STEP 2: Try to fetch the editable portfolio profile data ---
    try {
      const profileData = await getMyPublicProfile() as Profile;
      // If this succeeds, a profile exists. Populate the form.
      Object.assign(formState, profileData);
      profileExists.value = true;
    } catch (profileError) {
      if (profileError instanceof ApiError && profileError.httpStatus === 404) {
        // This is an expected "error": no profile exists yet. The user will create one.
        console.log("No existing profile found. User will create a new one.");
        profileExists.value = false;
      } else {
        // This is a real, unexpected error (e.g., network, server issue)
        throw profileError;
      }
    }

  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : 'Could not load your page data.';
    // This catches errors from either getAccount() or non-404 errors from getPortfolioProfile()
    console.error("Failed to load page data:", err);
    error.value = {
      title: 'Failed to Load Data',
      message: errorMsg
    };
    showErrorModal.value = true;
  } finally {
    isLoading.value = false;
  }
});

const handleSave = async () => {
  isSaving.value = true;
  try {
    // Both CREATE and UPDATE now use the same PUT /me/profile endpoint in the backend
    // which handles "upsert" logic.
    await updateMyPublicProfile(formState);
    profileExists.value = true;
    successMessage.value = 'Your portfolio profile has been saved successfully.';
    showSuccessModal.value = true;
  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : 'Could not save your profile.';
    console.error("Failed to save profile:", err);
    error.value = {
      title: 'Save Failed',
      message: errorMsg
    };
    showErrorModal.value = true;
  } finally {
    isSaving.value = false;
  }
};
</script>

<style scoped>
.admin-portfolio-profile-page h1,
.admin-portfolio-profile-page h5 {
  font-weight: 300;
}

.profile-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
}

.profile-image-placeholder {
  width: 60px;
  height: 60px;
  background-color: var(--bs-tertiary-bg);
}

.profile-image-placeholder .bi {
  font-size: 2rem;
  color: var(--bs-secondary-color);
}
</style>
