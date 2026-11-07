/**
 * @file app/utils/downloadUtils.ts
 * @description Reusable helper functions for handling file downloads.
 * Ported from ForkMyFolio-frontend-vue/src/utils/downloadUtils.js.
 * Nuxt auto-imports functions from `app/utils`, no manual import needed.
 */

/**
 * Triggers a browser download for a given blob and filename.
 * @param blob - The file content as a Blob.
 * @param filename - The desired name for the downloaded file.
 */
export const triggerDownload = (blob: Blob, filename: string): void => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.style.display = 'none';
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
};

/**
 * Extracts the filename from the 'content-disposition' header of a Response object.
 * @param response - The Fetch API Response object.
 * @param fallback - A fallback filename if the header is not present.
 * @returns The extracted or fallback filename.
 */
export const getFilenameFromResponse = (response: Response, fallback: string): string => {
  const contentDisposition = response.headers.get('content-disposition');
  if (contentDisposition) {
    // Looks first for a standard, quoted filename.
    const quotedMatch = contentDisposition.match(/filename="([^"]+)"/);
    if (quotedMatch && quotedMatch.length > 1) {
      // Return the captured group, which is the text inside the quotes.
      return quotedMatch[1].trim();
    }

    // As a fallback, it looks for an unquoted filename.
    const unquotedMatch = contentDisposition.match(/filename=([^;]+)/);
    if (unquotedMatch && unquotedMatch.length > 1) {
      return unquotedMatch[1].trim();
    }
  }
  // If no filename is found in the header, use the provided fallback.
  return fallback;
};
