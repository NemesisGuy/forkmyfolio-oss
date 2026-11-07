<template>
  <div id="projectFormModal" ref="modalRef" aria-hidden="true" aria-labelledby="projectFormModalLabel" class="modal fade"
       tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content glass-modal">
        <div class="modal-header">
          <h5 id="projectFormModalLabel" class="modal-title">
            {{ isEditing ? 'Edit Project' : 'Add New Project' }}</h5>
          <button aria-label="Close" class="btn-close" data-bs-dismiss="modal" type="button"></button>
        </div>
        <div class="modal-body" style="max-height: 75vh; overflow-y: auto;">
          <form class="row g-3" @submit.prevent="submitForm">

            <!-- Project Title -->
            <div class="col-12">
              <label class="form-label" for="projectTitle">Project Title</label>
              <input id="projectTitle" v-model="formState.title" class="form-control" required
                     type="text">
            </div>

            <!-- Project Description -->
            <div class="col-12">
              <label class="form-label" for="projectDescription">Description</label>
              <textarea id="projectDescription" v-model="formState.description" class="form-control"
                        rows="4"></textarea>
            </div>

            <!-- URLs -->
            <div class="col-md-6">
              <label class="form-label" for="projectImageUrl">Image URL</label>
              <input id="projectImageUrl" v-model="formState.imageUrl" class="form-control"
                     placeholder="https://..." type="url">
            </div>
            <div class="col-md-12">
              <label class="form-label" for="projectMediaUrl">Media URL (Video/YouTube/Vimeo)</label>
              <input id="projectMediaUrl" v-model="formState.mediaUrl" class="form-control"
                     placeholder="https://www.youtube.com/watch?v=..."
                     type="url">
            </div>
            <div class="col-md-6">
              <label class="form-label" for="projectLiveUrl">Live Demo URL</label>
              <input id="projectLiveUrl" v-model="formState.liveUrl" class="form-control"
                     placeholder="https://..."
                     type="url">
            </div>
            <div class="col-md-6">
              <label class="form-label" for="projectRepoUrl">Repository URL</label>
              <input id="projectRepoUrl" v-model="formState.repoUrl" class="form-control"
                     placeholder="https://github.com/..."
                     type="url">
            </div>

            <!-- Key Features (Phase 7.4 §6): ordered structured bullets -->
            <div class="col-12">
              <hr class="my-3">
              <label class="form-label">Key Features</label>
              <div class="small text-muted mb-2">Short bullet points shown next to the project (max 12). The list order
                is the display order.</div>
              <div v-for="(feature, index) in formState.keyFeatures" :key="'kf-' + index"
                   class="d-flex align-items-center gap-1 mb-2">
                <input v-model="formState.keyFeatures[index]" class="form-control form-control-sm"
                       :aria-label="`Key feature ${index + 1}`" maxlength="255" type="text">
                <button class="btn btn-sm btn-outline-secondary" type="button" title="Move up"
                        :disabled="index === 0" @click="moveFeature(index, -1)">&#8593;</button>
                <button class="btn btn-sm btn-outline-secondary" type="button" title="Move down"
                        :disabled="index === formState.keyFeatures.length - 1"
                        @click="moveFeature(index, 1)">&#8595;</button>
                <button class="btn btn-sm btn-outline-danger" type="button" title="Remove"
                        @click="removeFeature(index)">&times;</button>
              </div>
              <div v-if="formState.keyFeatures.length === 0" class="text-muted small mb-2">
                No key features yet — the public pages will use the description, as before.
              </div>
              <button class="btn btn-sm btn-outline-primary" type="button" :disabled="formState.keyFeatures.length >= 12"
                      @click="addFeature">+ Add Key Feature</button>
            </div>

            <!-- Display Order -->
            <div class="col-md-6">
              <label class="form-label" for="projectDisplayOrder">Display Order</label>
              <input id="projectDisplayOrder" v-model.number="formState.displayOrder"
                     class="form-control"
                     type="number">
            </div>

            <!-- REFACTOR: Use the standardized SkillTagInput component -->
            <div class="col-12">
              <hr class="my-3">
              <label class="form-label">Skills</label>
              <SkillTagInput v-model="formState.skills" :available-skills="userSkills"/>
            </div>

            <!-- Visibility Toggle -->
            <div class="col-12">
              <hr class="my-3">
              <div class="form-check form-switch">
                <input id="projectVisible" v-model="formState.visible" class="form-check-input"
                       role="switch"
                       type="checkbox">
                <label class="form-check-label" for="projectVisible">Visible on public
                  portfolio</label>
              </div>
            </div>

          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" data-bs-dismiss="modal" type="button">Close</button>
          <button class="btn btn-primary" data-bs-dismiss="modal" type="button" @click="submitForm">
            {{ isEditing ? 'Save Changes' : 'Add Project' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref, watch} from 'vue';
import {Modal} from 'bootstrap';
// REFACTOR: Import the reusable SkillTagInput component.
import SkillTagInput from '@/components/user/SkillTagInput.vue';
import type { Skill } from '@/types';

// --- Props and Emits ---
const props = defineProps({
  project: {
    type: Object,
    default: null,
  },
  userSkills: {
    type: Array,
    required: true,
  },
});
const emit = defineEmits(['save']);

// --- Component State ---
const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;
const isEditing = ref(false);

const getInitialFormState = () => ({
  uuid: null,
  title: '',
  description: '',
  repoUrl: '',
  liveUrl: '',
  imageUrl: '',
  mediaUrl: '',
  displayOrder: 100,
  skills: [] as string[], // Holds an array of skill NAMES
  // Phase 7.4 §6: structured key-features bullets (list order = display order)
  keyFeatures: [] as string[],
  visible: true,
});
const formState = reactive(getInitialFormState());

// --- Lifecycle and Watchers ---
onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
  }
});

watch(() => props.project, (newProject) => {
  if (newProject) {
    isEditing.value = true;
    Object.assign(formState, {
      ...newProject,
      // Ensure skills are an array of names, not objects
      skills: newProject.skills ? newProject.skills.map((s: Skill) => s.name) : [],
      // Phase 7.4 §6: plain string list (defensive copy, never share the prop's array)
      keyFeatures: Array.isArray(newProject.keyFeatures) ? [...newProject.keyFeatures] : [],
    });
  } else {
    isEditing.value = false;
    Object.assign(formState, getInitialFormState());
  }
}, {deep: true});

// --- Key feature list helpers (Phase 7.4 §6) ---
const moveFeature = (index: number, delta: number) => {
  const target = index + delta;
  if (target < 0 || target >= formState.keyFeatures.length) return;
  const features = formState.keyFeatures;
  [features[index], features[target]] = [features[target], features[index]];
};

const removeFeature = (index: number) => {
  formState.keyFeatures.splice(index, 1);
};

const addFeature = () => {
  if (formState.keyFeatures.length >= 12) return;
  formState.keyFeatures.push('');
};

const submitForm = () => {
  // Trim + drop blank entries (the API rejects blank items with 400)
  const cleanedFeatures = formState.keyFeatures
      .map((f) => f.trim())
      .filter((f) => f.length > 0);
  emit('save', {...formState, keyFeatures: cleanedFeatures});
};

defineExpose({
  show: () => modalInstance?.show(),
  hide: () => modalInstance?.hide(),
});
</script>

<style scoped>
/* All skill input styles are now encapsulated in the SkillTagInput component. */
</style>
