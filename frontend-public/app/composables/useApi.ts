import { useRuntimeConfig } from '#app';

/**
 * @file app/composables/useApi.ts
 * @description Thin fetch wrapper for the backend API, mirroring the original
 * apiClient behavior (`{ data: ... }` envelope unwrapping, raw-response support).
 */

export function useApi() {
  const config = useRuntimeConfig();
  const baseUrl = process.server
    ? config.apiBaseUrl
    : config.public.apiBaseUrl;

  const buildUrl = (path: string) => `${baseUrl}${path.startsWith('/') ? path : `/${path}`}`;

  const fetchJson = async <T = unknown>(path: string, options?: RequestInit): Promise<T> => {
    const res = await fetch(buildUrl(path), {
      headers: {
        'Content-Type': 'application/json',
        ...(options?.headers || {})
      },
      ...options
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`API error ${res.status}: ${text}`);
    }
    const json = await res.json();
    // API responses are wrapped in { data: ... }
    return (json?.data !== undefined ? json.data : json) as T;
  };

  /**
   * Performs a request and returns the raw Response (for blob downloads that
   * need the content-disposition header). Ported from apiClient responseType:'raw'.
   */
  const fetchRaw = async (path: string, options?: RequestInit): Promise<Response> => {
    return fetch(buildUrl(path), {
      headers: options?.headers || {},
      ...options
    });
  };

  return { baseUrl, fetchJson, fetchRaw };
}
