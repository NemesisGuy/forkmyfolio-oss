/**
 * @file src/utils/downloadUtils.js
 * @description Reusable helper functions for handling file downloads.
 */

/**
 * Triggers a browser download for a given blob and filename.
 * @param {Blob} blob - The file content as a Blob.
 * @param {string} filename - The desired name for the downloaded file.
 */
export const triggerDownload = (blob, filename) => {
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
 * @param {Response} response - The Fetch API Response object.
 * @param {string} fallback - A fallback filename if the header is not present.
 * @returns {string} The extracted or fallback filename.
 */
export const getFilenameFromResponse = (response, fallback) => {
  const contentDisposition = response.headers.get('content-disposition');
  if (contentDisposition) {
    // --- THIS IS THE FIX ---
    // The old regex was too broad. This new version is more precise.
    // It first looks for a standard, quoted filename.
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
