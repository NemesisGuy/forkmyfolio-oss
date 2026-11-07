/**
 * @file src/services/api/backup.api.js
 * @description API functions for data backup and restore operations.
 * Adheres to the Nemesis Architecture Style Guide.
 */
import {fetchWithAuth} from './apiClient';
// FIX: Import utilities to handle file downloads correctly.
import {getFilenameFromResponse, triggerDownload} from '@/utils/downloadUtils';

/**
 * Initiates a download of the full portfolio backup.
 * @returns {Promise<void>}
 */
export const downloadBackup = async () => {
  // FIX: The /admin/backup endpoint returns a raw file, not a standard JSON response.
  // We must request it with `responseType: 'raw'` to get the full Response object
  // and handle the file download manually.
  const response = await fetchWithAuth('/admin/backup', {
    method: 'GET',
    responseType: 'raw'
  });

  const blob = await response.blob();
  const filename = getFilenameFromResponse(response, `portfolio-backup-${new Date().toISOString().split('T')[0]}.json`);
  triggerDownload(blob, filename);
};

/**
 * Uploads a backup file to restore the portfolio.
 * @param {File} file The JSON backup file to upload.
 * @returns {Promise<any>} A promise that resolves with the server's response.
 */
export const ingestBackup = (file) => {
  const formData = new FormData();
  formData.append('file', file);

  return fetchWithAuth('/admin/ingest', {
    method: 'POST',
    body: formData,
    // Note: 'Content-Type' for multipart/form-data is set automatically by the browser
    // when using FormData and should not be specified manually.
  });
};
