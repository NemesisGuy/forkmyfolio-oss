<script setup lang="ts">
const route = useRoute();
const slug = route.params.slug as string;
// DEF-1: public analytics tracking (non-credential config; fails soft).
useAnalyticsTracking();
const { fetchJson, baseUrl } = useApi();

// --- Data (SEO v3.1.2: sourced from the full public portfolio payload) ---
// Same single request the landing page uses (shared `portfolio-${slug}`
// useAsyncData key — SPA navigation between sections never refetches), and it
// carries the profile needed for rich metadata. The backend's PortfolioMapper
// already restricts this list to visible projects; the client-side sort below
// is preserved from the original port.
const { data: portfolio, pending, error } = await useAsyncData(
  `portfolio-${slug}`,
  () => fetchJson(`/portfolios/${slug}`)
);

// Sort by displayOrder ascending (lower number first).
const projects = computed(() => {
  const projs = ((portfolio.value?.projects as any[]) || []) as any[];
  return [...projs].sort(
    (a, b) => (a.displayOrder || 999) - (b.displayOrder || 999)
  );
});

// SEO v3.1.2: full metadata from the same payload (TDZ — must stay below the
// declarations above; see usePortfolioSeo).
usePortfolioSeo({
  slug,
  section: 'projects',
  portfolio: () => portfolio.value,
  dataLead: (name: string) => {
    const list = (projects.value || []).filter((p: any) => p.visible !== false);
    if (!list.length) return null;
    const first = list[0]?.title ? ` including ${list[0].title}` : '';
    return `${list.length} project${list.length === 1 ? '' : 's'} by ${name}${first}.`;
  },
  httpErrorStatus: () => apiErrorStatus(error.value?.message),
});

// --- Owner check (client-side replica of the Vue authService check) ---
// The SPA keeps its access token in localStorage (or sessionStorage for guests)
// and fetches GET /me to know the account slug. The Nuxt public app has no
// global auth store, so this page replicates just that small logic inline.
const isOwner = ref(false);

// --- Vimeo Thumbnail Handling ---
const vimeoThumbnails = ref<Record<string, string>>({});

const fetchVimeoThumbnail = async (url: string) => {
  if (import.meta.server) return; // SSR guard (original was CSR-only)
  if (!url || vimeoThumbnails.value[url]) return;
  const match = url.match(/vimeo\.com\/(\d+)/);
  if (match) {
    const videoId = match[1];
    try {
      const response = await fetch(`https://vimeo.com/api/v2/video/${videoId}.json`);
      const data = await response.json();
      if (data && data[0] && data[0].thumbnail_large) {
        vimeoThumbnails.value[url] = data[0].thumbnail_large;
      }
    } catch (e) {
      console.warn('Failed to fetch vimeo thumbnail for', url, e);
    }
  }
};

// Helper for thumbnail
const getSmartThumbnail = (project: any) => {
  if (project.imageUrl) return project.imageUrl;

  // 1. If explicit mediaUrl is present, try to derive
  if (project.mediaUrl) {
     const derived = getThumbnailUrl(project.mediaUrl);
     if (derived) return derived;
  }

   // 2. If it is a Vimeo link, try to use the fetched thumbnail
   if (project.mediaUrl && vimeoThumbnails.value[project.mediaUrl]) {
     return vimeoThumbnails.value[project.mediaUrl];
   }

  // 3. Fallback to placeholder
  return 'https://placehold.co/600x400?text=Project'; // improved placeholder
};

// Watch for projects and fetch vimeo thumbnails
watch(projects, (newProjects) => {
  newProjects.forEach((p: any) => {
    if (p.mediaUrl && p.mediaUrl.includes('vimeo.com')) {
      fetchVimeoThumbnail(p.mediaUrl);
    }
  });
}, { immediate: true });

onMounted(async () => {
  // Owner check (runs client-side only)
  try {
    const token =
      localStorage.getItem('accessToken') ||
      sessionStorage.getItem('accessToken');
    if (!token) return;
    const res = await fetch(`${baseUrl}/me`, {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      }
    });
    if (!res.ok) return;
    const json = await res.json();
    const user = json?.data !== undefined ? json.data : json;
    isOwner.value = !!user && user.slug === slug;
  } catch (e) {
    console.warn('[ProjectsPage] Could not verify ownership for the owner tip.', e);
  }
});
</script>

