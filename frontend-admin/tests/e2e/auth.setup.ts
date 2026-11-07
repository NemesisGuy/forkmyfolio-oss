import { test as setup, expect } from '@playwright/test';

const authFile = 'playwright/.auth/user.json';

setup('authenticate', async ({ page }) => {
  console.log('[Auth Setup] Starting robust session initialization for Clark Kent...');

  // 1. Navigate to login
  await page.goto('/login');

  // 2. Fill credentials (supplied via environment; no credentials in source)
  await page.fill('#email', process.env.E2E_ADMIN_EMAIL ?? 'admin@forkmyfolio.com');
  await page.fill('#password', process.env.E2E_ADMIN_PASSWORD ?? '');
  await page.click('button[type="submit"]');

  // 3. Wait for redirect
  console.log('[Auth Setup] Waiting for redirect...');
  await expect(page).toHaveURL(/\/(dashboard|admin)/, { timeout: 15000 });
  console.log('[Auth Setup] Landed on dashboard.');

  // 4. Debug: Check cookies
  const cookies = await page.context().cookies();
  console.log('[Auth Setup] Captured Cookies:', JSON.stringify(cookies, null, 2));

  if (cookies.length === 0) {
    console.warn('[Auth Setup] WARNING: No cookies captured! Testing if we have localStorage...');
  }

  // 5. Save storage state
  await page.context().storageState({ path: authFile });
  console.log(`[Auth Setup] Auth state saved to ${authFile}`);
});
