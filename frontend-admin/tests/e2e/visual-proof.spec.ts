import { test } from '@playwright/test';

// Cluster A: this spec logs in itself, so it must start from a clean slate —
// an inherited session redirects /login away from the form.
test.use({ storageState: { cookies: [], origins: [] } });

test('Visual Proof of Contact Form & Media', async ({ page }) => {
  // 1. Login
  await page.goto('/login');
  await page.fill('#email', 'admin@forkmyfolio.com');
  await page.fill('#password', process.env.E2E_ADMIN_PASSWORD ?? '');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/manage/dashboard');

  // 2. Snapshot Dashboard
  await page.screenshot({ path: 'tests/e2e/screenshots/evidence-dashboard.png' });

  // 3. Navigate to Contact Page
  await page.goto('/admin-user/contact');
  await page.waitForSelector('.contact-page');

  // 4. Snapshot Contact Page (The Proof)
  await page.screenshot({ path: 'tests/e2e/screenshots/evidence-contact.png' });
});