<template>
  <div class="projects-page py-5">
    <div class="container-fluid">
      <!-- Centered and restyled hero section -->
      <div class="text-center mb-5">
        <h1 class="display-4 fw-bold animate-fade-in-up glass-text">
          <KanbanSquare aria-hidden="true" :size="48" /> Projects Showcase
        </h1>
        <p class="lead animate-fade-in-up glass-subtitle" style="animation-delay: 0.1s;">
          A curated collection of my work, from professional applications to personal experiments.
        </p>
      </div>

      <LoadingModal :visible="pending"/>

      <!-- Skeleton loader with new glass styles -->
      <div v-if="pending" class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        <div v-for="n in 6"
             :key="n"
             :style="{ 'animation-delay': (n * 0.05) + 's' }"
             class="col animate-fade-in-up">
          <div class="card h-100 glass-card glass-card-floating">
            <div class="card-img-top skeleton-line"
                 style="height: 200px; border-radius: 1.5rem 1.5rem 0 0;"></div>
            <div class="card-body d-flex flex-column">
              <div class="skeleton-line skeleton-title" style="width: 60%;"></div>
              <div class="skeleton-line skeleton-subtitle"></div>
              <div class="skeleton-line skeleton-subtitle" style="width: 80%;"></div>
              <div class="d-flex flex-wrap mt-auto pt-3">
                <div class="skeleton-line me-1 mb-1"
                     style="width: 50px; height: 24px; border-radius: 0.25rem;"></div>
                <div class="skeleton-line me-1 mb-1"
                     style="width: 70px; height: 24px; border-radius: 0.25rem;"></div>
                <div class="skeleton-line me-1 mb-1"
                     style="width: 60px; height: 24px; border-radius: 0.25rem;"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Error state with glassmorphic styling -->
      <div v-else-if="error" class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <AlertTriangle class="text-warning mb-3" style="font-size: 3rem;" />
          <h5 class="card-title glass-title mb-3">Unable to Load Projects</h5>
          <p class="card-text glass-description">
            Could not load projects data. Please try again later.
          </p>
        </div>
      </div>

      <div v-else-if="projects && projects.length > 0"
           class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        <div v-for="(project, index) in projects" :key="project.uuid"
             :style="{ 'animation-delay': (index * 0.1) + 0.2 + 's' }"
             class="col animate-fade-in-up">
          <NuxtLink :to="`/${slug}/project/${project.uuid}`"
                    class="project-card-link interactive-card-lift interactive-card-shadow-primary">
            <div class="card h-100 glass-card glass-card-floating">
                <div class="card-img-container">
                <img
                  v-if="getSmartThumbnail(project)"
                  :alt="project.title"
                  :src="getSmartThumbnail(project)"
                  class="card-img-top"
                >
                <div v-else class="card-img-top project-image-placeholder">
                  <Image />
                </div>
                <div class="card-img-overlay">
                  <div class="icon"><ExternalLink /></div>
                  <div class="text">View Project</div>
                </div>
              </div>
              <div class="card-body d-flex flex-column">
                <h5 class="card-title glass-title">{{ project.title }}</h5>
                <p class="card-text small glass-description">
                  {{
                    project.description?.substring(0, 180)
                  }}{{ (project.description?.length || 0) > 180 ? '...' : '' }}
                </p>

                <!-- Phase 7.4 §6: structured key features (fallback = description only) -->
                <ul v-if="project.keyFeatures && project.keyFeatures.length"
                    class="key-features-list card-text small glass-description mt-2 mb-0 ps-3">
                  <li v-for="(feature, fi) in project.keyFeatures.slice(0, 3)" :key="'kf-' + fi">
                    {{ feature }}
                  </li>
                  <li v-if="project.keyFeatures.length > 3" class="text-muted">
                    +{{ project.keyFeatures.length - 3 }} more
                  </li>
                </ul>

                <div v-if="project.skills && project.skills.length"
                     class="mt-auto pt-2 d-flex flex-wrap gap-2">
                  <!-- The shared SkillBadge component is auto-imported and takes a `skill` prop -->
                  <SkillBadge
                    v-for="skill in (project.skills as Skill[])"
                    :key="skill.skillId"
                    :skill="skill"
                  />
                </div>
              </div>
            </div>
          </NuxtLink>
        </div>
      </div>

      <!-- Enhanced empty state with a helpful tip for the owner -->
      <div v-else class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <div class="empty-state-icon mb-4">
            <KanbanSquare />
          </div>
          <h4 class="card-title glass-title mb-3">No Projects Yet</h4>

          <!-- Generic message for public visitors -->
          <p v-if="!isOwner" class="card-text glass-subtitle mb-4">
            New projects are in the pipeline. Please check back later to see the showcase.
          </p>

          <!-- Helpful tip for the portfolio owner -->
          <div v-else class="alert alert-info mt-3">
            <p class="mb-1"><strong>Hey there!</strong> It looks like you don't have any projects
              visible on your public page.</p>
            <p class="mb-0">
              Go to your
              <a :href="`/${slug}/manage/projects`">Project
                Management
              </a>
              page to add new projects or make existing ones visible.
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
// Icons ported from ProjectsPage.vue (lucide-vue-next)
import { KanbanSquare, AlertTriangle, Image, ExternalLink } from 'lucide-vue-next';
import type { Skill } from '~/utils/skillsUtils';

