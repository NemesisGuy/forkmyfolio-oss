<template>
  <div class="user-profile-edit-page py-5 flex-grow-1">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-8 col-md-10">
          <LoadingModal :visible="isLoading"/>
          <SuccessModal :visible="showSuccess" message="Your public profile has been updated."
                        title="Success" @close="closeSuccessModal"/>
          <ErrorModal v-if="error" :message="error" :visible="true" title="Update Failed"
                      @close="error = null"/>

          <div v-if="!isLoading && profileData" class="card glass-card p-0 animate-fade-in-up">
            <div class="card-body p-4 p-md-5">
              <h1 class="card-title mb-4 text-center display-6">Edit Public Profile</h1>
              <p class="text-center text-muted mb-4">This information is visible on your public
                portfolio page. Your profile picture is managed on the 'Edit Account' page.</p>
              <form @submit.prevent="handleSave">
                <div class="row g-3">
                  <!-- CORRECTED: The file upload section has been removed. -->

                  <div class="col-12">
                    <label class="form-label" for="headline">Headline</label>
                    <input id="headline" v-model="profileData.headline" class="form-control"
                           placeholder="e.g., Full-Stack Java Developer"
                           type="text">
                  </div>

                  <div class="col-12">
                    <label class="form-label" for="summary">Summary / Bio</label>
                    <textarea id="summary" v-model="profileData.summary" class="form-control"
                              placeholder="A brief summary about yourself..."
                              rows="5"></textarea>
                  </div>

                  <div class="col-md-6">
                    <label class="form-label" for="publicEmail">Public Email</label>
                    <input id="publicEmail" v-model="profileData.publicEmail" class="form-control"
                           placeholder="contact@example.com" type="email">
                  </div>

                  <div class="col-md-6">
                    <label class="form-label" for="location">Location</label>
                    <input id="location" v-model="profileData.location" class="form-control"
                           placeholder="e.g., San Francisco, CA" type="text">
                  </div>

                  <h5 class="mt-4 pt-2 fs-6 text-muted">Social & Professional Links</h5>
                  <div class="col-md-6">
                    <label class="form-label" for="websiteUrl">Website URL</label>
                    <input id="websiteUrl" v-model="profileData.websiteUrl" class="form-control"
                           placeholder="https://your-website.com" type="url">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="linkedinUrl">LinkedIn URL</label>
                    <input id="linkedinUrl" v-model="profileData.linkedinUrl" class="form-control"
                           placeholder="https://linkedin.com/in/your-profile"
                           type="url">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="githubUrl">GitHub URL</label>
                    <input id="githubUrl" v-model="profileData.githubUrl" class="form-control"
                           placeholder="https://github.com/your-username"
                           type="url">
                  </div>

                  <h5 class="mt-4 pt-2 fs-6 text-muted">Resume & Cover Letter</h5>
                  <div class="col-md-6">
                    <label class="form-label" for="resumeUrl">Resume URL</label>
                    <input id="resumeUrl" v-model="profileData.resumeUrl" class="form-control"
                           placeholder="URL to your hosted PDF resume"
                           type="url">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label" for="resumeImageUrl">Resume Preview Image URL</label>
                    <input id="resumeImageUrl" v-model="profileData.resumeImageUrl"
                           class="form-control"
                           placeholder="URL to an image of your resume"
                           type="url">
                  </div>
                  <div class="col-12">
                    <label class="form-label" for="coverLetterTemplate">Cover Letter
                      Template</label>
                    <textarea id="coverLetterTemplate" v-model="profileData.coverLetterTemplate"
                              class="form-control"
                              placeholder="A generic cover letter template you can reuse."
                              rows="8"></textarea>
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

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getMyPublicProfile, updateMyPublicProfile } from '@/services/api/user.api.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import type { Profile } from '@/types';

const props = defineProps<{
  slug: string;
}>();

const router = useRouter();
const profileData = ref<Profile | null>(null);
const isLoading = ref(true);
const isSaving = ref(false);
const error = ref<string | null>(null);
const showSuccess = ref(false);

onMounted(async () => {
  try {
    isLoading.value = true;
    const data = await getMyPublicProfile();
    profileData.value = reactive({ ...data }) as Profile;
  } catch (err: unknown) {
    console.error("Failed to fetch public profile data:", err);
    error.value = err instanceof Error ? err.message : 'An unexpected error occurred.';
  } finally {
    isLoading.value = false;
  }
});

const handleSave = async () => {
  if (!profileData.value) return;
  isSaving.value = true;
  error.value = null;
  try {
    // CORRECTED: Create a payload that exactly matches the backend's
    // UpdatePortfolioProfileRequest DTO. This prevents sending extra fields
    // like firstName, lastName, etc., which were causing the error.
    const data = profileData.value;
    const payload = {
      headline: data.headline,
      summary: data.summary,
      publicEmail: data.publicEmail,
      location: data.location,
      websiteUrl: data.websiteUrl,
      linkedinUrl: data.linkedinUrl,
      githubUrl: data.githubUrl,
      resumeUrl: data.resumeUrl,
      resumeImageUrl: data.resumeImageUrl,
      coverLetterTemplate: data.coverLetterTemplate,
    };
    await updateMyPublicProfile(payload);
    showSuccess.value = true;
  } catch (err: unknown) {
    console.error("Failed to update public profile:", err);
    error.value = err instanceof Error ? err.message : 'Could not save your changes.';
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
.user-profile-edit-page .display-6 {
  font-weight: 300;
}
</style>
