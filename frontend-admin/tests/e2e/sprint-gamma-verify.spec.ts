import { test, expect, type Page } from '@playwright/test';

test.describe('Sprint Gamma Verification', () => {

  const adminEmail = 'admin@forkmyfolio.com';
  const adminPass = process.env.E2E_ADMIN_PASSWORD ?? '';

  // Helper login
  async function login(page: Page, email: string, pass: string) {
    await page.goto('/login');
    await page.fill('#email', email);
    await page.fill('#password', pass);
    await page.click('button[type="submit"]');
    await page.waitForURL(url => url.pathname.includes('/manage/dashboard'), { timeout: 15000 });
  }

  test('Privacy Lock Screen should appear for private portfolio', async ({ page }) => {
    // 1. Login
    await login(page, adminEmail, adminPass);
    const slug = page.url().split('/')[3];

    // 2. Set Portfolio to Private
    await page.goto(`/${slug}/manage/settings/profile`);
    const visibilityToggle = page.locator('#publiclyVisible');
    await expect(visibilityToggle).toBeVisible({ timeout: 10000 });

    if (await visibilityToggle.isChecked()) {
        await visibilityToggle.uncheck();
        await page.click('button:has-text("Save Changes")');
        await page.waitForTimeout(2000);
    }

    // 3. Logout
    await page.locator('#navbarUserDropdown').click();
    await page.locator('.dropdown-item:has-text("Logout")').click();
    await page.locator('.modal.show button:has-text("Logout")').click();
    await page.waitForURL('/login');

    // 4. Visit public portfolio as guest
    await page.goto(`/${slug}`);

    // 5. Expect PrivateLockScreen
    const lockScreen = page.locator('.private-lock-screen');
    await expect(lockScreen).toBeVisible({ timeout: 10000 });

    // 6. Reset to Public
    await login(page, adminEmail, adminPass);
    await page.goto(`/${slug}/manage/settings/profile`);
    await page.locator('#publiclyVisible').check();
    await page.click('button:has-text("Save Changes")');
    await page.waitForTimeout(2000);
    console.log('Privacy Lock Screen verified and reset.');
  });

  test('Dynamic SEO Meta Tags should be updated', async ({ page }) => {
    const slug = 'admin-user';

    // 1. Visit Public Home Page
    await page.goto(`/${slug}`);

    // 2. Wait for profile data to load
    await expect(page.locator('.hero-section')).toBeVisible({ timeout: 15000 });

    // 3. Check Title
    const title = await page.title();
    console.log(`Page Title: ${title}`);
    expect(title).not.toBe('ForkMyFolio');

    // 4. Check Meta Description
    const metaDescription = page.locator('meta[name="description"]');
    const content = await metaDescription.getAttribute('content');
    console.log(`Meta Description: ${content}`);
    expect(content).not.toBe('ForkMyFolio - The ultimate developer portfolio builder.');
  });

});
