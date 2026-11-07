<script setup lang="ts">
/**
 * @file app/pages/[slug]/testimonials.vue
 * @description Public testimonials page.
 * Ported from ForkMyFolio-frontend-vue/src/views/public/TestimonialsPage.vue.
 *
 * The selected testimonial is synced with a `?testimonial=<uuid>` query param so
 * the browser Back button closes the modal and deep links open it.
 */
import { MessageSquareQuote, AlertTriangle, Quote } from 'lucide-vue-next';
import TestimonialDetailsModal from '~/components/modals/TestimonialDetailsModal.vue';

const route = useRoute();
const slug = route.params.slug as string;
// DEF-1: public analytics tracking (non-credential config; fails soft).
useAnalyticsTracking();

const { fetchJson, baseUrl } = useApi();

// --- Data (SEO v3.1.2: sourced from the full public portfolio payload) ---
// Same single request the landing page uses (shared `portfolio-${slug}`
// useAsyncData key — SPA navigation between sections never refetches), and it
// carries the profile needed for rich metadata. The backend's PortfolioMapper
// already restricts this list to visible testimonials; the client-side filter
// below is preserved from the original port (defence in depth).
const { data: portfolio, pending, error, refresh } = await useAsyncData(
  `portfolio-${slug}`,
  () => fetchJson(`/portfolios/${slug}`)
);

// FIX (from the original): the public page should only display testimonials that
// are marked as visible. We filter on the client side (defence in depth).
const testimonials = computed(() => {
  return ((portfolio.value?.testimonials as any[]) || []).filter(t => t.visible);
});

// SEO v3.1.2: full metadata from the same payload (TDZ — must stay below the
// declarations above; see usePortfolioSeo).
usePortfolioSeo({
  slug,
  section: 'testimonials',
  portfolio: () => portfolio.value,
  dataLead: (name: string) => {
    const list = testimonials.value || [];
    if (!list.length) return null;
    const first = list[0]?.authorName ? ` — including one from ${list[0].authorName}` : '';
    return `${list.length} testimonial${list.length === 1 ? '' : 's'} for ${name}${first}.`;
  },
  httpErrorStatus: () => apiErrorStatus(error.value?.message),
});

// --- Owner check (client-side replica of authService) ---
const isOwner = ref(false);

onMounted(async () => {
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
    console.warn('[TestimonialsPage] Could not verify ownership for the owner tip.', e);
  }
});

// --- Modal State & Route-Awareness ---
// The selected testimonial is synced with the `testimonial` query parameter in
// the URL so the browser's back button closes the modal naturally.
const selectedTestimonial = ref<any | null>(null);

// Watch for changes in the query parameters to show/hide the modal.
watch(() => route.query.testimonial, (uuid) => {
  if (uuid) {
    const found = testimonials.value.find(t => t.uuid === uuid);
    if (found) {
      selectedTestimonial.value = found;
    } else {
      selectedTestimonial.value = null; // Clear if UUID is invalid
    }
  } else {
    selectedTestimonial.value = null;
  }
}, { immediate: true });

const selectTestimonial = (testimonial: any) => {
  // Push the unique UUID to the current URL.
  // Using push creates a history entry, so 'Back' will naturally close the modal.
  useRouter().push({
    query: { ...route.query, testimonial: testimonial.uuid }
  });
};

const closeModal = () => {
  // When closing, remove the query parameter using replace so the History
  // doesn't become polluted with multiple "closed" states.
  const router = useRouter();
  const newQuery = { ...route.query };
  delete newQuery.testimonial;
  router.replace({ query: newQuery });
};

const retryLoad = () => {
  refresh();
};
</script>

