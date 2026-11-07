import { test } from '@playwright/test';

test('Visual Proof of Contact Form & Media', async ({ page }) => {
  // 1. Login
  await page.goto('/login');
  await page.fill('#username', 'admin@forkmyfolio.com');
  await page.fill('#password', process.env.E2E_ADMIN_PASSWORD ?? '');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin-user');

  // 2. Snapshot Dashboard
  await page.screenshot({ path: 'evidence-dashboard.png' });

  // 3. Navigate to Contact Page
  await page.goto('/admin-user/contact');
  await page.waitForSelector('.contact-page');

  // 4. Snapshot Contact Page (The Proof)
  await page.screenshot({ path: 'evidence-contact.png' });
});
