<script setup lang="ts">
/**
 * @file app/pages/[slug]/index.vue
 * @description The main public-facing portfolio homepage for a user.
 * Ported from ForkMyFolio-frontend-vue/src/views/public/HomePage.vue.
 *
 * Displays the profile summary card, social links, cover-letter modal,
 * share actions, and floating download buttons (PDF / Markdown / vCard).
 * Handles loading, error, private-portfolio and empty states.
 */
import {
  Lock, AlertTriangle, UserCircle, FileDown, Mail, Linkedin, Github, Globe, Twitter, Link as LinkIcon,
  File, FileCode2, CheckCircle, Info,
} from 'lucide-vue-next';

const route = useRoute();
const slug = route.params.slug as string;
// DEF-1: public analytics tracking (non-credential config; fails soft).
useAnalyticsTracking();

const { fetchJson, fetchRaw, baseUrl } = useApi();

// --- Data: mirrors the Vue publicPortfolioStore's primary fetch ---
// Phase 7.4 (403/404 policy): the backend now conceals private portfolios as
// 404 (indistinguishable from a nonexistent slug), so the "unavailable" state
// triggers on BOTH 404 and 403 (403 kept defensively for role denials).
const isPrivate = ref(false);
const { data: portfolio, pending, error, refresh } = await useAsyncData(
  `portfolio-${slug}`,
  async () => {
    isPrivate.value = false;
    try {
      return await fetchJson<any>(`/portfolios/${slug}`);
    } catch (e: any) {
      if (/\b(403|404)\b/.test(e?.message ?? '')) {
        isPrivate.value = true;
      }
      throw e;
    }
  }
);

// --- Computeds (ported) ---
const fullName = computed(() => {
  if (!portfolio.value?.user) return '';
  return `${portfolio.value.user.firstName || ''} ${portfolio.value.user.lastName || ''}`.trim();
});

// SEO v3.1.2: full metadata (title/description/canonical/robots/OG/Twitter/
// JSON-LD) derived from the fetched portfolio payload. Must stay BELOW the
// reactive declarations above (TDZ — see note in usePortfolioSeo).
usePortfolioSeo({
  slug,
  section: 'home',
  portfolio: () => portfolio.value,
  httpErrorStatus: () => (isPrivate.value ? 404 : apiErrorStatus(error.value?.message)),
});

// --- Inline replication of notificationService + NotificationContainer (toast UI) ---
// Global concern in the Vue original; ported inline because shared files are out of scope.
interface Notification {
  id: number;
  message: string;
  type?: 'success' | 'error' | 'warning' | 'info';
}

const notifications = ref<Notification[]>([]);

const addNotification = ({ message, type = 'info', duration = 5000 }: { message: string; type?: Notification['type']; duration?: number }) => {
  const id = Date.now() + Math.random();
  notifications.value.push({ id, message, type });
  setTimeout(() => {
    removeNotification(id);
  }, duration);
};

const removeNotification = (id: number) => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index !== -1) {
    notifications.value.splice(index, 1);
  }
};

const toastClass = (type: string | undefined) => {
  switch (type) {
    case 'success':
      return 'text-bg-success';
    case 'error':
      return 'text-bg-danger';
    case 'warning':
      return 'text-bg-warning';
    default:
      return 'text-bg-info';
  }
};

const iconComponent = (type?: string) => {
  switch (type) {
    case 'success':
      return CheckCircle;
    case 'error':
    case 'warning':
      return AlertTriangle;
    default:
      return Info;
  }
};

const toastTitle = (type?: string) => {
  switch (type) {
    case 'success':
      return 'Success';
    case 'error':
      return 'Error';
    case 'warning':
      return 'Warning';
    default:
      return 'Information';
  }
};

// --- Owner/Admin check (client-side replica of authService) ---
// Access token lives in localStorage (or sessionStorage for guests) shared with the SPA.
const adminUser = ref<any>(null);
const isAdmin = ref(false);
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
    adminUser.value = user;
    isAdmin.value = !!user?.roles?.includes('ADMIN');
    isOwner.value = !!user && user.slug === slug;
  } catch (e) {
    console.warn('[HomePage] Could not verify the current session for admin tips.', e);
  }
});

