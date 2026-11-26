import { test, expect } from '@playwright/test';

test.describe('Slug Guard & 404 Redirection', () => {
  test.beforeEach(async ({ page }) => {
    page.on('console', msg => console.log(`BROWSER [${msg.type()}]: ${msg.text()}`));
  });

  // NOTE (Phase 0.33): Nuxt serves these URLs itself (they never reach the Vue
  // SPA's /not-found route), so the URL is intentionally NOT rewritten. The
  // observable contract is a real 404 status plus the SEO-facing title, both of
  // which are asserted below.

  test('should redirect non-route reserved slugs to 404', async ({ page }) => {
    // These are reserved keywords that are NOT registered as static routes in the router,
    // so they would normally match /:slug if not guarded.
    const nonRouteReserved = ['null', 'undefined'];

    for (const slug of nonRouteReserved) {
      const response = await page.goto(`/${slug}`);
      expect(response?.status()).toBe(404);
      await expect(page).toHaveTitle(/Portfolio not found/);
    }
  });

  test('should redirect non-existent nested paths to 404', async ({ page }) => {
    // No Nuxt page matches a multi-segment unknown path, so the branded
    // error page renders (still at the requested URL) with a 404 status.
    const response = await page.goto('/some-random-slug/this/does/not/exist');
    expect(response?.status()).toBe(404);
    await expect(page).toHaveTitle(/Page not found/);
    await expect(page.locator('h1')).toContainText('404');
  });

  test('should redirect valid-looking but non-existent slugs to 404', async ({ page }) => {
    // A slug-shaped path that does not exist: Nuxt keeps the URL and serves
    // the portfolio "not found" state with a correct 404 status.
    const response = await page.goto('/valid-non-existent-user');
    expect(response?.status()).toBe(404);
    await expect(page).toHaveTitle(/Portfolio not found/);
  });
});
