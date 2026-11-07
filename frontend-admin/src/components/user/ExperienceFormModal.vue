<template>
  <div id="experienceModal" ref="modalRef" aria-hidden="true" aria-labelledby="experienceModalLabel"
       class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content glass-modal">
        <div class="modal-header">
          <h5 id="experienceModalLabel" class="modal-title">
            {{ isEditing ? 'Edit Experience' : 'Add New Experience' }}</h5>
          <button aria-label="Close" class="btn-close" data-bs-dismiss="modal"
                  type="button"></button>
        </div>
        <div class="modal-body" style="max-height: 75vh; overflow-y: auto;">
          <form class="row g-3" @submit.prevent="submitForm">
            <!-- Job Title & Display Order -->
            <div class="row">
              <div class="col-md-8 mb-3">
                <label class="form-label" for="expJobTitle">Job Title</label>
                <input id="expJobTitle" v-model="formState.jobTitle" class="form-control"
                       required type="text">
              </div>
              <div class="col-md-4 mb-3">
                <label class="form-label" for="expDisplayOrder">Display Order</label>
                <input id="expDisplayOrder" v-model.number="formState.displayOrder"
                       class="form-control"
                       type="number">
              </div>
            </div>
            <!-- Company & Employment Type -->
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expCompanyName">Company</label>
                <input id="expCompanyName" v-model="formState.companyName" class="form-control"
                       required type="text">
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expEmploymentType">Employment Type</label>
                <select id="expEmploymentType" v-model="formState.employmentType"
                        class="form-select">
                  <option v-for="type in EMPLOYMENT_TYPES" :key="type.value" :value="type.value">
                    {{ type.text }}
                  </option>
                </select>
              </div>
            </div>
            <!-- Location & Location Type -->
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expLocation">Location (e.g., "City, Country")</label>
                <input id="expLocation" v-model="formState.location" class="form-control"
                       type="text">
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expLocationType">Location Type</label>
                <select id="expLocationType" v-model="formState.locationType" class="form-select">
                  <option v-for="type in LOCATION_TYPES" :key="type.value" :value="type.value">
                    {{ type.text }}
                  </option>
                </select>
              </div>
            </div>
            <!-- Dates -->
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expStartDate">Start Date</label>
                <input id="expStartDate" v-model="formState.startDate" class="form-control"
                       required type="date">
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expEndDate">End Date</label>
                <input id="expEndDate" v-model="formState.endDate"
                       :disabled="formState.isCurrentJob" class="form-control"
                       type="date">
              </div>
            </div>
            <!-- Current Job Toggle -->
            <div class="form-check form-switch mb-3">
              <input id="isCurrentJob" v-model="formState.isCurrentJob" class="form-check-input"
                     type="checkbox">
              <label class="form-check-label" for="isCurrentJob">I currently work here</label>
            </div>
            <!-- Description & Achievements -->
            <div class="mb-3">
              <label class="form-label" for="expDescription">Description</label>
              <textarea id="expDescription" v-model="formState.description" class="form-control"
                        placeholder="Overview of your role and responsibilities."
                        rows="3"></textarea>
            </div>
            <div class="mb-3">
              <label class="form-label" for="expAchievements">Achievements (Optional)</label>
              <textarea id="expAchievements" v-model="formState.achievements" class="form-control"
                        placeholder="Key accomplishments or highlights." rows="3"></textarea>
            </div>

            <!-- Use the new SkillTagInput component -->
            <div class="mb-3">
              <label class="form-label">Skills</label>
              <SkillTagInput v-model="formState.skills" :available-skills="availableSkills"/>
            </div>

            <hr class="my-4">
            <h6 class="text-muted mb-3">Optional Links & Media</h6>
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expCompanyUrl">Company Website URL</label>
                <input id="expCompanyUrl" v-model="formState.companyUrl" class="form-control"
                       placeholder="https://..." type="url">
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label" for="expCompanyLogoUrl">Company Logo URL</label>
                <input id="expCompanyLogoUrl" v-model="formState.companyLogoUrl"
                       class="form-control"
                       placeholder="https://..." type="url">
              </div>
            </div>
            <hr class="my-4">
            <div class="form-check form-switch mb-3">
              <input id="expVisible" v-model="formState.visible" class="form-check-input"
                     role="switch"
                     type="checkbox">
              <label class="form-check-label" for="expVisible">Visible on public portfolio</label>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" data-bs-dismiss="modal" type="button">Close</button>
          <button class="btn btn-primary" data-bs-dismiss="modal" type="button" @click="submitForm">
            {{ isEditing ? 'Save Changes' : 'Add Experience' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {defineExpose, onMounted, onUnmounted, reactive, ref, watch} from 'vue';
import {Modal} from 'bootstrap';
import SkillTagInput from './SkillTagInput.vue';

// --- Props and Emits ---
import type { Experience, Skill } from '@/types';

const props = defineProps({
  experience: { type: Object as () => Experience | null, default: null },
  availableSkills: { type: Array as () => string[], required: true },
});
const emit = defineEmits(['save']);

// --- Constants ---
const EMPLOYMENT_TYPES = [
  { value: 'FULL_TIME', text: 'Full-time' }, { value: 'PART_TIME', text: 'Part-time' },
  { value: 'CONTRACT', text: 'Contract' }, { value: 'FREELANCE', text: 'Freelance' },
  { value: 'INTERNSHIP', text: 'Internship' }, { value: 'APPRENTICESHIP', text: 'Apprenticeship' }
];
const LOCATION_TYPES = [
  { value: 'ON_SITE', text: 'On-site' }, { value: 'HYBRID', text: 'Hybrid' }, {
    value: 'REMOTE',
    text: 'Remote'
  }
];

// --- State ---
const modalRef = ref<HTMLElement | null>(null); // This ref is on the modal's root div
let modalInstance: Modal | null = null;
const isEditing = ref(false);

const getInitialFormState = () => ({
  uuid: null as string | null,
  jobTitle: '',
  companyName: '',
  companyUrl: '',
  companyLogoUrl: '',
  location: '',
  locationType: 'ON_SITE',
  employmentType: 'FULL_TIME',
  startDate: '',
  endDate: '',
  description: '',
  achievements: '',
  displayOrder: 100,
  skills: [] as string[],
  visible: true,
  isCurrentJob: false,
});
const formState = reactive(getInitialFormState());

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
  }
});

