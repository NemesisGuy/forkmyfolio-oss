/**
 * @file src/services/authService.ts
 * @description Manages authentication state, including login, logout, token storage, and session initialization.
 * This service acts as the central hub for authentication logic and provides a reactive state.
 */
import { ref, type Ref } from 'vue';
import type { User } from '@/types';

interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
}

// Import the specific API functions
import {
  login as apiLogin,
  logout as apiLogout,
  refreshToken as apiRefreshToken,
  register as apiRegister
} from './api/auth.api';
import { getMyAccount } from './api/user.api';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore';
import { guestPassService } from './guestPassService';

// --- Reactive State ---
/**
 * A reactive flag indicating if the user is currently authenticated.
 */
const isAuthenticated = ref(false);
/**
 * A reactive object containing the authenticated user's data. Null if not authenticated.
 */
const user: Ref<User | null> = ref(null);
/**
 * A reactive flag that is true only during the initial session restoration process on app load.
 */
const isLoading = ref(true);

// --- KEY CHANGE: Persist accessToken in localStorage (Normal) or sessionStorage (Guest) ---
let accessToken: string | null = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');

// --- Token Refresh State ---
let isRefreshing = false;
let refreshPromise: Promise<boolean> | null = null;

// --- Private Functions ---

/**
 * Updates the authentication state.
 * @param token - The new access token.
 * @param userData - The user data object.
 * @param isGuest - Whether this is a guest session.
 */
function _updateAuthState(token: string | null, userData: User | null, isGuest: boolean = false): void {
  accessToken = token;
  if (token) {
    if (isGuest) {
      sessionStorage.setItem('accessToken', token);
      sessionStorage.setItem('isGuestSession', 'true');
      localStorage.removeItem('accessToken'); // Ensure we don't mix them
    } else {
      localStorage.setItem('accessToken', token);
      sessionStorage.removeItem('accessToken');
      sessionStorage.removeItem('isGuestSession');
    }
  } else {
    localStorage.removeItem('accessToken');
    sessionStorage.removeItem('accessToken');
  }
  isAuthenticated.value = !!token;
  user.value = userData;
  console.log(`[AuthService] Auth state updated. Authenticated: ${isAuthenticated.value}, Guest: ${isGuest}`);
}

/**
 * Clears the authentication state.
 */
function _clearAuthState(): void {
  console.log('[AuthService] Clearing auth state.');
  accessToken = null;
  localStorage.removeItem('accessToken');
  sessionStorage.removeItem('accessToken');
  isAuthenticated.value = false;
  user.value = null;
  isLoading.value = false;
}

/**
 * Clears all local session data without making an API call.
 * This is used internally by the apiClient to prevent infinite loops on auth failure.
 */
function clearLocalSession(): void {
  _clearAuthState();
  // Reset the portfolio store to clear any previous user's data.
  const portfolioStore = usePublicPortfolioStore();
  portfolioStore.clearPortfolio();
}

/**
 * Manually updates the local user state after a successful password change.
 * This prevents the navigation guard from re-triggering a redirect.
 */
function passwordHasBeenChanged(): void {
  if (user.value) {
    // Set a non-null value to satisfy the router guard. The next full page
    // reload or re-login will fetch the real timestamp from the backend.
    user.value.passwordLastChangedAt = new Date().toISOString();
    console.log('[AuthService] passwordLastChangedAt flag updated locally to prevent re-redirect.');
  }
}

/**
 * Manually updates the local user state after a successful terms acceptance.
 * This prevents the navigation guard from re-triggering a redirect.
 */
function termsHaveBeenAccepted(): void {
  if (user.value) {
    // Set a non-null value to satisfy the router guard. The next full page
    // reload or re-login will fetch the real timestamp from the backend.
    user.value.termsAcceptedAt = new Date().toISOString();
    console.log('[AuthService] termsAcceptedAt flag updated locally to prevent re-redirect.');
  }
}

// --- Public API for the Service ---

/**
 * Logs in a user and establishes their session.
 * @param credentials - An object containing login credentials.
 */
async function login(credentials: Record<string, unknown>): Promise<void> {
  const loginResponse = await apiLogin(credentials) as unknown as AuthResponse;

  // Temporarily set the token so the next API call is authenticated.
  accessToken = loginResponse.accessToken;

  const freshUserAccount = await getMyAccount();

  // Now, formally update the application's state with the token and the complete user object.
  _updateAuthState(loginResponse.accessToken, freshUserAccount as User);
}

