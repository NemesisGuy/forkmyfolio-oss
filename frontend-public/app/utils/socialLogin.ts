/**
 * @file app/utils/socialLogin.ts
 * @description Deny-by-default parser for the non-secret public runtime flag
 * that optionally shows a sign-in link on the Nuxt public site.
 * No issuer URL, client id or secret ever reaches the Nuxt bundle.
 */
export function isSocialLoginEnabled(value: unknown): boolean {
  if (typeof value === 'boolean') return value;
  if (typeof value !== 'string') return false;
  return value.trim().toLowerCase() === 'true';
}
