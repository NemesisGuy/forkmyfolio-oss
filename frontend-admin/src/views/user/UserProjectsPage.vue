<template>
  <div class="user-projects-page py-5 flex-grow-1">
    <div class="container">
      <!-- Header with Add Button -->
      <div class="d-flex justify-content-between align-items-center mb-4 animate-fade-in-up">
        <h2 class="mb-0 glass-text">My Projects</h2>
        <button class="btn btn-primary interactive-lift" @click="openAddModal">
          <PlusCircle class="me-2"/>Add Project
        </button>
      </div>

      <!-- Modals -->
      <LoadingModal :visible="isLoading"/>
      <ErrorModal :message="error || ''" :visible="!!error" title="An Error Occurred"
                  @close="error = null"/>
      <SuccessModal :message="successMessage || ''" :visible="!!successMessage" title="Success"
                    @close="successMessage = null"/>
      <ProjectFormModal
        ref="projectFormModalRef"
        :project="currentProject"
        :user-skills="userSkills"
        @save="handleSaveProject"
      />
      <ConfirmModal
        ref="confirmModalRef"
        message="Are you sure you want to delete this project? This action cannot be undone."
        title="Confirm Deletion"
        type="danger"
        @close="projectToDelete = null"
        @confirm="handleDeleteProject"
      />

      <!-- Projects Grid -->
      <div v-if="!isLoading && projects.length > 0" class="row g-4">
        <div v-for="(project, index) in projects" :key="project.uuid"
             :style="{ 'animation-delay': (index * 0.05) + 's' }"
             class="col-md-6 col-lg-4 animate-fade-in-up">
          <div class="card h-100 glass-card interactive-lift">
            <!-- Project Image -->
            <div class="card-img-container">
              <img v-if="project.imageUrl" :alt="project.title" :src="project.imageUrl"
                   class="card-img-top">
              <div v-else class="card-img-top project-image-placeholder">
                <Code/>
              </div>
            </div>
            <!-- Project Details -->
            <div class="card-body d-flex flex-column">
              <h5 class="card-title glass-text">{{ project.title }}</h5>
              <p class="card-text glass-description flex-grow-1">
                {{ project.description || 'No description provided.' }}</p>
              <!-- Tech Stack & Visibility -->
              <div class="mt-auto pt-3 d-flex flex-column">
                <!-- REFACTOR: Use the SkillBadge component for consistency and maintainability. -->
                <div v-if="project.skills && project.skills.length"
                     class="mb-2 d-flex flex-wrap gap-2">
                 <SkillBadge v-for="skill in project.skills" :key="(skill as Skill).skillId" :skill="(skill as Skill)"/>
                </div>
                <div class="d-flex align-items-center justify-content-between mt-2">
                  <span :class="['badge', (project.visible ?? false) ? 'bg-success' : 'bg-secondary']">
                    {{ (project.visible ?? false) ? 'Visible' : 'Hidden' }}
                  </span>
                  <VisibilityToggle :is-loading="project.isVisibilityLoading" :visible="project.visible ?? false"
                                    @toggle="handleVisibilityToggle(project)"/>
                </div>
              </div>
            </div>
            <!-- Actions & Links -->
            <div class="card-footer d-flex justify-content-between align-items-center">
              <div>
                <a v-if="project.liveUrl" :href="project.liveUrl"
                   class="btn btn-sm btn-outline-secondary me-1"
                   target="_blank" title="Live Demo">
                  <ExternalLink/>
                </a>
                <a v-if="project.repoUrl" :href="project.repoUrl"
                   class="btn btn-sm btn-outline-secondary"
                   target="_blank" title="Source Code">
                  <Github/>
                </a>
              </div>
              <div>
                <button class="btn btn-sm btn-outline-primary me-1" title="Edit Project"
                        @click="openEditModal(project)">
                  <Pencil/>
                </button>
                <button class="btn btn-sm btn-outline-danger" title="Delete Project"
                        @click="openDeleteConfirm(project)">
                  <Trash2/>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!isLoading" class="text-center p-5 glass-card animate-fade-in-up">
        <div class="empty-state-icon mb-4"><KanbanSquare/></div>
        <h4 class="glass-title">No Projects Yet</h4>
        <p class="glass-subtitle">Click the "Add Project" button to get started.</p>
        <button class="btn btn-primary mt-3 interactive-lift" @click="openAddModal">Add Your First
          Project
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import { PlusCircle, Code, ExternalLink, Github, Pencil, Trash2, KanbanSquare } from 'lucide-vue-next';
import {projectsApi, skillsApi} from '@/services/api/user.api.js';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {authService} from '@/services/authService.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';
import ProjectFormModal from '@/components/user/ProjectFormModal.vue';
// REFACTOR: Import the reusable SkillBadge component for consistency.
import VisibilityToggle from '@/components/common/VisibilityToggle.vue';
import SkillBadge from '@/components/common/SkillBadge.vue';
import type { Project, Skill, User } from '@/types';

