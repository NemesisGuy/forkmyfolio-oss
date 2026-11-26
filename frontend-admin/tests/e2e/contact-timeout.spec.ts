import { test, expect } from '@playwright/test';

test.describe('Contact Form Integrity', () => {

  test('should handle network timeouts gracefully (15s limit)', async ({ page }) => {
    // 1. Visit a public contact page
    // Using 'admin-user' slug as it likely exists from seed data
    await page.goto('/admin-user/contact');

    // 2. Setup Route Interception to simulate a hang.
    // The backend endpoint is /api/v1/portfolios/{slug}/contact-messages.
    // IMPORTANT: the route must NEVER settle. Fulfilling it late (e.g. after
    // 16s) races the apiClient's own 15s abort — if the late 200 wins, the
    // client reports success and no error toast is ever shown, which is what
    // made this test flaky.
    await page.route('**/contact-messages', () => new Promise(() => {}));

    // 3. Fill the form
    await page.fill('input#name', 'Test User');
    await page.fill('input#email', 'test@example.com');
    await page.fill('textarea#message', 'This message should time out.');

    // 4. Submit
    /*
       Note: We expect the frontend to abort after 15s.
       Playwright default timeout is 30s, so this is safe.
    */
    const startTime = Date.now();
    await page.click('button[type="submit"]');

    // 5. Verify Error State
    // The frontend should show a notification or error modal about the timeout.
    // Based on apiClient.js, it throws status 408 "Request timed out..."
    // ContactPage.vue uses notificationService to show error.

    // Check for the error toast/notification
    const errorToast = page.locator('.toast-body, .alert-danger, .notification-error');
    await expect(errorToast).toBeVisible({ timeout: 20000 });
    await expect(errorToast).toContainText(/Request timed out/i);

    // Ensure loading state is gone
    const sendButton = page.locator('button[type="submit"]');
    await expect(sendButton).not.toBeDisabled();
    await expect(sendButton).toContainText('Send Message');

    const duration = Date.now() - startTime;
    console.log(`Timeout test took ${duration}ms`);
    expect(duration).toBeGreaterThan(14000); // Should be at least ~15s
  });

});
