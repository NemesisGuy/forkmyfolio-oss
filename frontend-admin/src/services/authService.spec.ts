import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authService } from './authService';
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