interface ProjectWithLoading extends Project {
  isVisibilityLoading?: boolean;
}

// --- Component State ---
const projects = ref<ProjectWithLoading[]>([]);
const userSkills = ref<Skill[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);

const projectFormModalRef = ref<InstanceType<typeof ProjectFormModal> | null>(null);
const confirmModalRef = ref<InstanceType<typeof ConfirmModal> | null>(null);
const currentProject = ref<Project | undefined>(undefined); // For editing or adding
const projectToDelete = ref<ProjectWithLoading | null>(null); // For deletion confirmation

// --- Store and Services ---
const portfolioStore = usePublicPortfolioStore();

// --- Lifecycle Hooks ---
onMounted(async () => {
  await Promise.all([
    fetchProjects(),
    fetchUserSkills(),
  ]);
});

// --- API Functions ---
const fetchProjects = async () => {
  try {
    isLoading.value = true;
    const fetchedProjects = await projectsApi.getAll() as Project[];
    projects.value = fetchedProjects
      .map(p => ({...p, isVisibilityLoading: false} as ProjectWithLoading)) // Add loading state for the toggle
      .sort((a, b) => (a.displayOrder ?? 999) - (b.displayOrder ?? 999));
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'An unexpected error occurred while fetching projects.';
    console.error("Failed to fetch user projects:", err);
    error.value = message;
  } finally {
    isLoading.value = false;
  }
};

const fetchUserSkills = async () => {
  try {
    userSkills.value = await skillsApi.getAll() as Skill[];
  } catch (err: unknown) {
    console.error("Failed to fetch user skills for project form:", err);
    // Non-critical error, so we don't block the page from loading.
    // The form will still work, just without skill suggestions.
  }
};

/**
 * Forces a refresh of the public portfolio data in the store.
 * This ensures that any changes made here (add, edit, delete) are
 * immediately reflected on the public-facing pages.
 */
const refreshPublicData = async () => {
  const userSlug = (authService.user.value as User | null)?.slug;
  if (userSlug) {
    // The 'true' forces a refetch, bypassing the cache.
    await portfolioStore.fetchPortfolio(userSlug, true);
  }
};

// --- Modal Handling ---
const openAddModal = () => {
  currentProject.value = undefined; // Clear for a new project
  projectFormModalRef.value?.show();
};

const openEditModal = (project: Project) => {
  // Pass a deep copy to the modal to prevent direct mutation of the list item
  currentProject.value = JSON.parse(JSON.stringify(project)) as Project;
  projectFormModalRef.value?.show();
};

const openDeleteConfirm = (project: ProjectWithLoading) => {
  projectToDelete.value = project;
  confirmModalRef.value?.show();
};

// --- CRUD Operations ---

/**
 * Creates a clean payload object suitable for the project API endpoints.
 * It strips out any frontend-only state (like `isVisibilityLoading`) and
 * transforms the `skills` array from objects to a simple array of names.
 * @param {object} project - The project object from the component's state.
 * @returns {object} A clean data transfer object for the API.
 */
const buildProjectPayload = (project: Project) => {
  const payload: Partial<Project> = {
    // All fields from the ProjectDto are included
    ...project,
  };

  // FIX: The skills can be an array of objects (from the main list) or an array of strings (from the form).
  // The API always expects an array of strings. This logic handles both cases.
  if (Array.isArray(project.skills) && project.skills.length > 0) {
    // If the first item is an object with a 'name' property, map to names.
    const firstSkill = project.skills[0];
    if (typeof firstSkill === 'object' && firstSkill !== null && 'name' in firstSkill) {
      payload.skills = (project.skills as Skill[]).map(skill => skill.name);
    } // Otherwise, it's already an array of strings from the form, so we do nothing.
  } else {
    payload.skills = [];
  }

  // Remove any frontend-only state properties before sending to the backend.
  delete (payload as { isVisibilityLoading?: boolean }).isVisibilityLoading;
  return payload;
};

