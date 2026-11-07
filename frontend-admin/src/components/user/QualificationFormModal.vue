<template>
  <div id="qualificationFormModal" ref="modalRef" aria-hidden="true"
       aria-labelledby="qualificationModalLabel" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content glass-modal">
        <div class="modal-header">
          <h5 id="qualificationModalLabel" class="modal-title">
            {{ isEditing ? 'Edit Qualification' : 'Add New Qualification' }}</h5>
          <button aria-label="Close" class="btn-close" data-bs-dismiss="modal"
                  type="button"></button>
        </div>
        <div class="modal-body" style="max-height: 70vh; overflow-y: auto;">
          <form @submit.prevent="submitForm">
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label" for="qualName">Qualification / Degree</label>
                <input id="qualName" v-model="formState.qualificationName"
                       class="form-control"
                       required type="text">
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label" for="qualLevel">Level</label>
                <select id="qualLevel" v-model="formState.level" class="form-select"
                        required>
                  <option disabled value="">Select a level</option>
                  <option v-for="level in QUALIFICATION_LEVELS" :key="level.value"
                          :value="level.value">{{ level.text }}
                  </option>
                </select>
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label" for="qualInstitution">Institution</label>
              <input id="qualInstitution" v-model="formState.institutionName"
                     class="form-control"
                     required type="text">
            </div>
            <div class="mb-3">
              <label class="form-label" for="qualFieldOfStudy">Field of Study (Optional)</label>
              <input id="qualFieldOfStudy" v-model="formState.fieldOfStudy"
                     class="form-control"
                     type="text">
            </div>
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label" for="qualStartYear">Start Year</label>
                <input id="qualStartYear" v-model.number="formState.startYear"
                       :max="new Date().getFullYear() + 5"
                       :min="1900" class="form-control" required
                       type="number">
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label" for="qualCompletionYear">Completion Year</label>
                <input id="qualCompletionYear"
                       v-model.number="formState.completionYear"
                       :disabled="formState.stillStudying"
                       :max="new Date().getFullYear() + 10"
                       :min="1900" class="form-control"
                       type="number">
              </div>
            </div>
            <div class="form-check form-switch mb-3">
              <input id="qualStillStudying" v-model="formState.stillStudying"
                     class="form-check-input"
                     role="switch" type="checkbox">
              <label class="form-check-label" for="qualStillStudying">I am still studying for
                this qualification</label>
            </div>
            <div class="mb-3">
              <label class="form-label" for="qualGrade">Grade / Result (Optional)</label>
              <input id="qualGrade" v-model="formState.grade" class="form-control"
                     type="text">
            </div>
            <hr class="my-4">
            <h6 class="text-muted mb-3">Optional Links</h6>
            <div class="mb-3">
              <label class="form-label" for="qualInstitutionLogoUrl">Institution Logo
                URL</label>
              <input id="qualInstitutionLogoUrl"
                     v-model="formState.institutionLogoUrl" class="form-control"
                     placeholder="https://..."
                     type="url">
            </div>
            <div class="mb-3">
              <label class="form-label" for="qualInstitutionWebsite">Institution Website
                URL</label>
              <input id="qualInstitutionWebsite"
                     v-model="formState.institutionWebsite" class="form-control"
                     placeholder="https://..."
                     type="url">
            </div>
            <div class="mb-3">
              <label class="form-label" for="qualCredentialUrl">Credential URL</label>
              <input id="qualCredentialUrl" v-model="formState.credentialUrl"
                     class="form-control"
                     placeholder="https://..." type="url">
              <div class="form-text">A link to a digital certificate or verification page.</div>
            </div>
            <hr class="my-4">
            <div class="form-check form-switch mb-3">
              <input id="qualVisible" v-model="formState.visible"
                     class="form-check-input" role="switch"
                     type="checkbox">
              <label class="form-check-label" for="qualVisible">Visible on public
                portfolio</label>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" data-bs-dismiss="modal" type="button">Close</button>
          <button class="btn btn-primary" type="button" @click="submitForm">
            {{ isEditing ? 'Save Changes' : 'Add Qualification' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch, onUnmounted } from 'vue';
import { Modal } from 'bootstrap';
import type { Qualification } from '@/types';

const props = defineProps({
  qualification: { type: Object as () => Qualification | null, default: null },
  isEditing: { type: Boolean, default: false },
});

const emit = defineEmits(['save']);

const QUALIFICATION_LEVELS = [
  { value: 'DOCTORATE', text: 'Doctorate (PhD)' },
  { value: 'MASTERS', text: 'Master\'s Degree' },
  { value: 'POSTGRADUATE_DIPLOMA', text: 'Postgraduate Diploma' },
  { value: 'BACHELORS', text: 'Bachelor\'s Degree' },
  { value: 'ASSOCIATE_DEGREE', text: 'Associate Degree' },
  { value: 'DIPLOMA', text: 'Diploma' },
  { value: 'CERTIFICATE', text: 'Certificate' },
  { value: 'HIGH_SCHOOL', text: 'High School / Secondary' },
  { value: 'OTHER', text: 'Other' },
];

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;

// Form state needs to be reactive and match the input types (mostly strings)
// We provide defaults for optional fields to avoid undefined in inputs
const getInitialState = () => ({
  uuid: null as string | null,
  qualificationName: '',
  institutionName: '',
  institutionLogoUrl: '',
  institutionWebsite: '',
  fieldOfStudy: '',
  level: '',
  startYear: new Date().getFullYear() - 4,
  completionYear: new Date().getFullYear() as number | null,
  stillStudying: false,
  grade: '',
  credentialUrl: '',
  visible: true,
});

const formState = reactive(getInitialState());

watch(() => props.qualification, (newVal) => {
  if (newVal) {
    // Map from potentially sparse central type to our concrete form state
    Object.assign(formState, {
      uuid: newVal.uuid || null,
      qualificationName: newVal.qualificationName || '',
      institutionName: newVal.institutionName || '',
      institutionLogoUrl: newVal.institutionLogoUrl || '',
      institutionWebsite: newVal.institutionWebsite || '',
      fieldOfStudy: newVal.fieldOfStudy || '',
      level: newVal.level || '',
      startYear: newVal.startYear || new Date().getFullYear() - 4,
      completionYear: newVal.completionYear ?? null,
      stillStudying: !!newVal.stillStudying,
      grade: newVal.grade || '',
      credentialUrl: newVal.credentialUrl || '',
      visible: newVal.visible ?? true,
    });
  } else {
    Object.assign(formState, getInitialState());
  }
}, { deep: true, immediate: true });

watch(() => formState.stillStudying, (isStudying) => {
  if (isStudying) {
    formState.completionYear = null;
  } else if (formState.completionYear === null) {
    formState.completionYear = new Date().getFullYear();
  }
});

// --- Modal History & Show Logic ---
const show = () => {
    // Push state so back button closes modal
    history.pushState({ modal: 'qualification' }, '', '#modal-open');
    modalInstance?.show();
};

const hide = () => {
    modalInstance?.hide();
};

const submitForm = () => {
  emit('save', {...formState});
};

// Listen for popstate
// Listen for popstate to close modal on back button
const handlePopState = (event: PopStateEvent) => {
  console.log('[QualificationModal] Detected popstate. Event state:', event.state);
  try {
    if (modalInstance) {
      console.log('[QualificationModal] Calling modalInstance.hide()...');
      modalInstance.hide();
      console.log('[QualificationModal] modalInstance.hide() call successful.');
    } else {
      console.warn('[QualificationModal] No modalInstance found during popstate!');
      // Attempt recovery if modalRef exists but instance is null
      if (modalRef.value) {
        console.log('[QualificationModal] Attempting to recover modalInstance...');
        modalInstance = new Modal(modalRef.value);
        // Try hiding again after recovery
        if (modalInstance) {
          console.log('[QualificationModal] Recovered modalInstance, attempting hide again.');
          modalInstance.hide();
          console.log('[QualificationModal] Recovered modalInstance.hide() call successful.');
        }
      }
    }
  } catch (e) {
    console.error('[QualificationModal] Failed to call modalInstance.hide():', e);
  }

  console.log('[QualificationModal] Scheduling safety fallback cleanup (1000ms)...');
  // Safety fallback for stubborn mobile/headless transitions
  setTimeout(() => {
    const bodyHasModalOpen = document.body.classList.contains('modal-open');
    const modalHasShow = modalRef.value?.classList.contains('show');
    const backdrops = document.querySelectorAll('.modal-backdrop');

    console.log(`[QualificationModal] Cleanup check after 1s. body.modal-open: ${bodyHasModalOpen}, modal.show: ${modalHasShow}, backdrops: ${backdrops.length}`);

    if (bodyHasModalOpen || modalHasShow || backdrops.length > 0) {
      console.warn('[QualificationModal] Modal stuck visible after .hide(), forcing deep cleanup.');
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
    console.log('[QualificationModal] onMounted: attaching popstate listener.');
    if (modalRef.value) {
        modalInstance = new Modal(modalRef.value);
        // Clean up hash
        modalRef.value.addEventListener('hidden.bs.modal', () => {
             if (window.location.hash === '#modal-open') {
                 history.back();
             }
        });
    }
    window.addEventListener('popstate', handlePopState);
});


onUnmounted(() => {
    console.log('[QualificationModal] Component unmounting. Cleaning up listeners and DOM.');
    window.removeEventListener('popstate', handlePopState);

    // Emergency immediate cleanup in case Bootstrap linger
    if (document.body.classList.contains('modal-open')) {
        console.log('[QualificationModal] Modal remains in body on unmount, forcing cleanup.');
        document.body.classList.remove('modal-open');
        document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
    }
});

defineExpose({
    show,
    hide,
});
</script>
