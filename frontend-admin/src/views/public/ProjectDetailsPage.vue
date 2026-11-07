<template>
  <!-- The root element correctly uses the global animated background class -->
  <div class="project-details-page">
    <LoadingModal :visible="isLoading" class="glass-modal"/>

    <!-- Skeleton Loader with new glass styles -->
    <div v-if="isLoading">
      <div class="hero-image-container skeleton-line"></div>
      <div class="container content-container">
        <div class="card glass-card glass-card-floating p-4 p-md-5">
          <!-- ... skeleton content ... -->
        </div>
      </div>
    </div>

    <!-- Error state with glassmorphic styling -->
    <!-- Error state with glassmorphic styling -->
    <div v-else-if="error" class="container py-5">
      <div class="glass-card mx-auto p-5 text-center" style="max-width: 600px;">
        <AlertTriangle class="text-warning display-4 mb-3" />
        <h3 class="glass-title mb-3">Oops! Something went wrong</h3>
        <!-- FIX: Display the actual error message -->
        <p class="glass-description mb-4">{{ (error as { message?: string })?.message || 'Failed to load project details.' }}</p>
        <button class="btn glass-btn-primary" @click="retryLoad">
          <RefreshCw class="me-2" /> Try Again
        </button>
      </div>
    </div>

    <div v-else-if="project">
      <!-- Media Section (Video/Hero) -->
      <div class="hero-media-container animate-fade-in">
        <div v-if="mediaType === 'youtube' || mediaType === 'vimeo'" class="ratio ratio-16x9 media-embed-wrapper w-100">
          <iframe
            :src="embedUrl"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowfullscreen
            loading="lazy"
            class="glass-iframe"
            title="Project Video"
          ></iframe>
        </div>
        <div v-else-if="mediaType === 'video'" class="ratio ratio-16x9 media-embed-wrapper">
          <video :src="project.mediaUrl" controls autoplay muted class="glass-video"></video>
        </div>
        <div v-else-if="mediaType === 'image'" class="media-embed-wrapper" style="height: 50vh;">
          <img :src="project.mediaUrl" alt="Project Media" class="glass-iframe" style="object-fit: contain;">
        </div>
        <div v-else :style="{ backgroundImage: `url(${project.imageUrl || ''})` }" class="hero-image-bg">
          <div class="hero-overlay"></div>
        </div>
      </div>

      <div class="container content-container">
        <!-- Main Content Card -->
        <div
          class="card glass-card glass-card-floating p-4 p-md-5 animate-fade-in-up interactive-card-lift interactive-card-shadow-primary">
          <!-- Header -->
          <div class="text-center mb-4">
            <h1 class="display-4 fw-bold glass-title">{{ project.title }}</h1>
          </div>

          <!-- Tech Stack -->
          <div v-if="project.skills && project.skills.length"
               class="text-center mb-4 d-flex flex-wrap justify-content-center gap-2">
            <!-- FIX: Replaced with the new SkillBadge component -->
            <!-- FIX: The key should be `skill.skillId` for consistency with other pages
                 like UserProjectsPage and ExperiencePage, which use the same data structure. -->
            <!-- FIX: Explicitly cast to Skill[] to resolve TypeScript lint errors -->
            <SkillBadge
              v-for="skill in (project.skills as Skill[])"
              :key="skill.skillId"
              :skill="skill"
              class="tech-badge"
            />
          </div>

          <!-- Description -->
          <div class="project-description mb-5">
            <p class="glass-description">{{ project.description }}</p>

            <!-- Phase 7.4 §6: structured key features (fallback = description only) -->
            <ul v-if="project.keyFeatures && project.keyFeatures.length"
                class="key-features-list glass-description text-start mx-auto">
              <li v-for="(feature, fi) in project.keyFeatures" :key="'kf-' + fi">
                {{ feature }}
              </li>
            </ul>
          </div>

          <!-- Links Section -->
          <div class="d-flex flex-wrap justify-content-center gap-3 mb-5">
            <a v-if="project.liveUrl" :href="project.liveUrl"
               class="btn glass-btn-primary btn-lg interactive-lift"
               target="_blank">
              <ExternalLink class="me-2" /> Live Demo
            </a>
            <a v-if="project.repoUrl" :href="project.repoUrl"
               class="btn glass-btn btn-lg interactive-lift"
               target="_blank">
              <Github class="me-2" /> Source Code
            </a>
            <!-- LinkedIn Share Button -->
            <button class="btn glass-btn btn-lg interactive-lift" @click="shareOnLinkedIn">
              <Linkedin class="me-2" /> Share on LinkedIn
            </button>
            <!-- Generic Share Button -->
            <button class="btn glass-btn btn-lg interactive-lift" @click="copyToClipboard">
              <Share2 class="me-2" /> {{ shareText }}
            </button>
          </div>

          <div class="text-center">
            <router-link :to="{ name: 'projects-public', params: { slug: currentSlug } }"
                         class="btn btn-link glass-subtitle">
              <ArrowLeft class="me-1" /> Back to All Projects
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- Not Found state with glassmorphic styling -->
    <div v-else-if="!isLoading" class="container py-5">
      <!-- ... not found content ... -->
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {ApiError} from '@/services/api/index.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
// FIX: Import the new SkillBadge component
import SkillBadge from '@/components/common/SkillBadge.vue';
import type { Project, Portfolio, Skill } from '@/types';
import { AlertTriangle, RefreshCw, ExternalLink, Github, Linkedin, Share2, ArrowLeft } from 'lucide-vue-next';