// --- Local UI State ---
const showCoverLetterModal = ref(false);
const coverLetterModalRef = ref<HTMLElement | null>(null);
const isDownloading = ref(false);
const errorModalMessage = ref('');
const linkCopied = ref(false);

let coverLetterModalInstance: any = null;
onMounted(async () => {
  const bootstrap = await import('bootstrap');
  if (coverLetterModalRef.value) {
    coverLetterModalInstance = new bootstrap.Modal(coverLetterModalRef.value);
    // Keep state in sync if modal is closed by other means (e.g., Esc key)
    coverLetterModalRef.value.addEventListener('hide.bs.modal', () => {
      showCoverLetterModal.value = false;
    });
  }
});

watch(showCoverLetterModal, (isVisible) => {
  if (isVisible) {
    coverLetterModalInstance?.show();
  } else {
    coverLetterModalInstance?.hide();
  }
});

// --- Download Handlers ---
// Vue used downloadPublicPortfolioBySlug / downloadMarkdownBySlug / downloadVCardBySlug
// (raw-response endpoints /portfolios/{slug}/pdf|markdown|vcard). fetchRaw returns the
// raw Response, matching the original's responseType:'raw' behavior.
const createDownloadHandler = (path: string, fileType: string, extension: string) => async () => {
  if (!slug) return;
  isDownloading.value = true;
  errorModalMessage.value = ''; // Clear previous errors

  try {
    const response = await fetchRaw(`/portfolios/${slug}/${path}`);
    if (!response.ok) {
      throw new Error(`API error ${response.status}`);
    }
    const blob = await response.blob();
    const filename = getFilenameFromResponse(response, `${slug}-portfolio.${extension}`);
    triggerDownload(blob, filename);
    addNotification({
      message: `${fileType.toUpperCase()} download has started.`,
      type: 'success'
    });
  } catch (err: unknown) {
    console.error(`${fileType} download failed:`, err);
    errorModalMessage.value = err instanceof Error ? err.message : `An unexpected error occurred while downloading the ${fileType.toUpperCase()} file.`;
  } finally {
    isDownloading.value = false;
  }
};

const handleDownloadPdf = createDownloadHandler('pdf', 'pdf', 'pdf');
const handleDownloadMd = createDownloadHandler('markdown', 'md', 'md');
const handleDownloadVcf = createDownloadHandler('vcard', 'vcf', 'vcf');

// --- Social Sharing Handlers ---
const shareOnLinkedIn = () => {
  const url = encodeURIComponent(window.location.href);
  window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${url}`, '_blank');
};

const shareOnX = () => {
  const url = encodeURIComponent(window.location.href);
  const text = encodeURIComponent(`Check out ${fullName.value}'s portfolio on ForkMyFolio!`);
  window.open(`https://twitter.com/intent/tweet?url=${url}&text=${text}`, '_blank');
};

const copyPortfolioLink = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href);
    linkCopied.value = true;
    setTimeout(() => {
      linkCopied.value = false;
    }, 2000);
    addNotification({
      message: 'Portfolio link copied to clipboard!',
      type: 'success'
    });
  } catch (err) {
    console.error('Failed to copy link:', err);
  }
};

// Retry for the error state (Vue had no explicit retry here, but refresh() is the
// Nuxt equivalent of re-fetching; the error card shows the original message).
const retryLoad = () => {
  refresh();
};
</script>

