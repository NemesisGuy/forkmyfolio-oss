<template>
  <div class="data-exports-page py-5 flex-grow-1">
    <div class="container">
      <!-- Header -->
      <div class="text-center mb-5 animate-fade-in-up">
        <h2 class="display-5 glass-text">Data & Exports</h2>
        <p class="lead glass-subtitle" style="animation-delay: 0.1s;">
          Download your portfolio in various formats.
        </p>
      </div>

      <!-- Full screen loading modal -->
      <LoadingModal :visible="isLoading"/>

      <!-- Modal for error feedback -->
      <ErrorModal :message="errorModalMessage" :visible="!!errorModalMessage" title="Download Failed" @close="errorModalMessage = ''"/>

      <div class="row g-4 justify-content-center">
        <!-- PDF Card -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.2s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3"><FileText class="display-4 text-danger" />
              </div>
              <h5 class="card-title glass-title">PDF Portfolio</h5>
              <p class="card-text glass-subtitle small flex-grow-1">A professional, print-ready PDF
                version of your portfolio.</p>
              <button :disabled="isLoading" class="btn btn-danger mt-auto interactive-lift"
                      @click="handleDownloadPdf">
                Download PDF
              </button>
            </div>
          </div>
        </div>

        <!-- Markdown Card -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.3s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3"><FileCode class="display-4 text-info" /></div>
              <h5 class="card-title glass-title">Markdown File</h5>
              <p class="card-text glass-subtitle small flex-grow-1">A plain-text Markdown file,
                perfect for version control or static site generators.</p>
              <button :disabled="isLoading" class="btn btn-info mt-auto interactive-lift"
                      @click="handleDownloadMd">
                Download .md
              </button>
            </div>
          </div>
        </div>

        <!-- vCard Card -->
        <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.4s;">
          <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
            <div class="card-body d-flex flex-column text-center p-4">
              <div class="mb-3"><UserCircle class="display-4 text-success" /></div>
              <h5 class="card-title glass-title">vCard Contact</h5>
              <p class="card-text glass-subtitle small flex-grow-1">A standard vCard (.vcf) file
                that can be easily imported into contact applications.</p>
              <button :disabled="isLoading" class="btn btn-success mt-auto interactive-lift"
                      @click="handleDownloadVcf">
                Download .vcf
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import { FileText, FileCode, UserCircle } from 'lucide-vue-next';
import {authService} from '@/services/authService';
import {
  downloadMarkdownBySlug,
  downloadPublicPortfolioBySlug,
  downloadVCardBySlug
} from '@/services/api';
import {getFilenameFromResponse, triggerDownload} from '@/utils/downloadUtils';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import {notificationService} from '@/services/notificationService';
import type { User } from '@/types';

const currentSlug = computed(() => (authService.user.value as User | null)?.slug);

// State for feedback
const errorModalMessage = ref(''); // Use a string to control visibility

// A single loading state for the full-screen modal
const isLoading = ref(false);

const createDownloadHandler = (downloadFunc: (slug: string) => Promise<Response>, fileType: string, extension: string) => async () => {
  if (!currentSlug.value) return;

  isLoading.value = true;

  try {
    const response = await downloadFunc(currentSlug.value);
    const blob = await response.blob();
    const filename = getFilenameFromResponse(response, `${currentSlug.value}-portfolio.${extension}`);
    triggerDownload(blob, filename);
    // Use a non-blocking toast for success, just like the backup page
    notificationService.add({
      message: `${fileType.toUpperCase()} download has started.`,
      type: 'success'
    });
  } catch (err: unknown) {
    const error = err as Error;
    console.error(`${fileType} download failed:`, error);
    errorModalMessage.value = error.message || `An unexpected error occurred while downloading the ${fileType.toUpperCase()} file.`;
  } finally {
    isLoading.value = false;
  }
};

const handleDownloadPdf = createDownloadHandler(downloadPublicPortfolioBySlug, 'pdf', 'pdf');
const handleDownloadMd = createDownloadHandler(downloadMarkdownBySlug, 'md', 'md');
const handleDownloadVcf = createDownloadHandler(downloadVCardBySlug, 'vcf', 'vcf');
</script>

<style scoped>
.data-exports-page .display-5 {
  font-weight: 300;
}

.card-title i {
  vertical-align: -0.125em;
}
</style>