// --- Watchers ---
watch(() => props.experience, (newExp) => {
  if (newExp) {
    isEditing.value = true;
    Object.assign(formState, {
      uuid: newExp.uuid || null,
      jobTitle: newExp.jobTitle || '',
      companyName: newExp.companyName || '',
      companyUrl: newExp.companyUrl || '',
      companyLogoUrl: newExp.companyLogoUrl || '',
      location: newExp.location || '',
      locationType: newExp.locationType || 'ON_SITE',
      employmentType: newExp.employmentType || 'FULL_TIME',
      startDate: newExp.startDate || '',
      endDate: newExp.endDate || '',
      description: newExp.description || '',
      achievements: newExp.achievements || '',
      displayOrder: newExp.displayOrder ?? 100,
      skills: newExp.skills ? newExp.skills.map((skill: string | Skill) =>
        typeof skill === 'string' ? skill : skill.name
      ) : [],
      visible: newExp.visible ?? true,
      isCurrentJob: !newExp.endDate,
    });
  } else {
    isEditing.value = false;
    Object.assign(formState, getInitialFormState());
  }
}, { deep: true, immediate: true });

watch(() => formState.isCurrentJob, (isCurrent) => {
  if (isCurrent) {
    formState.endDate = '';
  }
});

// --- Methods ---
const buildPayload = () => {
  return {
    ...formState,
    endDate: formState.isCurrentJob ? null : formState.endDate,
  };
};

// --- Modal History & Show Logic ---
// Simple manual history management for now to fix the UX quickly
const show = () => {
  // Push state so back button closes modal
  history.pushState({ modal: 'experience' }, '', '#modal-open');
  modalInstance?.show();
};

const hide = () => {
  modalInstance?.hide();
};

const submitForm = () => {
  emit('save', buildPayload());
  // Helper to sync history if needed (handled by parent usually via hide / re-fetch)
};

// Listen for popstate to close modal on back button
const handlePopState = (event: PopStateEvent) => {
  console.log('[ExperienceModal] Detected popstate. Event state:', event.state);
  try {
    if (modalInstance) {
      console.log('[ExperienceModal] Calling modalInstance.hide()...');
      modalInstance.hide();
      console.log('[ExperienceModal] modalInstance.hide() call successful.');
    } else {
      console.warn('[ExperienceModal] No modalInstance found during popstate!');
      // Attempt recovery if modalRef exists but instance is null
      if (modalRef.value) {
        console.log('[ExperienceModal] Attempting to recover modalInstance...');
        modalInstance = new Modal(modalRef.value);
        // Try hiding again after recovery
        if (modalInstance) {
          console.log('[ExperienceModal] Recovered modalInstance, attempting hide again.');
          modalInstance.hide();
          console.log('[ExperienceModal] Recovered modalInstance.hide() call successful.');
        }
      }
    }
  } catch (e) {
    console.error('[ExperienceModal] Failed to call modalInstance.hide():', e);
  }

  console.log('[ExperienceModal] Scheduling safety fallback cleanup (1000ms)...');
  // Safety fallback for stubborn mobile/headless transitions
  setTimeout(() => {
    const bodyHasModalOpen = document.body.classList.contains('modal-open');
    const modalHasShow = modalRef.value?.classList.contains('show');
    const backdrops = document.querySelectorAll('.modal-backdrop');

    console.log(`[ExperienceModal] Cleanup check after 1s. body.modal-open: ${bodyHasModalOpen}, modal.show: ${modalHasShow}, backdrops: ${backdrops.length}`);

    if (bodyHasModalOpen || modalHasShow || backdrops.length > 0) {
      console.warn('[ExperienceModal] Modal stuck visible after .hide(), forcing deep cleanup.');
      if (modalRef.value) {
        modalRef.value.classList.remove('show');
        modalRef.value.classList.remove('fade');
        modalRef.value.style.display = 'none';
        modalRef.value.setAttribute('aria-hidden', 'true');
        modalRef.value.removeAttribute('aria-modal');
        modalRef.value.removeAttribute('role');
      }
      document.body.classList.remove('modal-open');
      document.body.style.overflow = '';
      document.body.style.paddingRight = '';
      backdrops.forEach(el => el.remove());
    }
  }, 1000); // Wait longer for mobile transitions
};

onMounted(() => {
   console.log('[ExperienceModal] onMounted: attaching popstate listener.');
   window.addEventListener('popstate', handlePopState);
   // Clean up hash on close
   if (modalRef.value) {
     modalRef.value.addEventListener('hidden.bs.modal', () => {
       if (window.location.hash === '#modal-open') {
         history.back();
       }
     });
   }
});

onUnmounted(() => {
  console.log('[ExperienceModal] Component unmounting. Cleaning up listeners and DOM.');
  window.removeEventListener('popstate', handlePopState);

  // Emergency immediate cleanup in case Bootstrap linger
  if (document.body.classList.contains('modal-open')) {
    console.log('[ExperienceModal] Modal remains in body on unmount, forcing cleanup.');
    document.body.classList.remove('modal-open');
    document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
  }
});

defineExpose({
  show,
  hide,
});
</script>
