import { test, expect } from '@playwright/test';

test.describe('Slug Guard & 404 Redirection', () => {
  test.beforeEach(async ({ page }) => {
    page.on('console', msg => console.log(`BROWSER [${msg.type()}]: ${msg.text()}`));
  });

  test('should redirect non-route reserved slugs to 404', async ({ page }) => {
    // These are reserved keywords that are NOT registered as static routes in the router,
    // so they would normally match /:slug if not guarded.
    const nonRouteReserved = ['null', 'undefined'];

    for (const slug of nonRouteReserved) {
      await page.goto(`/${slug}`);
      await expect(page).toHaveURL(/.*\/not-found/);
      await expect(page.locator('h1')).toContainText('404');
    }
  });

  test('should redirect non-existent nested paths to 404', async ({ page }) => {
    await page.goto('/some-random-slug/this/does/not/exist');
    await expect(page).toHaveURL(/.*\/not-found/);
    await expect(page.locator('h1')).toContainText('404');
  });

  test('should redirect valid-looking but non-existent slugs to 404', async ({ page }) => {
     // If the slug is NOT reserved but doesn't exist, we now redirect to /not-found in the router.
     await page.goto('/valid-non-existent-user');
     await expect(page).toHaveURL(/.*\/not-found/);
     await expect(page.locator('h1')).toContainText('404');
  });
});
