import {ApiError} from './ApiError';

/**
 * @file src/services/api/apiClient.ts
 * @description Centralized API client for making requests to the backend using the Fetch API.
 */

export interface AuthService {
  getAccessToken(): string | null;
  refreshToken(): Promise<boolean>;
  clearLocalSession(): void;
}

interface FetchOptions extends Omit<RequestInit, 'body'> {
  responseType?: 'json' | 'raw' | 'blob';
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  body?: any; // Allow anything for the options; we handle serialization internally.
}

declare global {
  interface Window {
    runtimeConfig?: {
      API_BASE_URL?: string;
    };
  }
}

// This will be injected by initAuth.js to break the circular dependency.
let authService: AuthService | null = null;

/**
 * Injects the authService into the apiClient.
 * @param {AuthService} service - The authentication service.
 */
export function setAuthService(service: AuthService) {
  authService = service;
}

// --- KEY CHANGE: Read the Base URL from Environment Variables ---
// This logic correctly handles both Docker runtime configuration and local development.
// 1. In Docker, `entrypoint.sh` replaces "##VITE_API_BASE_URL##" with the real URL.
// 2. In local dev, the placeholder remains, so we check for it and provide a fallback.
const runtimeUrl = window.runtimeConfig?.API_BASE_URL;
const VITE_API_BASE_URL = (runtimeUrl && !runtimeUrl.startsWith('##'))
  ? runtimeUrl // Use the valid runtime URL from the Docker environment.
  : '/api/v1'; // Fallback for local development and unified origin.


/**
 * A wrapper around the Fetch API that handles authentication, API response structure, and error handling.
 * @param {string} endpoint The API endpoint to call (e.g., '/users').
 * @param {FetchOptions} [options={}] Fetch options (method, body, etc.).
 * @param {boolean} [requiresAuth=true] Whether the endpoint requires an Authorization header.
 * @param {boolean} [isRetry=false] Internal flag to prevent infinite refresh loops.
 * @returns {Promise<any>} The `data` from the API response, a Blob, or the raw Response object.
 * @throws {ApiError} If the API returns a non-success status.
 */
export async function fetchWithAuth<T = unknown>(
  endpoint: string,
  options: FetchOptions = {},
  requiresAuth = true,
  isRetry = false
): Promise<T> {
  const {responseType = 'json', ...fetchOptions} = options;

  const headers = new Headers((fetchOptions.headers as Record<string, string>) || {});
  if (requiresAuth && authService?.getAccessToken()) {
    headers.append('Authorization', `Bearer ${authService.getAccessToken()}`);
  }

  if (fetchOptions.body && !(fetchOptions.body instanceof FormData)) {
    headers.append('Content-Type', 'application/json');
    fetchOptions.body = JSON.stringify(fetchOptions.body);
  }

  const config: RequestInit = {
    ...fetchOptions,
    headers,
    credentials: 'include',
  };

  // --- TIMEOUT CAPACITY ---
  // Global 15s cap so no single call can hang the UI forever. A caller may pass
  // its own AbortSignal (e.g. the short 5s logout timeout in auth.api.ts) —
  // respect it instead of overwriting it.
  let timeoutId: ReturnType<typeof setTimeout> | undefined;
  if (fetchOptions.signal) {
    config.signal = fetchOptions.signal; // caller owns the signal lifecycle
  } else {
    const controller = new AbortController();
    timeoutId = setTimeout(() => controller.abort(), 15000); // 15s timeout
    config.signal = controller.signal;
  }

  try {
    const response = await fetch(`${VITE_API_BASE_URL}${endpoint}`, config);
    clearTimeout(timeoutId);

    if (response.status === 401 && requiresAuth && !isRetry) {
      // GUEST SESSION FIX: If this is a guest session, do not attempt to refresh.
      // Guest tokens are one-off (effectively tab-bound access tokens) and don't
      // have a corresponding refresh token cookie.
      const isGuest = sessionStorage.getItem('isGuestSession') === 'true';

      if (isGuest) {
        console.warn('[ApiClient] 401 Unauthorized for guest session. Skipping refresh and clearing session.');
        authService?.clearLocalSession();
        throw new ApiError('Guest session expired or pass revoked.', 401, []);
      }

      try {
        await authService?.refreshToken();
        return fetchWithAuth(endpoint, options, true, true);
      } catch {
        // FIX: When a token refresh fails, we must clear the local session state to
        // prevent an infinite loop, but we should not navigate here. Instead, we
        // re-throw the error so the original caller (e.g., the UI component) can
        // handle the navigation gracefully.
        authService?.clearLocalSession();
        throw new ApiError('Session expired. Please log in again.', 401, []);
      }
    }

    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch {
        throw new ApiError(`HTTP error! Status: ${response.status}`, response.status);
      }
      throw new ApiError(
        errorData.errors?.[0]?.message || 'An unknown API error occurred.',
        response.status,
        errorData.errors || []
      );
    }

    // --- SUCCESS HANDLING ---
    // Handle different response types based on the 'responseType' option.
    if (responseType === 'raw') {
      return response as unknown as T; // Return the entire Response object for manual handling.
    }

    if (responseType === 'blob') {
      return (await response.blob()) as unknown as T; // Return the response body as a Blob.
    }

    // Handle successful responses with no content.
    if (response.status === 204) {
      return null as unknown as T;
    }

    // Default to JSON parsing for all other successful responses.
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      const responseData = await response.json();
      if (responseData.status === 'success') {
        return responseData.data as T;
      } else {
        throw new ApiError(
          responseData.errors?.[0]?.message || 'API returned a non-success status.',
          response.status,
          responseData.errors || []
        );
      }
    } else {
      const text = await response.text();
      return (text ? text : null) as unknown as T;
    }
  } catch (error: unknown) {
    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiError('Request timed out. The server might be overloaded.', 408);
    }
    throw error;
  }
}
