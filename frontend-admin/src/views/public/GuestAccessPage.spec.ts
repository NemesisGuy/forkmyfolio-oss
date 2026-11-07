import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import GuestAccessPage from './GuestAccessPage.vue';
import { authService } from '@/services/authService';
import { useRoute, useRouter, type RouteLocationNormalizedLoaded, type Router } from 'vue-router';

// NOTE (Phase 7.3): the previous version of this spec referenced `ref`
// inside a hoisted vi.mock factory (TDZ crash: "_vi_import_ before
// initialization") and asserted a router.push signature the component
// never used. Both were test-side drift, not app bugs; assertions now pin
// the CURRENT component behaviour (router.replace with guest query).
const userState = vi.hoisted(() => ({ value: null as { slug: string } | null }));

vi.mock('@/services/authService', () => ({
  authService: {
    loginAsGuest: vi.fn(),
    user: userState,
  },
}));

vi.mock('vue-router', () => ({
  useRoute: vi.fn(),
  useRouter: vi.fn(),
}));

describe('GuestAccessPage.vue', () => {
  let mockRouter: Partial<Router>;

  beforeEach(() => {
    vi.clearAllMocks();
    userState.value = null;
    mockRouter = { push: vi.fn(), replace: vi.fn() };
    vi.mocked(useRouter).mockReturnValue(mockRouter as Router);
  });

  it('shows error if no token in URL', async () => {
    vi.mocked(useRoute).mockReturnValue({ query: {} } as unknown as RouteLocationNormalizedLoaded);

    const wrapper = mount(GuestAccessPage, {
      global: {
        stubs: ['router-link']
      }
    });

    // verifyToken() runs asynchronously onMounted — wait for the error state
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('No access token was provided');
    });
    expect(authService.loginAsGuest).not.toHaveBeenCalled();
  });

  it('attempts guest login if token is present', async () => {
    vi.mocked(useRoute).mockReturnValue({ query: { token: 'valid-token' } } as unknown as RouteLocationNormalizedLoaded);
    vi.mocked(authService.loginAsGuest).mockImplementation(async () => {
      userState.value = { slug: 'test-user' };
    });

    mount(GuestAccessPage, {
      global: {
        stubs: ['router-link']
      }
    });

    expect(authService.loginAsGuest).toHaveBeenCalledWith('valid-token');
  });

  it('redirects (router.replace) to portfolio with guest query on successful login', async () => {
    vi.mocked(useRoute).mockReturnValue({ query: { token: 'valid-token' } } as unknown as RouteLocationNormalizedLoaded);
    vi.mocked(authService.loginAsGuest).mockImplementation(async () => {
      userState.value = { slug: 'test-user' };
    });

    mount(GuestAccessPage, {
      global: {
        stubs: ['router-link']
      }
    });

    // Wait for async auth logic
    await vi.waitFor(() => {
      expect(mockRouter.replace).toHaveBeenCalledWith({
        name: 'portfolio-home',
        params: { slug: 'test-user' },
        query: { guest: 'true' }
      });
    });
  });

  it('displays invalid-token state if token verification fails', async () => {
    vi.mocked(useRoute).mockReturnValue({ query: { token: 'invalid-token' } } as unknown as RouteLocationNormalizedLoaded);
    vi.mocked(authService.loginAsGuest).mockRejectedValue(new Error('Invalid token'));

    const wrapper = mount(GuestAccessPage, {
      global: {
        stubs: ['router-link']
      }
    });

    await vi.waitFor(() => {
      const titleEl = wrapper.find('[data-testid="error-title"]');
      expect(titleEl.text()).toContain('Invalid Token');
    });
    // Session loss message is shown
    expect(wrapper.text()).toContain('The provided token is not recognized by our system.');
  });
});