export default {
  components: {
    KanbanSquare,
    AlertTriangle,
    Image,
    ExternalLink,
  },
};
</script>

<style scoped>
/* --- Page Styling --- */
.projects-page {
  overflow-x: hidden;
}

/* --- Phase 7.4 §6: structured key features on project cards --- */
.key-features-list {
  list-style: none;
  padding-left: 1rem;
  position: relative;
  margin: 0.5rem 0 0 0;
}

.key-features-list li {
  position: relative;
  margin-bottom: 0.15rem;
}

.key-features-list li::before {
  content: '–';
  position: absolute;
  left: -0.9rem;
  color: var(--bs-primary);
}

/* --- Optimization --- */
.project-card-link {
  will-change: transform, box-shadow;
}

/* --- Link Wrapper for Card --- */
.project-card-link {
  display: block;
  height: 100%;
  text-decoration: none;
  color: inherit;
  border-radius: 1rem;
  /* The interactive classes will apply their transitions */
  /* Add will-change to hint the browser about upcoming transformations for smoother animations. */
  will-change: transform, box-shadow;
}

.project-card-link:hover {
  color: inherit;
}

/*
  PERFORMANCE FIX:
  The global 'interactive-card-shadow-primary' class has a very heavy box-shadow
  animation which can cause lag. We override it here with a simpler,
  more performant shadow that still provides a nice "lift" effect.
*/
.project-card-link.interactive-card-shadow-primary:hover {
  box-shadow: 0 12px 35px rgba(var(--bs-primary-rgb), 0.25) !important;
}

.card-img-container {
  height: 200px;
  overflow: hidden;
  position: relative;
  border-top-left-radius: 1rem;
  border-top-right-radius: 1rem;
}

.card-img-top {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
  /* Hint to the browser that this property will be animated. */
  will-change: transform;
}

.project-card-link:hover .card-img-top {
  transform: scale(1.05); /* Reduced scale for better performance and a subtler effect. */
}

.project-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--glass-bg);
  color: var(--glass-text-secondary);
  font-size: 3rem;
  opacity: 0.8;
}

/* --- Image Overlay Styling --- */
.card-img-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(var(--bs-primary-rgb), 0.7);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
  /* Hint to the browser that this property will be animated. */
  will-change: opacity;
}

.project-card-link:hover .card-img-overlay {
  opacity: 1;
}

.card-img-overlay .icon {
  font-size: 2.75rem;
  margin-bottom: 0.5rem;
  transition: transform 0.3s ease;
}

.project-card-link:hover .card-img-overlay .icon {
  transform: scale(1.1) rotate(5deg);
}

.card-img-overlay .text {
  font-weight: 500;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--bs-primary);
  opacity: 0.6;
}
</style>
