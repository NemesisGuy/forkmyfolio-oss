import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import SignupPage from './SignupPage.vue';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));
vi.mock('@/services/authService', () => ({
  authService: { register: vi.fn(), isAuthenticated: { value: false }, user: { value: null } },
}));
vi.mock('@/services/settingsService', () => ({ settingsService: { initialize: vi.fn() } }));
vi.mock('@/stores/publicPortfolioStore', () => ({
  usePublicPortfolioStore: () => ({ currentSlug: { value: null } }),
}));
vi.mock('@/services/api/policy.api', () => ({
  getTermsOfService: vi.fn().mockResolvedValue({ version: 'test-terms' }),
}));

const mountPage = () => mount(SignupPage, {
  global: {
    stubs: {
      RouterLink: { template: '<a><slot /></a>' },
      SuccessModal: true,
      ErrorModal: true,
      TermsAgreementModal: true,
    },
  },
});

describe('SignupPage SSO alternative', () => {
  beforeEach(() => { window.runtimeConfig = undefined; });
  afterEach(() => { window.runtimeConfig = undefined; });

  it('shows no SSO alternative by default', () => {
    const wrapper = mountPage();
    expect(wrapper.find('a[href^="/oauth2/authorize/"]').exists()).toBe(false);
    wrapper.unmount();
  });

  it('offers a vendor-neutral SSO alternative only when enabled', () => {
    window.runtimeConfig = { SOCIAL_LOGIN_ENABLED: 'true', SOCIAL_PROVIDERS: 'keycloak' };
    const wrapper = mountPage();
    const button = wrapper.find('a[href^="/oauth2/authorize/keycloak?"]');
    expect(button.exists()).toBe(true);
    expect(button.text()).toBe('Sign in with SSO');
    expect(button.text()).not.toContain('Keycloak');
    expect(decodeURIComponent(button.attributes('href') || '')).toContain('/oauth2/redirect');
    wrapper.unmount();
  });
});
