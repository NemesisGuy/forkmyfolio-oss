import { test, expect } from '@playwright/test';

test.describe('Testimonials Page', () => {
  const timestamp = Date.now();
  const user = {
    email: `testimonials-test-${timestamp}@example.com`,
    password: 'Password123!',
    firstName: 'Test',
    lastName: 'User'
  };

  test('should load testimonials page and handle invalid UUID gracefully', async ({ page }) => {
    test.slow();

    // 1. Register a user
    await page.goto('/register');
    await page.fill('#firstName', user.firstName);
    await page.fill('#lastName', user.lastName);
    await page.fill('#email', user.email);
    await page.fill('#password', user.password);
    await page.fill('#confirmPassword', user.password);
    await page.click('button[type="submit"]');

    // Handle T&C Modal
    const termsModal = page.locator('.modal.show').filter({ hasText: 'Terms of Service' });
    await expect(termsModal).toBeVisible();
    await page.locator('#modalTermsCheck').check();
    await page.locator('button', { hasText: 'Confirm & Create Account' }).click();

    // Handle Success Modal
    const successModal = page.locator('.modal.show').filter({ hasText: 'Registration Successful' });
    await expect(successModal).toBeVisible();
    await successModal.locator('button:has-text("Great!")').click();

    // Wait for validation that we are on the dashboard
    // The URL structure is /:slug/manage/dashboard
    await expect(page).toHaveURL(/.*\/manage\/dashboard/);
    const dashboardUrl = new URL(page.url()).pathname;
    console.log(`Debug: Dashboard path is ${dashboardUrl}`);

    // Split the path /:slug/manage/dashboard -> ["", "slug", "manage", "dashboard"]
    const pathParts = dashboardUrl.split('/');
    const realSlug = pathParts[1];

    if (!realSlug || realSlug === 'null' || realSlug === 'undefined') {
      throw new Error(`Failed to extract valid real slug from path: ${dashboardUrl}`);
    }
    console.log(`Extracted real slug: ${realSlug}`);

    // 2. Navigate to public testimonials page
    await page.goto(`/${realSlug}/testimonials`);

    // 3. Verify it loaded (no ReferenceError crash)
    await expect(page.locator('h1')).toContainText('Testimonials');
    await expect(page.locator('text=No Testimonials Yet')).toBeVisible();

    // 4. Handle invalid testimonial UUID gracefully (from query param)
    await page.goto(`/${realSlug}/testimonials?testimonial=non-existent-uuid`);
    await expect(page.locator('h1')).toContainText('Testimonials');

    // Ensure we are STILL on the testimonials page (not 404)
    const currentUrl = page.url();
    expect(currentUrl).toContain(`/${realSlug}/testimonials`);

    // The modal backdrop or open modal should not be present
    const modal = page.locator('.modal.show');
    await expect(modal).not.toBeVisible();
  });
});
