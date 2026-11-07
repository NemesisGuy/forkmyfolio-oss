import { test, expect, type Page } from '@playwright/test';

test.describe('Background Height Verification', () => {

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

  test('Main content should fill viewport height on sparse pages (Profile & Account)', async ({ page }) => {
    // 1. Login
    await login(page, adminEmail, adminPass);
    const slug = page.url().split('/')[3];
    console.log(`Logged in, slug: ${slug}`);

    // 2. Navigate to Profile & Account
    await page.goto(`/${slug}/manage/account`);

    // 3. Wait for content
    const heading = page.locator('h1:has-text("Profile & Account")');
    await expect(heading).toBeVisible({ timeout: 15000 });
    await page.waitForSelector('main.main-content');
    await page.waitForTimeout(1000); // Wait for potential transitions

    // 4. Verify heights
    const metrics = await page.evaluate(() => {
        const viewport = window.innerHeight;
        const main = document.querySelector('main.main-content')?.getBoundingClientRect().height || 0;
        const footer = document.querySelector('footer')?.getBoundingClientRect().height || 0;
        const nav = document.querySelector('nav.navbar')?.getBoundingClientRect().height || 0;
        return { viewportHeight: viewport, mainHeight: main, navbarHeight: nav, footerHeight: footer };
    });

    console.log(`Viewport: ${metrics.viewportHeight}, Main: ${metrics.mainHeight}, Navbar: ${metrics.navbarHeight}, Footer: ${metrics.footerHeight}`);
    console.log(`Total Stack: ${metrics.navbarHeight + metrics.mainHeight + metrics.footerHeight}`);

    const totalHeight = metrics.navbarHeight + metrics.mainHeight + metrics.footerHeight;
    expect(totalHeight).toBeGreaterThanOrEqual(metrics.viewportHeight - 2);

    // Verify main is the one stretching
    expect(metrics.mainHeight).toBeGreaterThan(200);
  });
});