/**
 * Logs out the user from the backend and clears local state.
 */
async function logout(): Promise<void> {
  try {
    await apiLogout();
  } catch (e) {
    console.error('[AuthService] Backend logout failed, clearing state anyway.', e);
  } finally {
    sessionStorage.removeItem('isGuestSession'); // Explicit logout clears guest state
    clearLocalSession();
  }
}

/**
 * Registers a new user and logs them in.
 * @param userData - An object containing account details.
 */
async function register(userData: Record<string, unknown>): Promise<void> {
  const registerResponse = await apiRegister(userData) as unknown as AuthResponse;

  accessToken = registerResponse.accessToken;

  const freshUserAccount = await getMyAccount();

  _updateAuthState(registerResponse.accessToken, freshUserAccount as User);
}

/**
 * Logs in a guest using a secure pass token.
 * This session is stored in sessionStorage (tab-bound).
 * @param token - The guest token.
 */
async function loginAsGuest(token: string): Promise<void> {
  console.log('[AuthService] Initiating guest login...');
  const response = await guestPassService.login(token) as unknown as AuthResponse;

  accessToken = response.accessToken;

  try {
    const freshUserAccount = await getMyAccount();
    _updateAuthState(response.accessToken, freshUserAccount as User, true);
    console.log('[AuthService] Guest login successful.');
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    console.error('[AuthService] Failed to fetch profile for guest session:', message);
    _clearAuthState();
    throw error;
  }
}

/**
 * Refreshes the access token using the HttpOnly refresh token cookie.
 * Manages a single refresh promise to prevent race conditions.
 */
async function refreshToken(): Promise<boolean> {
  if (isRefreshing && refreshPromise) {
    return refreshPromise;
  }

  isRefreshing = true;
  refreshPromise = (async () => {
    try {
      const response = await apiRefreshToken() as unknown as AuthResponse;
      accessToken = response.accessToken;
      isAuthenticated.value = true; // We have a new token
      console.log('[AuthService] Token refresh successful.');
      return true;
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Unknown error';
      console.error('[AuthService] Token refresh failed:', message);
      _clearAuthState();
      throw e;
    } finally {
      isRefreshing = false;
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

/**
 * Initializes the authentication state on application startup.
 * It attempts to restore a session by refreshing the token and fetching user data.
 */
async function initAuth(): Promise<void> {
  console.log('[AuthService] Initializing session...');
  isLoading.value = true;
  try {
    // 1. If we have an accessToken in localStorage, try to use it first.
    if (accessToken) {
      console.log('[AuthService] Found persisted access token, verifying...');
      try {
        const freshUserAccount = await getMyAccount();
        _updateAuthState(accessToken, freshUserAccount as User);
        console.log('[AuthService] Session restored using persisted token.');
        return; // Success!
      } catch {
        console.log('[AuthService] Persisted token invalid or expired. Attempting refresh...');
        // Fall through to refresh logic
      }
    }

    // 2. Attempt to refresh the token to see if a session exists (via HttpOnly cookie).
    await refreshToken();
    if (isAuthenticated.value) {
      // If the session is valid, fetch the latest user data to ensure it's fresh.
      const freshUserAccount = await getMyAccount();
      _updateAuthState(accessToken, freshUserAccount as User);
      console.log('[AuthService] Session restored via refresh token.');
    }
  } catch (_error: unknown) {
    const message = _error instanceof Error ? _error.message : 'Unknown error';
    console.log('[AuthService] No active session found or refresh failed:', message);
  } finally {
    isLoading.value = false;
    console.log(
      `[AuthService] Session initialization complete. User is ${isAuthenticated.value ? 'authenticated' : ' not authenticated'}.`
    );
  }
}

export const authService = {
  // State
  isAuthenticated,
  user,
  isLoading,
  // Getters
  getAccessToken: () => accessToken,
  // Actions
  login,
  loginAsGuest,
  logout,
  register,
  refreshToken,
  initAuth,
  // For internal use by apiClient
  clearLocalSession,
  // For use after forced password change
  passwordHasBeenChanged,
  // For use after terms acceptance
  termsHaveBeenAccepted,
};