<template>
  <div class="home-page py-5">
    <LoadingModal :visible="pending || isDownloading"/>

    <!-- Skeleton Loader: A placeholder for when the profile is loading -->
    <div v-if="pending" class="container">
      <div class="animate-fade-in-up">
        <div class="card glass-card glass-card-floating p-2 p-md-5">
          <div class="card-body">
            <div class="row align-items-center">
              <div class="col-md-4 text-center mb-3 mb-md-0">
                <div class="skeleton-line rounded-circle shadow-lg mx-auto"
                     style="width: 100%; max-width: 200px; aspect-ratio: 1/1;"></div>
              </div>
              <div class="col-md-8">
                <div class="skeleton-line skeleton-title" style="width: 70%; height: 36px;"></div>
                <div class="skeleton-line skeleton-subtitle"
                     style="width: 50%; height: 24px;"></div>
                <div class="skeleton-line skeleton-grade" style="width: 100%;"></div>
                <div class="skeleton-line skeleton-grade" style="width: 90%;"></div>
                <div class="skeleton-line skeleton-grade"
                     style="width: 80%; margin-bottom: 1.5rem;"></div>
                <div class="d-flex flex-wrap align-items-center mb-3 justify-content-center">
                  <div class="skeleton-line me-2 mb-2"
                       style="width: 140px; height: 36px; border-radius: 0.75rem;"></div>
                  <div class="skeleton-line mb-2"
                       style="width: 140px; height: 36px; border-radius: 0.75rem;"></div>
                </div>
                <div class="d-flex flex-wrap justify-content-center">
                  <div class="skeleton-icon me-2"></div>
                  <div class="skeleton-icon me-2"></div>
                  <div class="skeleton-icon me-2"></div>
                  <div class="skeleton-icon"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Private Portfolio State -->
    <div v-else-if="isPrivate" class="container py-4">
      <div class="glass-card mx-auto" style="max-width: 90%;">
        <div class="card-body text-center p-4">
          <div class="mb-4">
            <Lock class="text-warning" style="font-size: 4rem;" />
          </div>
          <h2 class="h4 glass-title mb-3">Portfolio Not Available</h2>
          <p class="glass-description mb-4">
            This portfolio does not exist or is not publicly accessible.
          </p>
          <div v-if="isAdmin" class="alert alert-info border-0 shadow-sm rounded-4 mt-3">
            <p class="mb-2"><strong>Tip:</strong> This is your portfolio! Access is restricted because it's set to <b>Private</b>.</p>
            <a :href="`/${slug}/manage/settings/display`" class="btn btn-sm btn-primary rounded-pill px-3">
              Go to Settings
            </a>
            to make it public.
          </div>
          <NuxtLink to="/" class="btn glass-btn-primary mt-3 px-4 rounded-pill">
            Return Home
          </NuxtLink>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="container py-4">
      <div class="glass-card mx-auto" style="max-width: 90%;">
        <div class="card-body text-center p-3">
          <AlertTriangle class="text-warning mb-2" style="font-size: 2rem;" />
          <h5 class="card-title glass-title mb-2">Unable to Load Profile</h5>
          <p class="card-text glass-description">
            {{ (error as any)?.message || 'Could not load profile data. Please try again later.' }}
          </p>
          <button class="btn glass-btn-primary mt-2" @click="retryLoad">
            Try Again
          </button>
        </div>
      </div>
    </div>

    <!-- Profile Display State: The main cover page view -->
    <div v-else-if="portfolio?.profile" class="hero-section">
      <div class="container">
        <div class="animate-fade-in-up">
          <div
            class="card glass-card glass-card-floating p-2 p-md-5 interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body">
              <div class="row align-items-center">
                <div class="col-md-4 text-center mb-3 mb-md-0">
                  <a :href="portfolio?.profile?.resumeUrl || '#'" class="profile-image-link shadow-lg"
                     target="_blank">
                    <img v-if="portfolio?.profile?.resumeImageUrl || portfolio?.user?.profileImageUrl"
                         :alt="portfolio?.profile?.resumeImageUrl ? 'Resume Preview' : 'Profile Picture'"
                         :src="portfolio?.profile?.resumeImageUrl || portfolio?.user?.profileImageUrl"
                         class="profile-image"/>
                    <div v-else
                         class="profile-image-placeholder d-flex align-items-center justify-content-center">
                      <UserCircle />
                    </div>
                  </a>
                </div>
                <div class="col-md-8">
                  <h1 class="display-4 fw-light text-gradient">{{ fullName }}</h1>
                  <p class="lead text-primary glass-subtitle">{{ portfolio?.profile?.headline }}</p>
                  <p class="summary-text glass-description">{{ portfolio?.profile?.summary }}</p>

                  <div class="mt-3">
                    <div class="d-flex flex-wrap align-items-center mb-3 justify-content-center">
                      <a v-if="portfolio?.profile?.resumeUrl"
                         :href="portfolio?.profile?.resumeUrl"
                         class="btn glass-btn-primary me-2 mb-2 interactive-lift"
                         target="_blank">
                        <FileDown class="me-1" />View Resume
                      </a>
                      <button v-if="portfolio?.profile?.coverLetterTemplate"
                              class="btn glass-btn-primary me-2 mb-2 interactive-lift"
                              @click="showCoverLetterModal = true">
                        <Mail class="me-1" />View Cover Letter
                      </button>
                    </div>
                    <div class=" social-links d-flex flex-wrap justify-content-center">
                      <a v-if="portfolio?.profile?.linkedinUrl" :href="portfolio?.profile?.linkedinUrl"
                         class="social-icon" target="_blank"
                         title="LinkedIn"><Linkedin /></a>
                      <a v-if="portfolio?.profile?.githubUrl" :href="portfolio?.profile?.githubUrl"
                         class="social-icon" target="_blank"
                         title="GitHub"><Github /></a>
                      <a v-if="portfolio?.profile?.websiteUrl" :href="portfolio?.profile?.websiteUrl"
                         class="social-icon" target="_blank"
                         title="Personal Website"><Globe /></a>
                      <a v-if="portfolio?.profile?.publicEmail"
                         :href="`mailto:${portfolio?.profile?.publicEmail}`" class="social-icon"
                         title="Email Me"><Mail /></a>
                    </div>
                  </div>

                  <!-- Share Section -->
                  <div class="mt-4 pt-3 border-top w-100">
                    <p class="text-center small text-muted mb-2">Share this portfolio:</p>
                    <div class="d-flex justify-content-center gap-3">
                      <button class="btn btn-sm glass-btn-outline rounded-pill px-3" @click="shareOnLinkedIn" title="Share on LinkedIn">
                        <Linkedin class="me-1" /> LinkedIn
                      </button>
                      <button class="btn btn-sm glass-btn-outline rounded-pill px-3" @click="shareOnX" title="Share on X">
                        <Twitter class="me-1" /> X
                      </button>
                      <button class="btn btn-sm glass-btn-outline rounded-pill px-3" @click="copyPortfolioLink" title="Copy Link">
                        <LinkIcon class="me-1" /> {{ linkCopied ? 'Copied!' : 'Link' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Profile Missing / Empty State -->
    <div v-else class="container py-4">
      <div class="glass-card mx-auto" style="max-width: 90%;">
        <div class="card-body text-center p-3">
          <div class="empty-state-icon mb-3">
            <UserCircle />
          </div>
          <h4 class="card-title glass-title mb-2">Portfolio Coming Soon!</h4>
          <p class="card-text glass-subtitle mb-3">
            The owner is currently setting things up. Please check back later.
          </p>
          <div v-if="isAdmin" class="alert alert-info mt-3">
            <p class="mb-1"><strong>Admin Tip:</strong> Your public profile is live but appears
              empty.</p>
            <p class="mb-0">
              <a v-if="adminUser?.slug" :href="`/${adminUser?.slug}/manage/profile`">
                Go to the Profile Editor
              </a>
              to add your headline, summary, and more.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Cover Letter Modal -->
    <div ref="coverLetterModalRef" class="modal fade" tabindex="-1">
      <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content glass-modal">
          <div class="modal-header">
            <h5 class="modal-title glass-title">Cover Letter Template</h5>
            <button aria-label="Close" class="btn-close" data-bs-dismiss="modal" type="button"></button>
          </div>
          <div class="modal-body">
            <pre v-if="portfolio?.profile" class="cover-letter-text glass-description">{{
                portfolio?.profile?.coverLetterTemplate
              }}</pre>
          </div>
          <div class="modal-footer">
            <button class="btn btn-outline-secondary" data-bs-dismiss="modal" type="button">Close</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal for download errors -->
    <ErrorModal
      :message="errorModalMessage"
      :visible="!!errorModalMessage"
      title="Download Failed"
      @close="errorModalMessage = ''"
    />

    <!-- Floating Action Buttons -->
    <div v-if="portfolio?.profile" class="download-actions">
      <!-- PDF Button -->
      <button
        :disabled="isDownloading"
        class="btn glass-btn-primary btn-lg rounded-circle shadow-lg"
        title="Download Resume as PDF"
        @click="handleDownloadPdf"
      >
        <span v-if="isDownloading" aria-hidden="true" class="spinner-border spinner-border-sm"
              role="status"></span>
        <File v-else />
      </button>
      <!-- Markdown Button -->
      <button
        :disabled="isDownloading"
        class="btn glass-btn-primary btn-lg rounded-circle shadow-lg"
        title="Download as Markdown"
        @click="handleDownloadMd"
      >
        <span v-if="isDownloading" aria-hidden="true" class="spinner-border spinner-border-sm"
              role="status"></span>
        <FileCode2 v-else />
      </button>
      <!-- vCard Button -->
      <button
        :disabled="isDownloading"
        class="btn glass-btn-primary btn-lg rounded-circle shadow-lg"
        title="Download vCard"
        @click="handleDownloadVcf"
      >
        <span v-if="isDownloading" aria-hidden="true" class="spinner-border spinner-border-sm"
              role="status"></span>
        <UserCircle v-else />
      </button>
    </div>

    <!-- Toast notifications (replicating NotificationContainer) -->
    <ClientOnly>
      <div class="toast-container position-fixed top-0 end-0 p-3">
        <TransitionGroup name="toast-fade">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            :class="toastClass(notification.type)"
            aria-atomic="true"
            aria-live="assertive"
            class="toast show"
            role="alert"
          >
            <div class="toast-header">
              <component :is="iconComponent(notification.type)" class="me-2" />
              <strong class="me-auto">{{ toastTitle(notification.type) }}</strong>
              <button
                aria-label="Close"
                class="btn-close"
                type="button"
                @click="removeNotification(notification.id)"
              ></button>
            </div>
            <div class="toast-body">
              {{ notification.message }}
            </div>
          </div>
        </TransitionGroup>
      </div>
    </ClientOnly>
  </div>
</template>

<style scoped>
.home-page {
  overflow-x: hidden;
}

.hero-section {
  width: 100%;
}

.profile-image-link {
  display: inline-block;
  position: relative;
  width: 100%;
  max-width: 200px;
  aspect-ratio: 1 / 1;
  margin: 0 auto;
  border-radius: 50%;
  border: 4px solid var(--glass-bg);
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.profile-image-link:hover {
  transform: scale(1.05);
  box-shadow: 0 0.5rem 1.5rem rgba(0, 0, 0, 0.15) !important;
}

.profile-image,
.profile-image-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-image-placeholder .bi {
  font-size: 4rem;
  color: var(--glass-text-secondary);
}

.text-gradient {
  background: linear-gradient(45deg, var(--bs-primary), var(--bs-info));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  color: var(--bs-primary);
  padding-bottom: 0.15em;
}

.summary-text {
  font-size: clamp(0.85rem, 2.5vw, 0.95rem);
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

.social-links {
  gap: 15px;
}

.social-links a {
  margin-right: 0;
  font-size: 1.8rem;
  color: var(--glass-text-secondary);
  transition: all 0.3s ease;
}

.social-links a:hover {
  color: var(--bs-primary);
  transform: translateY(-2px);
}

.cover-letter-text {
  white-space: pre-wrap;
  font-family: var(--bs-font-sans-serif);
  font-size: clamp(0.8rem, 2.5vw, 0.9rem);
  line-height: 1.5;
}

.empty-state-icon {
  font-size: 3rem;
  color: var(--bs-primary);
}

/* --- Download Buttons --- */
.download-actions {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 1030;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.download-actions .btn {
  width: 48px;
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1.2rem;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.download-actions .btn:hover {
  transform: scale(1.1);
}

@media (max-width: 576px) {
  .col-md-8 {
    text-align: center;
  }

  .profile-image-link {
    width: 70%;
    max-width: 160px;
  }

  .skeleton-line.rounded-circle {
    width: 70%;
    max-width: 140px;
    aspect-ratio: 1 / 1;
  }

  h1.display-4 {
    font-size: clamp(1.2rem, 4.5vw, 1.8rem);
  }

  p.lead {
    font-size: clamp(0.85rem, 2.5vw, 0.95rem);
  }

  .d-flex.flex-wrap {
    justify-content: center !important;
    gap: 8px;
  }

  .download-actions {
    bottom: 1rem;
    right: 1rem;
    gap: 0.5rem;
  }

  .download-actions .btn {
    width: 40px;
    height: 40px;
    font-size: 1rem;
  }

  .modal-dialog.modal-lg {
    margin: 0.5rem;
    max-width: 98%;
  }

  .card-body {
    padding: 1rem;
  }

  .glass-card {
    max-width: 95%;
  }

  .social-links a {
    font-size: 1.5rem;
  }

  .btn.glass-btn-primary {
    font-size: 0.85rem;
    padding: 0.5rem 1rem;
  }
}
</style>
