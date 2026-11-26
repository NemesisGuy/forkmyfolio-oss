/**
 * @file src/services/oauth2Redirect.ts
 * @description Pure helpers for the SPA's `/oauth2/redirect?token=...` callback.
 * Kept separate from the view so the query handling is unit-testable without
 * mounting a router.
 */
import type { LocationQuery, LocationQueryValue } from 'vue-router';

export interface OAuth2RedirectParams {
  token: string | null;
  error: string | null;
}

const firstValue = (value: LocationQueryValue | LocationQueryValue[] | undefined): string | null => {
  const raw = Array.isArray(value) ? value[0] : value;
  if (typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  return trimmed || null;
};

/**
 * Reads the callback parameters. The token is passed straight to authService
 * (never logged or persisted separately); an `error` parameter takes priority.
 */
export function extractOAuth2RedirectParams(query: LocationQuery): OAuth2RedirectParams {
  return {
    token: firstValue(query.token),
    error: firstValue(query.error),
  };
}

/**
 * Fixed, non-reflected failure messages. Raw query text must never be rendered
 * as an error message from a URL controlled by a third party.
 */
export function oauth2ErrorMessage(error: string | null): string {
  switch (error) {
    case 'access_denied':
      return 'Sign-in was cancelled or denied. Please try again or sign in with your password.';
    case 'invalid_token':
    case 'invalid_request':
    case 'expired_token':
      return 'The sign-in link is invalid or has expired. Please try again.';
    default:
      return 'Social sign-in failed. Please try again or sign in with your password.';
  }
}
