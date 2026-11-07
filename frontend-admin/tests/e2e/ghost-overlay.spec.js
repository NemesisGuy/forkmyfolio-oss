import { test, expect } from '@playwright/test';

test.describe('Ghost Overlay Detection', () => {
  test('should clear modal backdrop when navigating back from login error', async ({ page }) => {
    // 0. Visit home first so we have a place to go back to
    await page.goto('/');

    // 1. Go to Login page
    await page.goto('/login');

    // 2. Trigger an Error Modal by submitting wrong credentials
    await page.fill('#email', 'wrong@example.com');
    await page.fill('#password', 'WrongPass123!');
    await page.click('button[type="submit"]');

    // 3. Verify Error Modal is visible
    const modal = page.locator('.modal.show');
    await expect(modal).toBeVisible();

    // 4. Navigate back (to Home page)
    await page.goBack();

    // 5. Assertions: Modal and backdrop should be GONE
    await expect(modal).not.toBeVisible();
    await page.waitForTimeout(500);
    const bodyClass = await page.evaluate(() => document.body.className);
    expect(bodyClass).not.toContain('modal-open');
    const backdropCount = await page.locator('.modal-backdrop').count();
    expect(backdropCount).toBe(0);
  });
});
