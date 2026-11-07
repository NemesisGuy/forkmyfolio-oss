import { test, expect } from '@playwright/test';

test.describe('Fast-Fail Validation', () => {

  test('Contact Form should block submission with invalid email', async ({ page }) => {
    await page.goto('/public/admin-user/contact'); // Adjust path as needed

    // Fill with invalid email
    await page.fill('#name', ' Tester');
    await page.fill('#email', 'invalid-email-format');
    await page.fill('#message', 'Hello world');

    // Attempt submit
    await page.click('button[type="submit"]');

    // Verify frontend validation error appears
    // The exact selector depends on implementation, but searching for "valid email" text or .is-invalid class
    const errorVisible = await page.isVisible('.invalid-feedback:has-text("valid email")');
    expect(errorVisible).toBeTruthy();

    // Verify API was NOT called (harder in E2E, but we expect no success message)
    const successVisible = await page.isVisible('.text-bg-success');
    expect(successVisible).toBeFalsy();
  });

  test('Contact Form should block empty fields', async ({ page }) => {
      await page.goto('/public/admin-user/contact');
      // Click submit empty
      await page.click('button[type="submit"]');

      // Check for required errors
      await expect(page.locator('.invalid-feedback')).toHaveCount(3); // Name, Email, Message
  });

});
