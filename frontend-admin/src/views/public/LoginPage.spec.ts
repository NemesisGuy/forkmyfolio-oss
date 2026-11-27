import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import LoginPage from './LoginPage.vue';

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}));

const mountPage = () => mount(LoginPage, {
  global: {
    stubs: {
      RouterLink: { template: '<a><slot /></a>' },
      SuccessModal: true,
      ErrorModal: true,
    },
  },
});

describe('LoginPage social login buttons', () => {
  beforeEach(() => {
    window.runtimeConfig = undefined;
  });

  afterEach(() => {
    window.runtimeConfig = undefined;
  });

  it('renders no social buttons when runtime flags are absent', () => {
    const wrapper = mountPage();
    expect(wrapper.find('a[href^="/oauth2/authorize/"]').exists()).toBe(false);
    wrapper.unmount();
  });

  it('renders the Keycloak button pointing at the backend authorize endpoint when enabled', () => {
    window.runtimeConfig = {
      SOCIAL_LOGIN_ENABLED: 'true',
      SOCIAL_PROVIDERS: 'keycloak',
    };

    const wrapper = mountPage();
    const button = wrapper.find('a[href^="/oauth2/authorize/keycloak?"]');
    expect(button.exists()).toBe(true);
    expect(button.text()).toBe('Sign in with SSO');
    expect(button.attributes('href')).toContain('redirect_uri=');
    expect(decodeURIComponent(button.attributes('href') || '')).toContain('/oauth2/redirect');
    wrapper.unmount();
  });

  it('renders nothing when the flag is false even if a provider id is present', () => {
    window.runtimeConfig = {
      SOCIAL_LOGIN_ENABLED: 'false',
      SOCIAL_PROVIDERS: 'keycloak',
    };

    const wrapper = mountPage();
    expect(wrapper.find('a[href^="/oauth2/authorize/"]').exists()).toBe(false);
    wrapper.unmount();
  });

  it('offers a "Use a different account" link that forces prompt=login', () => {
    window.runtimeConfig = {
      SOCIAL_LOGIN_ENABLED: 'true',
      SOCIAL_PROVIDERS: 'keycloak',
    };

    const wrapper = mountPage();
    const link = wrapper.findAll('a').find(a => a.text().includes('Use a different account'));
    expect(link).toBeDefined();
    expect(decodeURIComponent(link?.attributes('href') || '')).toContain('prompt=login');
    wrapper.unmount();
  });
});