const props = defineProps({
  uuid: {
    type: String,
    required: true
  }
});

const project = ref<Project | null>(null);
const isLoading = ref(true);
const error = ref<ApiError | { message: string } | null>(null);

const {
  portfolio,
  currentSlug
} = usePublicPortfolioStore() as {
  portfolio: import('vue').Ref<Portfolio | null>,
  currentSlug: import('vue').Ref<string | null>
};

// --- Rich Media Logic ---
import { getMediaType, getEmbedUrl } from '@/utils/mediaUtils';
import { computed } from 'vue';

const mediaType = computed(() => getMediaType(project.value?.mediaUrl || ''));

const embedUrl = computed(() => {
  return getEmbedUrl(project.value?.mediaUrl || '', true);
});

// All tooltip logic has been removed from here and is now in SkillBadge.vue

const loadProject = async () => {
  isLoading.value = true;
  error.value = null;

  if (portfolio.value) {

    const foundProject = portfolio.value.projects?.find((p: Project) => p.uuid === props.uuid);
    if (foundProject) {
      project.value = foundProject;
    } else {
      error.value = {message: `Project with ID ${props.uuid} not found in this portfolio.`};
    }
    isLoading.value = false;
    return;
  }

  // Fallback logic remains the same
  console.warn('[ProjectDetails] Portfolio not in store. This should be handled by the router guard. Falling back to direct API call.');
  try {
    const {getPublicProjectById} = await import('@/services/api/index.js');
    project.value = (await getPublicProjectById(props.uuid)) as Project;
  } catch (err) {
    console.error(`Failed to fetch project with UUID ${props.uuid}:`, err);
    error.value = err instanceof ApiError ? err : {message: 'An unexpected error occurred.'};
  } finally {
    isLoading.value = false;
  }
};

const shareText = ref('Share Project');

const copyToClipboard = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href);
    shareText.value = 'Copied!';
    setTimeout(() => {
      shareText.value = 'Share Project';
    }, 2000);
  } catch (err) {
    console.error('Failed to copy: ', err);
  }
};

const shareOnLinkedIn = () => {
  const url = encodeURIComponent(window.location.href);
  window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${url}`, '_blank');
};

const retryLoad = () => {
  loadProject();
};

onMounted(loadProject);
</script>

<style scoped>
/* --- Local styles remain the same, but badge styles are now in the component --- */
.project-details-page {
  overflow-x: hidden;
  padding-bottom: 5rem;
}

.hero-media-container {
  min-height: 50vh;
  position: relative;
  background-color: var(--bs-black); /* Fallback for videos */
  overflow: hidden;
}

.hero-image-bg {
  height: 50vh;
  background-size: cover;
  background-position: center;
  position: relative;
}

.media-embed-wrapper {
  max-height: 70vh; /* Don't take up too much vertical space */
  margin: 0 auto;
}

.glass-iframe, .glass-video {
  width: 100%;
  height: 100%;
  border: none;
  border-bottom: 1px solid rgba(var(--bs-body-color-rgb), 0.1);
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to top, var(--bs-body-bg) 5%, rgba(var(--bs-body-bg-rgb), 0.5) 100%);
}

.content-container {
  margin-top: -15vh;
  position: relative;
  z-index: 2;
}

.project-description {
  font-size: 1.1rem;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* --- Phase 7.4 §6: structured key features --- */
.key-features-list {
  list-style: none;
  padding-left: 1.5rem;
  max-width: 720px;
}

.key-features-list li {
  position: relative;
  margin-bottom: 0.35rem;
}

.key-features-list li::before {
  content: '–';
  position: absolute;
  left: -1.2rem;
  color: var(--bs-primary);
}

.animate-fade-in {
  animation: fadeIn 1s ease-out forwards;
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out 0.2s forwards;
}

/* Overriding the component's margin for this specific layout */
.tech-badge {
  margin-right: 0.5rem;
  margin-bottom: 0.5rem;
}
</style>
