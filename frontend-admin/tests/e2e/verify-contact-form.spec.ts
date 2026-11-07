import { test, expect } from '@playwright/test';

test.describe('Contact Page Verification', () => {

  test('Contact Form should be visible and interactive', async ({ page }) => {
    // Navigate to a valid public profile's contact page
    // Using 'admin-user' as confirmed active user in Dev DB
    await page.goto('/admin-user/contact');

    // Verify Page Title matches "Get In Touch" as per user description
    await expect(page.locator('h1, h2, h3').filter({ hasText: 'Get In Touch' })).toBeVisible();

    // Verify the Form itself is visible
    // Looking for widely used inputs like email, subject, or message
    const emailInput = page.locator('input[type="email"]');
    const messageInput = page.locator('textarea');
    const submitBtn = page.locator('button[type="submit"]');

    console.log('Checking for Email Input visibility...');
    await expect(emailInput).toBeVisible({ timeout: 5000 });

    console.log('Checking for Message Input visibility...');
    await expect(messageInput).toBeVisible();

    console.log('Checking for Submit Button visibility...');
    await expect(submitBtn).toBeVisible();

    // Verify it's not disabled (unless logic requires it to be disabled initially,
    // but usually contact forms are enabled)
    await expect(submitBtn).toBeEnabled();
  });

});