<template>
  <div class="testimonials-page py-5">
    <div class="container">
      <div class="text-center mb-5">
        <h1 class="display-4 fw-bold animate-fade-in-up glass-text">
          <MessageSquareQuote aria-hidden="true" :size="48" /> Testimonials
        </h1>
        <p class="lead animate-fade-in-up glass-subtitle" style="animation-delay: 0.1s;">
          What colleagues and clients are saying about my work.
        </p>
      </div>

      <LoadingModal :visible="pending"/>

      <!-- A skeleton loader that mimics the new testimonial card style -->
      <div v-if="pending" class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        <div v-for="n in 6"
             :key="n"
             :style="{ 'animation-delay': (n * 0.05) + 's' }"
             class="col animate-fade-in-up">
          <div class="card glass-card glass-card-floating h-100">
            <div class="card-body d-flex flex-column">
              <div class="flex-grow-1">
                <div class="skeleton-line skeleton-subtitle mb-2"></div>
                <div class="skeleton-line skeleton-subtitle mb-2"></div>
                <div class="skeleton-line skeleton-grade" style="width: 70%;"></div>
              </div>
              <div class="mt-auto text-end">
                <div class="skeleton-line skeleton-title"
                     style="width: 60%; margin-left: auto;"></div>
                <div class="skeleton-line skeleton-subtitle"
                     style="width: 80%; margin-left: auto;"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <AlertTriangle class="text-warning mb-3" style="font-size: 3rem;" />
          <h5 class="card-title glass-title mb-3">Unable to Load Testimonials</h5>
          <p class="card-text glass-description">
            {{ (error as any)?.message || 'Could not load testimonials. Please try again later.' }}
          </p>
          <button class="btn glass-btn-primary mt-2" @click="retryLoad">
            Try Again
          </button>
        </div>
      </div>

      <!-- State for when a portfolio is loaded and has testimonials -->
      <div v-else-if="testimonials.length > 0"
           class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        <div v-for="(testimonial, index) in testimonials"
             :key="testimonial.uuid"
             :style="{ 'animation-delay': (index * 0.1 + 0.2) + 's' }"
             class="col animate-fade-in-up"
        >
          <div
            class="card glass-card glass-card-floating h-100 interactive-card-lift interactive-card-shadow-primary"
            role="button" tabindex="0"
            @click="selectTestimonial(testimonial)"
          >
            <div class="card-body d-flex flex-column">
              <Quote aria-hidden="true" class="card-quote-icon" />
              <figure class="mb-0 d-flex flex-column flex-grow-1">
                <blockquote class="blockquote mb-4 flex-grow-1">
                  <p class="glass-description">{{ testimonial.quote }}</p>
                </blockquote>
                <figcaption class="blockquote-footer mt-auto text-end">
                  <strong class="d-block glass-title">{{ testimonial.authorName }}</strong>
                  <cite :title="testimonial.authorTitle"
                        class="glass-subtitle">{{ testimonial.authorTitle }}</cite>
                </figcaption>
              </figure>
            </div>
          </div>
        </div>
      </div>

      <!-- State for when a portfolio is loaded but has no testimonials -->
      <div v-else class="glass-card mx-auto" style="max-width: 800px;">
        <div class="card-body text-center p-5">
          <div class="empty-state-icon mb-4">
            <MessageSquareQuote />
          </div>
          <h4 class="card-title glass-title mb-3">No Testimonials Yet</h4>
          <!-- Generic message for public visitors -->
          <p v-if="!isOwner" class="card-text glass-subtitle mb-4">
            This user has not added any testimonials yet. Please check back later.
          </p>

          <!-- Helpful tip for the portfolio owner -->
          <div v-else class="alert alert-info mt-3">
            <p class="mb-1"><strong>Hey there!</strong> It looks like you don't have any testimonials
              visible on your public page.</p>
            <p class="mb-0">
              Go to your
              <a :href="`/${slug}/manage/testimonials`">
                Testimonial Management
              </a>
              page to add new entries.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Details Modal -->
    <TestimonialDetailsModal
      :testimonial="selectedTestimonial"
      @close="closeModal"
    />
  </div>
</template>

<style scoped>
.testimonials-page {
  overflow-x: hidden;
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out forwards;
}

.card[role="button"] {
  cursor: pointer;
}

/* Watermark Quote Icon */
.card-quote-icon {
  position: absolute;
  top: -10px;
  left: -10px;
  font-size: 8rem;
  color: var(--glass-shimmer);
  transform: rotate(-15deg);
  z-index: 0;
  pointer-events: none;
  opacity: 0.5;
}

/* Card Body */
.card-body {
  z-index: 1;
}

/* Blockquote Styling */
.blockquote p {
  font-size: 1.1rem;
  font-style: italic;
  line-height: 1.7;
}

.blockquote-footer {
  font-size: 0.9rem;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--bs-primary);
  opacity: 0.6;
}
</style>
