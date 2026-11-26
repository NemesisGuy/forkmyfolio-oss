import { describe, it, expect } from 'vitest';
import {
  extractOAuth2RedirectParams,
  oauth2ErrorMessage,
} from './oauth2Redirect';

describe('OAuth2 SPA callback query handling', () => {
  it('extracts a single token parameter', () => {
    expect(extractOAuth2RedirectParams({ token: 'app-jwt' })).toEqual({
      token: 'app-jwt',
      error: null,
    });
  });

  it('uses the first value when a parameter is repeated', () => {
    expect(extractOAuth2RedirectParams({ token: ['first', 'second'], error: ['access_denied'] })).toEqual({
      token: 'first',
      error: 'access_denied',
    });
  });

  it('treats blank tokens as missing', () => {
    expect(extractOAuth2RedirectParams({ token: '   ' }).token).toBeNull();
    expect(extractOAuth2RedirectParams({}).token).toBeNull();
  });

  it('keeps an error even when a token is present (error wins)', () => {
    const result = extractOAuth2RedirectParams({ token: 'app-jwt', error: 'server_error' });
    expect(result.error).toBe('server_error');
    expect(result.token).toBe('app-jwt');
  });

  it('renders fixed messages and never reflects raw query text', () => {
    const denied = oauth2ErrorMessage('access_denied');
    expect(denied).toContain('cancelled or denied');

    const generic = oauth2ErrorMessage('<script>alert(1)</script>');
    expect(generic).not.toContain('<script>');
    expect(generic.length).toBeGreaterThan(10);
  });
});
