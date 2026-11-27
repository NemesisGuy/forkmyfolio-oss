import { test, expect } from '@playwright/test';

/**
 * v1.3.0 Keycloak/OIDC provisions — end-to-end smoke on the DEV stack.
 * No real IdP is configured in DEV: the backend registers a harmless local
 * placeholder issuer, so we pin the PROXY ROUTE and the SPA CALLBACK surface
 * only. The real provider flow is UAT'd by the owner against staging/prod.
 */
test.describe('OAuth2 / OIDC provisions', () => {
  test('authorize request reaches the backend (not Nuxt) and redirects to the configured issuer', async ({ request }) => {
    const response = await request.get('/oauth2/authorize/keycloak?redirect_uri=%2Foauth2%2Fredirect', {
      maxRedirects: 0,
    });
    // Backend Spring Security returns a 302 to the IdP authorize endpoint;
    // a Nuxt catch-all would instead return 200 HTML or a 404.
    expect(response.status()).toBe(302);
    const location = response.headers()['location'] ?? '';
    expect(location).toContain('/protocol/openid-connect/auth');
    expect(location).toContain('keycloak');
  });

  test('login page offers a "Use a different account" link that forces prompt=login', async ({ page }) => {
    test.skip(test.info().project.name !== 'chromium', 'requires an unauthenticated session — logged-in users are redirected away from /login');
    await page.goto('/login');
    const link = page.getByRole('link', { name: 'Use a different account' });
    await expect(link).toBeVisible();
    expect(decodeURIComponent(await link.getAttribute('href') || '')).toContain('prompt=login');
  });

  test('SPA callback page renders a fixed error for ?error= (no reflected query text)', async ({ page }) => {
    const reflected = '<img src=x onerror=alert(1)>';
    await page.goto(`/oauth2/redirect?error=${encodeURIComponent(reflected)}`);
    await expect(page.locator('h1', { hasText: 'Social sign-in' })).toBeVisible();
    const alert = page.locator('[role="alert"]');
    await expect(alert).toContainText('Social sign-in failed');
    await expect(alert).not.toContainText(reflected);
  });
});
