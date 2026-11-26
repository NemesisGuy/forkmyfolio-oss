import { test, expect } from '@playwright/test';

test.describe('Contact Form Visibility Audit', () => {

  test('Contact Form should be fully visible and interactive', async ({ page }) => {
    // 1. Navigate to a user's contact page (public view)
    // Assuming 'admin-user' has a public profile enabled
    await page.goto('/admin-user/contact');

    // 2. Check for the main container
    // 15s, not the 5s default: under a fullyParallel run the SPA mounts well
    // after the default window on a loaded machine (run D failed at 5000ms
    // with the full contact page rendering immediately after — see
    // TESTING_POLICY "load-sensitive specs").
    const container = page.locator('.contact-page');
    await expect(container).toBeVisible({ timeout: 15000 });

    // 3. Verify the form exists and is visible
    const form = page.locator('form');
    await expect(form).toBeVisible();
    // Pass generic selector fallback if specific class unknown, checking for "Contact" header
    await expect(page.locator('h1, h2').filter({ hasText: /Contact|Get in touch/i }).first()).toBeVisible();

    // 3. Verify Inputs
    // Name
    const nameInput = page.locator('input#name, input[name="name"]');
    await expect(nameInput).toBeVisible();
    await expect(nameInput).toBeEditable();

    // Email
    const emailInput = page.locator('input#email, input[name="email"]');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeEditable();

    // Message
    const messageInput = page.locator('textarea#message, textarea[name="message"]');
    await expect(messageInput).toBeVisible();
    await expect(messageInput).toBeEditable();

    // 4. Verify Submit Button
    const submitBtn = page.locator('button[type="submit"]');
    await expect(submitBtn).toBeVisible();
    await expect(submitBtn).toBeEnabled();
    await expect(submitBtn).toHaveText(/Send|Submit/i);

    // 5. Verify no blocking overlays (like an infinite spinner)
    // Ensure the form is not covered by a .loading-overlay
    await expect(page.locator('.loading-overlay')).not.toBeVisible();
  });

});