const handleSaveProject = async (projectData: Project) => {
  isLoading.value = true;
  projectFormModalRef.value?.hide();
  try {
    // REFACTOR: Always build the payload in the parent component for consistency.
    // This ensures the data is in the correct format for the API.
    const payload = buildProjectPayload(projectData);
    if (payload.uuid) {
      // Update existing project
      await projectsApi.update(payload.uuid, payload);
      successMessage.value = 'Project updated successfully!';
    } else {
      // Create new project
      await projectsApi.create(payload as Project);
      successMessage.value = 'Project created successfully!';
    }
    await fetchProjects(); // Refresh the list in the management view
    await refreshPublicData(); // Refresh the public data store
  } catch (err: unknown) {
    console.error("Failed to save project:", err);
    error.value = (err as Error).message || 'An error occurred while saving the project.';
  } finally {
    isLoading.value = false;
  }
};

const handleDeleteProject = async () => {
  if (!projectToDelete.value) return;

  // FIX: Capture the project object before closing the modal.
  // The closeConfirmModal() function sets projectToDelete to null, which was
  // causing the subsequent API call to fail because it couldn't find the UUID.
  const projectToDeleteRef = projectToDelete.value;
  isLoading.value = true;
  confirmModalRef.value?.hide();

  try {
    // Use the captured reference for the API call.
    if (!projectToDeleteRef.uuid) return;
    await projectsApi.remove(projectToDeleteRef.uuid);
    successMessage.value = 'Project deleted successfully.';
    // Optimistically remove from the local array for a faster UI response
    projects.value = projects.value.filter(p => p.uuid !== projectToDeleteRef.uuid);
    await refreshPublicData(); // Refresh the public data store
  } catch (err: unknown) {
    console.error("Failed to delete project:", err);
    error.value = (err as Error).message || 'An error occurred while deleting the project.';
  } finally {
    isLoading.value = false;
  }
};

const handleVisibilityToggle = async (project: ProjectWithLoading) => {
  project.isVisibilityLoading = true;
  const originalVisibility = project.visible;
  project.visible = !project.visible; // Optimistic update

  try {
    // FIX: Use the helper function to build a clean payload for the API.
    const payload = buildProjectPayload(project);
    if (!project.uuid) return;
    await projectsApi.update(project.uuid, payload);
    successMessage.value = `Visibility for '${project.title}' updated.`;
    await refreshPublicData();
  } catch (err: unknown) {
    project.visible = originalVisibility ?? false; // Revert on error
    error.value = (err as Error).message || 'Failed to update visibility.';
  } finally {
    project.isVisibilityLoading = false;
  }
};
</script>

<style scoped>
.empty-state-icon {
  font-size: 4rem;
  color: var(--glass-text);
}

.card-img-container {
  height: 180px;
  overflow: hidden;
  position: relative;
  border-top-left-radius: var(--bs-card-inner-border-radius);
  border-top-right-radius: var(--bs-card-inner-border-radius);
  background-color: rgba(var(--bs-body-color-rgb), 0.05);
}

.card-img-top {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.project-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--glass-text-secondary);
  font-size: 3rem;
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive styles for mobile-first design */
@media (max-width: 576px) {
  .user-projects-page {
    padding: 1rem 0;
  }

  .user-projects-page .container {
    padding: 0 1rem;
  }

  .user-projects-page .d-flex {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .user-projects-page h2 {
    font-size: 1.5rem;
    text-align: center;
  }

  .card-img-container {
    height: 120px;
  }

  .project-image-placeholder {
    font-size: 2rem;
  }

  .card-title {
    font-size: 1.1rem;
  }

  .card-text {
    font-size: 0.9rem;
  }

  .btn {
    min-height: 44px;
    font-size: 0.9rem;
  }
}

/* Tablet enhancements */
@media (min-width: 768px) {
  .card-img-container {
    height: 150px;
  }
}

/* Desktop enhancements */
@media (min-width: 1024px) {
  .user-projects-page .container {
    max-width: 1200px;
  }
}
</style>
