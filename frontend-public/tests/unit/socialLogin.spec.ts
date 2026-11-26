import { describe, it, expect } from 'vitest';
import { isSocialLoginEnabled } from '~/utils/socialLogin';

describe('Nuxt public social-login flag', () => {
  it('is deny-by-default', () => {
    expect(isSocialLoginEnabled(undefined)).toBe(false);
    expect(isSocialLoginEnabled(null)).toBe(false);
    expect(isSocialLoginEnabled('')).toBe(false);
    expect(isSocialLoginEnabled('false')).toBe(false);
    expect(isSocialLoginEnabled('yes')).toBe(false);
  });

  it('accepts only an explicit true', () => {
    expect(isSocialLoginEnabled(true)).toBe(true);
    expect(isSocialLoginEnabled('true')).toBe(true);
    expect(isSocialLoginEnabled(' TRUE ')).toBe(true);
  });
});
