import { test, expect } from '@playwright/test';

/**
 * Phase 7.3 — Playwright revival smoke suite.
 *
 * Read-only: only performs GET requests / page views against the default
 * deployment (local staging at localhost:8189), creates no data, and serves
 * as the executable sanity check that the Playwright stack + staging
 * deployment are alive end-to-end. Heavier, data-creating specs
 * (e.g. critical-path.spec.ts) remain in tests/e2e but are NOT part of the
 * smoke gate.
 */
test.describe('Smoke: staging liveness', () => {
  test('login page renders the authentication form', async ({ page }) => {
    await page.goto('/login');

    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('public SPA shell loads at the root without a server error', async ({ page }) => {
    const response = await page.goto('/');

    expect(response?.status()).toBeLessThan(500);
    // The SPA actually mounted: the landing hero renders
    await expect(page.getByRole('heading', { name: 'Welcome to ForkMyFolio' })).toBeVisible();
    await expect(page.locator('nav')).toBeVisible();
  });
});
