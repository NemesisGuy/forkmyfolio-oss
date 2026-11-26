import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import {
  getSocialLoginConfig,
  getSocialProviderLabel,
  parseBooleanFlag,
  parseProviderIds,
} from './runtimeConfig';

describe('runtimeConfig social-login parsing (non-secret flags only)', () => {
  beforeEach(() => {
    window.runtimeConfig = undefined;
  });

  afterEach(() => {
    window.runtimeConfig = undefined;
  });

  it('enables a flag only for an explicit true (deny by default)', () => {
    expect(parseBooleanFlag(true)).toBe(true);
    expect(parseBooleanFlag('true')).toBe(true);
    expect(parseBooleanFlag(' TRUE ')).toBe(true);
    expect(parseBooleanFlag(false)).toBe(false);
    expect(parseBooleanFlag('false')).toBe(false);
    expect(parseBooleanFlag(undefined)).toBe(false);
    expect(parseBooleanFlag(null)).toBe(false);
    expect(parseBooleanFlag(1)).toBe(false);
    expect(parseBooleanFlag('yes')).toBe(false);
  });

  it('treats unsubstituted placeholders as disabled/empty', () => {
    expect(parseBooleanFlag('##VITE_SOCIAL_LOGIN_ENABLED##')).toBe(false);
    expect(parseProviderIds('##VITE_SOCIAL_PROVIDERS##')).toEqual([]);
  });

  it('parses, normalises and de-duplicates known provider ids, ignoring unknown ones', () => {
    expect(parseProviderIds(' keycloak , KEYCLOAK, google, keycloak ')).toEqual(['keycloak']);
    expect(parseProviderIds('google,github')).toEqual([]);
    expect(parseProviderIds('')).toEqual([]);
    expect(parseProviderIds(undefined)).toEqual([]);
  });

  it('reads both flags from window.runtimeConfig', () => {
    window.runtimeConfig = {
      API_BASE_URL: '/api/v1',
      SOCIAL_LOGIN_ENABLED: 'true',
      SOCIAL_PROVIDERS: 'keycloak',
    };

    expect(getSocialLoginConfig()).toEqual({ enabled: true, providers: ['keycloak'] });
  });

  it('stays disabled when config.js was not loaded or flags are placeholders', () => {
    expect(getSocialLoginConfig()).toEqual({ enabled: false, providers: [] });

    window.runtimeConfig = {
      SOCIAL_LOGIN_ENABLED: '##VITE_SOCIAL_LOGIN_ENABLED##',
      SOCIAL_PROVIDERS: '##VITE_SOCIAL_PROVIDERS##',
    };
    expect(getSocialLoginConfig()).toEqual({ enabled: false, providers: [] });
  });

  it('uses a vendor-neutral button label (does not disclose the IdP)', () => {
    expect(getSocialProviderLabel('keycloak')).toBe('Sign in with SSO');
  });
});
