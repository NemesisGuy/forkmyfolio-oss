/**
 * @file src/services/runtimeConfig.ts
 * @description Non-secret runtime configuration injected into public/config.js
 * by entrypoint.sh at container start (the same mechanism as VITE_API_BASE_URL).
 *
 * ONLY booleans and provider ids belong here. Never put an issuer URL, client
 * id, client secret or any other credential in these values — the config file
 * is publicly served and is included in built image assets.
 */

export interface RuntimeConfigSnapshot {
  API_BASE_URL?: string | null;
  SOCIAL_LOGIN_ENABLED?: string | boolean | null;
  SOCIAL_PROVIDERS?: string | null;
}

declare global {
  interface Window {
    runtimeConfig?: RuntimeConfigSnapshot;
  }
}

/** Provider ids the SPA knows how to render a button for. */
export const SOCIAL_PROVIDER_IDS = ['keycloak'] as const;
export type SocialProviderId = (typeof SOCIAL_PROVIDER_IDS)[number];

export interface SocialLoginConfig {
  enabled: boolean;
  providers: SocialProviderId[];
}

const isPlaceholder = (value: unknown): boolean =>
  typeof value === 'string' && /^##.*##$/.test(value.trim());

/**
 * Deny-by-default boolean parsing. Only an explicit `true` (or boolean true)
 * enables a feature; unset values, placeholders and arbitrary strings do not.
 */
export function parseBooleanFlag(value: unknown): boolean {
  if (typeof value === 'boolean') return value;
  if (typeof value !== 'string' || isPlaceholder(value)) return false;
  return value.trim().toLowerCase() === 'true';
}

/**
 * Parses a comma-separated provider-id list. Unknown ids are ignored so an
 * operator typo cannot render a broken authorize URL; ids are normalised to
 * lower case and de-duplicated.
 */
export function parseProviderIds(value: unknown): SocialProviderId[] {
  if (typeof value !== 'string' || isPlaceholder(value)) return [];

  const allowed = new Set<string>(SOCIAL_PROVIDER_IDS);
  const seen = new Set<string>();
  const result: SocialProviderId[] = [];
  for (const entry of value.split(',')) {
    const id = entry.trim().toLowerCase();
    if (allowed.has(id) && !seen.has(id)) {
      seen.add(id);
      result.push(id as SocialProviderId);
    }
  }
  return result;
}

/** Reads the current runtime config. Values are not cached (config.js is loaded once per page). */
export function getSocialLoginConfig(): SocialLoginConfig {
  const raw = typeof window === 'undefined' ? undefined : window.runtimeConfig;
  return {
    enabled: parseBooleanFlag(raw?.SOCIAL_LOGIN_ENABLED),
    providers: parseProviderIds(raw?.SOCIAL_PROVIDERS),
  };
}

/** Vendor-neutral label — never disclose the IdP brand in the UI. */
export function getSocialProviderLabel(_id: SocialProviderId): string {
  void _id; // signature kept for callers; label is intentionally vendor-neutral
  return 'Sign in with SSO';
}
