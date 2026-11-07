<script setup lang="ts">
/**
 * @file app/pages/[slug]/project/[uuid].vue
 * @description Public project details page.
 * Ported from ForkMyFolio-frontend-vue/src/views/public/ProjectDetailsPage.vue.
 *
 * The Vue original resolves the project from the portfolio store (fetched by the
 * router guard) and falls back to a direct `GET /projects/{uuid}` API call. Here
 * the same two-step strategy runs inside one useAsyncData for SSR.
 */
import LoadingModal from '~/components/modals/LoadingModal.vue';

const route = useRoute();
const slug = route.params.slug as string;
// DEF-1: public analytics tracking (non-credential config; fails soft).
useAnalyticsTracking();
const uuid = route.params.uuid as string;

const { fetchJson } = useApi();

// --- Data (portfolio-first, single-project fallback — mirrors loadProject()) ---
// SEO v3.1.2: the handler now returns the profile/user alongside the project
// (from the SAME portfolio request it already made) so page metadata can be
// derived without any extra backend call. The fallback path (direct project
// fetch) has no profile — metadata falls back gracefully via the composable.
// FIX: the error ref is now named `error` (was `loadError`) — the template's
// error branch referenced `error`, which never existed, so the friendly error
// UI could not render.
const { data: result, pending, error, refresh } = await useAsyncData(
  `project-${slug}-${uuid}`,
  async () => {
    try {
      const portfolioData = await fetchJson<any>(`/portfolios/${slug}`);
      const found = portfolioData?.projects?.find((p: any) => p.uuid === uuid);
      if (found) {
        return {
          project: found,
          profile: portfolioData?.profile || null,
          user: portfolioData?.user || null,
        };
      }
      // Found portfolio but not the project — same message as the original.
      throw new Error(`Project with ID ${uuid} not found in this portfolio.`);
    } catch (e: any) {
      // Fallback logic remains the same as the Vue original.
      console.warn('[ProjectDetails] Falling back to direct API call.');
      try {
        const p = await fetchJson<any>(`/projects/${uuid}`);
        return { project: p, profile: null, user: null };
      } catch (fallbackErr: any) {
        console.error(`Failed to fetch project with UUID ${uuid}:`, fallbackErr);
        throw new Error(e?.message || 'An unexpected error occurred.');
      }
    }
  }
);

const project = computed(() => result.value?.project ?? null);

// SEO v3.1.2: project-specific metadata (TDZ — must stay below the reactive
// declarations above; see usePortfolioSeo).
usePortfolioSeo({
  slug,
  section: 'project-details',
  portfolio: () => (result.value ? { profile: result.value.profile, user: result.value.user } : null),
  item: () => (project.value
    ? {
        title: project.value.title,
        description: project.value.description,
        image: project.value.imageUrl,
        mediaUrl: project.value.mediaUrl,
      }
    : null),
  httpErrorStatus: () => (error.value ? 404 : null),
});

// --- Rich Media Logic ---
const mediaType = computed(() => getMediaType(project.value?.mediaUrl || ''));
const embedUrl = computed(() => getEmbedUrl(project.value?.mediaUrl || '', true));

// --- Share logic ---
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
  refresh();
};
</script>

<template>
  <!-- The root element correctly uses the global animated background class -->
  <div class="project-details-page">
    <LoadingModal :visible="pending" class="glass-modal"/>

    <!-- Skeleton Loader with new glass styles -->
    <div v-if="pending">
      <div class="hero-image-container skeleton-line"></div>
      <div class="container content-container">
        <div class="card glass-card glass-card-floating p-4 p-md-5">
          <!-- ... skeleton content ... -->
        </div>
      </div>
    </div>

    <!-- Error state with glassmorphic styling -->
    <div v-else-if="error" class="container py-5">
      <div class="glass-card mx-auto p-5 text-center" style="max-width: 600px;">
        <AlertTriangle class="text-warning display-4 mb-3" />
        <h3 class="glass-title mb-3">Oops! Something went wrong</h3>
        <p class="glass-description mb-4">{{ (error as any)?.message || 'Failed to load project details.' }}</p>
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
            <SkillBadge
              v-for="skill in project.skills"
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
            <NuxtLink :to="`/${slug}/projects`" class="btn btn-link glass-subtitle">
              <ArrowLeft class="me-1" /> Back to All Projects
            </NuxtLink>
          </div>
        </div>
      </div>
    </div>

    <!-- Not Found state with glassmorphic styling -->
    <div v-else-if="!pending" class="container py-5">
      <!-- ... not found content ... -->
    </div>
  </div>
</template>

<script lang="ts">
/**
 * Icons and components ported from ProjectDetailsPage.vue (lucide-vue-next).
 */
import SkillBadge from '~/components/SkillBadge.vue';
import { AlertTriangle, RefreshCw, ExternalLink, Github, Linkedin, Share2, ArrowLeft } from 'lucide-vue-next';

export default {
  components: {
    SkillBadge,
    AlertTriangle,
    RefreshCw,
    ExternalLink,
    Github,
    Linkedin,
    Share2,
    ArrowLeft,
  },
};
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
