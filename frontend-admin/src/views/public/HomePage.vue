<template>
  <div class="home-page py-5">
    <LoadingModal :visible="isLoading || isDownloading"/>

    <!-- Skeleton Loader: A placeholder for when the profile is loading -->
    <div v-if="isLoading" class="container">
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
            <!-- UAT-5 FIX: previously name 'settings' (unregistered) — the RouterLink computed threw a NO_MATCH error on every render of this block. -->
            <router-link :to="adminSettingsTarget" class="btn btn-sm btn-primary rounded-pill px-3">
              Go to Settings
            </router-link>
            to make it public.
          </div>
          <router-link to="/" class="btn glass-btn-primary mt-3 px-4 rounded-pill">
            Return Home
          </router-link>
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
            {{ error.message || 'Could not load profile data. Please try again later.' }}
          </p>
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
                        <Link class="me-1" /> {{ linkCopied ? 'Copied!' : 'Link' }}
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
              <router-link v-if="adminUser" :to="{ name: 'profile', params: { slug: adminUser.slug } }">
                Go to the Profile Editor
              </router-link>
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
            <!-- FIX: Use standard btn-outline-secondary for consistency with other modals -->
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
  </div>
</template>

<script setup lang="ts">
/**
 * @file src/views/public/HomePage.vue
 * @description The main public-facing portfolio homepage for a user.
 * It displays the user's profile summary, social links, and provides actions
 * to download the portfolio in various formats. It also handles loading, error,
 * and private portfolio states.
 */
import {computed, onMounted, ref, watch, type Ref} from 'vue';
import { Lock, AlertTriangle, UserCircle, FileDown, Mail, Linkedin, Github, Globe, Twitter, Link, FileText, File, FileCode2 } from 'lucide-vue-next';
import {Modal} from 'bootstrap';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {authService} from '@/services/authService.js';
import type { Portfolio, User } from '@/types';
import {
  downloadMarkdownBySlug,
  downloadPublicPortfolioBySlug,
  downloadVCardBySlug
} from '@/services/api';
import {ApiError} from '@/services/api/ApiError.js';
import {getFilenameFromResponse, triggerDownload} from '@/utils/downloadUtils';
import {notificationService} from '@/services/notificationService.js';

// Modal components
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';

// --- State from Central Store ---
const {
  portfolio,
  isLoading,
  error,
  isPrivate,
  currentSlug,
} = usePublicPortfolioStore() as {
  portfolio: Ref<Portfolio | null>,
  isLoading: Ref<boolean>,
  error: Ref<ApiError | null>,
  isPrivate: Ref<boolean>,
  currentSlug: Ref<string | null>,
};

// --- Setup State & Computeds ---
const fullName = computed(() => {
  if (!portfolio.value?.user) return '';
  return `${portfolio.value.user.firstName || ''} ${portfolio.value.user.lastName || ''}`.trim();
});

const adminUser = computed(() => authService.user.value as User | null);

const isAdmin = computed(() => {
  return authService.isAuthenticated.value && adminUser.value?.roles?.includes('ADMIN');
});

// UAT-5 FIX: route-safe target for the admin tip card. The old name 'settings'
// was never registered in the router, so the RouterLink resolve computed threw
// a NO_MATCH error every time this block rendered. Guard with a fallback so it
// can never throw again even if the user object is briefly absent.
const adminSettingsTarget = computed(() => {
  const slug = adminUser.value?.slug;
  return slug ? {name: 'display-settings', params: {slug}} : {name: 'home'};
});

// --- Local UI State ---
const showCoverLetterModal = ref(false);
const coverLetterModalRef = ref<HTMLElement | null>(null);
const isDownloading = ref(false);
const errorModalMessage = ref('');
const linkCopied = ref(false);

// --- SEO Logic ---
const updateSeoTags = () => {
  if (portfolio.value?.profile) {
    const name = fullName.value;
    const headline = portfolio.value.profile.headline || 'Developer Portfolio';
    document.title = `${name} | ${headline} - ForkMyFolio`;

    // Update meta description
    const summary = portfolio.value.profile.summary || `View ${name}'s professional portfolio on ForkMyFolio.`;
    const metaDesc = document.querySelector('meta[name="description"]');
    if (metaDesc) {
      metaDesc.setAttribute('content', summary.substring(0, 160));
    }
  }
};

watch(() => portfolio.value, updateSeoTags, { immediate: true });
watch(() => fullName.value, updateSeoTags);

// --- Modal Instance Management ---
let coverLetterModalInstance: Modal | null = null;
onMounted(() => {
  if (coverLetterModalRef.value) {
    coverLetterModalInstance = new Modal(coverLetterModalRef.value);
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
const createDownloadHandler = (downloadFunc: (slug: string) => Promise<Response>, fileType: string, extension: string) => async () => {
  if (!currentSlug.value) return;
  isDownloading.value = true;
  errorModalMessage.value = ''; // Clear previous errors

  try {
    const response = await downloadFunc(currentSlug.value);
    const blob = await response.blob();
    const filename = getFilenameFromResponse(response, `${currentSlug.value}-portfolio.${extension}`);
    triggerDownload(blob, filename);
    notificationService.add({
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

const handleDownloadPdf = createDownloadHandler(downloadPublicPortfolioBySlug, 'pdf', 'pdf');
const handleDownloadMd = createDownloadHandler(downloadMarkdownBySlug, 'md', 'md');
const handleDownloadVcf = createDownloadHandler(downloadVCardBySlug, 'vcf', 'vcf');

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
    notificationService.add({
      message: 'Portfolio link copied to clipboard!',
      type: 'success'
    });
  } catch (err) {
    console.error('Failed to copy link:', err);
  }
};

</script>

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
