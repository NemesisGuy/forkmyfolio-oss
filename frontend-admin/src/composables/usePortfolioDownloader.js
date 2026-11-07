/**
 * @file src/composables/usePortfolioDownloader.js
 * @description A Vue composable to handle the logic for downloading a public PDF portfolio.
 */

import {ref, unref} from 'vue';
import {downloadPublicPortfolioBySlug} from '@/services/api';
import {getFilenameFromResponse, triggerDownload} from '@/utils/downloadUtils';

/**
 * A composable to handle the logic for downloading a public portfolio PDF.
 * @param {import('vue').Ref<string> | import('vue').ComputedRef<string>} slugRef - A ref or computed ref containing the portfolio slug.
 */
export function usePortfolioDownloader(slugRef) {
  const isDownloadingPdf = ref(false);
  const showSuccessModal = ref(false);
  const successModalMessage = ref('');
  const showErrorModal = ref(false);
  const errorModalMessage = ref('');

  const handleDownloadPdf = async () => {
    const slug = unref(slugRef); // Get the current value from the ref or computed ref
    if (!slug) {
      console.error('[Downloader] Aborted: No portfolio slug is available.');
      errorModalMessage.value = 'Cannot download PDF. No portfolio slug is available.';
      showErrorModal.value = true;
      return;
    }

    console.log(`[Downloader] Starting PDF download for slug: ${slug}`);
    isDownloadingPdf.value = true;
    try {
      // 1. Await the raw HTTP response from the API call.
      const response = await downloadPublicPortfolioBySlug(slug);
      console.log('[Downloader] Received raw response from API.', response);

      // 2. THE FIX: Correctly extract the Blob data from the response body.
      const blob = await response.blob();
      console.log(`[Downloader] Extracted blob from response. Type: ${blob.type}, Size: ${blob.size}`);

      // 3. Extract the filename from headers, with a sensible fallback.
      const filename = getFilenameFromResponse(response, `${slug}-portfolio.pdf`);
      console.log(`[Downloader] Determined filename: ${filename}`);

      // 4. Use the utility to trigger the download with the correct Blob and filename.
      triggerDownload(blob, filename);
      console.log('[Downloader] Download triggered successfully.');

      successModalMessage.value = `The download for "${filename}" has started.`;
      showSuccessModal.value = true;

    } catch (err) {
      console.error('[Downloader] PDF download failed:', err);
      errorModalMessage.value = err.message || 'An unexpected error occurred while preparing your download.';
      showErrorModal.value = true;
    } finally {
      isDownloadingPdf.value = false;
      console.log('[Downloader] Download process finished.');
    }
  };

  return {
    isDownloadingPdf,
    showSuccessModal,
    successModalMessage,
    showErrorModal,
    errorModalMessage,
    handleDownloadPdf,
  };
}
