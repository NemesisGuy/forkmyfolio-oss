import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authService } from './authService';
import { logout as apiLogout, refreshToken as apiRefreshToken } from './api/auth.api';
import { getMyAccount } from './api/user.api';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore';
import { ApiError } from '@/services/api/ApiError';

// authService pulls in the real API layer; nothing here should hit the network.
vi.mock('./api/auth.api', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  refreshToken: vi.fn(),
  register: vi.fn(),
}));
vi.mock('./api/user.api', () => ({
  getMyAccount: vi.fn(),
}));
vi.mock('./guestPassService', () => ({
  guestPassService: { login: vi.fn() },
}));

/**
 * clearLocalSession() is what the apiClient calls when a GUEST session gets a
 * 401 (revoked/expired pass). The router guard reads `store.error` AFTER
 * fetchPortfolio settles to decide whether to bounce the guest to
 * /guest-access?error=invalid — so wiping the error here silently disabled
 * the kick and left a revoked recruiter sitting on the portfolio.
 */
describe('authService.clearLocalSession (guest-pass kick)', () => {
  const store = usePublicPortfolioStore();

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    store.clearPortfolio();
  });

  it('preserves a pending fetch error so the router guard can still kick a revoked guest', () => {
    const kickError = new ApiError('Guest session expired or pass revoked.', 401);
    store.portfolio.value = { profile: { fullName: 'Guest Owner' } } as never;
    store.currentSlug.value = 'guest-owner';
    store.isPrivate.value = true;
    store.error.value = kickError;

    authService.clearLocalSession();

    // The error MUST survive — this is the signal the guard acts on.
    expect(store.error.value).toBe(kickError);
    expect(store.error.value?.httpStatus).toBe(401);
  });

  it('still clears portfolio data, slug and privacy flag (no stale content survives)', () => {
    store.portfolio.value = { profile: { fullName: 'Guest Owner' } } as never;
    store.currentSlug.value = 'guest-owner';
    store.isPrivate.value = true;
    store.error.value = new ApiError('Guest session expired or pass revoked.', 401);

    authService.clearLocalSession();

    expect(store.portfolio.value).toBeNull();
    expect(store.currentSlug.value).toBeNull();
    expect(store.isPrivate.value).toBe(false);
    expect(store.isLoading.value).toBe(false);
  });

  it('does not invent an error when none was pending', () => {
    authService.clearLocalSession();

    expect(store.error.value).toBeNull();
  });

  it('clears the auth token alongside the store', () => {
    localStorage.setItem('accessToken', 'stale-token');
    sessionStorage.setItem('accessToken', 'stale-guest-token');

    authService.clearLocalSession();

    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('accessToken')).toBeNull();
    expect(authService.isAuthenticated.value).toBe(false);
    expect(authService.getAccessToken()).toBeNull();
  });
});

/**
 * Social/OIDC handoff: the backend issues the app JWT and the SPA consumes it
 * exactly as it would a password-login token (token -> getMyAccount -> state).
 */
describe('authService.loginWithToken (OAuth2 redirect handoff)', () => {
  const account = { slug: 'oidc-user', roles: ['USER'] };

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
    authService.clearLocalSession();
  });

  it('stores the token, fetches the account and publishes authenticated state', async () => {
    vi.mocked(getMyAccount).mockResolvedValue(account as never);

    await authService.loginWithToken('callback-jwt');

    expect(getMyAccount).toHaveBeenCalledOnce();
    expect(authService.getAccessToken()).toBe('callback-jwt');
    expect(localStorage.getItem('accessToken')).toBe('callback-jwt');
    expect(sessionStorage.getItem('accessToken')).toBeNull();
    expect(authService.isAuthenticated.value).toBe(true);
    expect(authService.user.value?.slug).toBe('oidc-user');
  });

  it('clears the half-established session and rethrows when the account fetch fails', async () => {
    vi.mocked(getMyAccount).mockRejectedValue(new ApiError('Account unavailable', 500));

    await expect(authService.loginWithToken('callback-jwt')).rejects.toThrow('Account unavailable');

    expect(authService.getAccessToken()).toBeNull();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(authService.isAuthenticated.value).toBe(false);
    expect(authService.user.value).toBeNull();
  });

  it('rejects a blank token without fetching an account', async () => {
    await expect(authService.loginWithToken('   ')).rejects.toThrow('Missing token');

    expect(getMyAccount).not.toHaveBeenCalled();
    expect(authService.isAuthenticated.value).toBe(false);
  });
});

/**
 * Sticky logout (staging regression): after an explicit logout, initAuth() must
 * not silently re-authenticate from a surviving HttpOnly refresh cookie.
 */
describe('authService sticky logout', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
    authService.clearLocalSession();
  });

  it('marks logout and skips the silent refresh on the next init', async () => {
    await authService.logout();

    expect(apiLogout).toHaveBeenCalledOnce();
    expect(localStorage.getItem('fmf_logged_out')).toBe('true');
    expect(localStorage.getItem('accessToken')).toBeNull();

    await authService.initAuth();

    expect(apiRefreshToken).not.toHaveBeenCalled();
    expect(authService.isAuthenticated.value).toBe(false);
    expect(authService.user.value).toBeNull();
  });

  it('clears the marker on an explicit login so refresh works again', async () => {
    localStorage.setItem('fmf_logged_out', 'true');
    vi.mocked(getMyAccount).mockResolvedValue({ slug: 'again' } as never);

    await authService.loginWithToken('new-token');

    expect(localStorage.getItem('fmf_logged_out')).toBeNull();
    expect(authService.isAuthenticated.value).toBe(true);
  });
});
