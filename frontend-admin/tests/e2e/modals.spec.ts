import { test, expect } from '@playwright/test';

test.describe('Modal Theme and Visibility Regression', () => {

  test.beforeEach(async ({ page }) => {
    // We test on the login page as it uses both Success and Error modals
    await page.goto('/login');
  });

  async function setDarkMode(page) {
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-bs-theme', 'dark');
      localStorage.setItem('theme', 'dark');
    });
    await page.reload();
  }

  async function setLightMode(page) {
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-bs-theme', 'light');
      localStorage.setItem('theme', 'light');
    });
    await page.reload();
  }

  test('Error Modal Theme Awareness (Light vs Dark)', async ({ page }) => {
    // 1. Light Mode Check
    await setLightMode(page);
    await page.fill('input[type="email"]', 'wrong@email.com');
    await page.fill('input[type="password"]', 'wrongpass');
    await page.click('button[type="submit"]');

    const errorModal = page.locator('.modal.show');
    const htmlTheme = await page.evaluate(() => document.documentElement.getAttribute('data-bs-theme'));
    console.log(`[DEBUG] HTML data-bs-theme: ${htmlTheme}`);

    // Check for light-mode readability (should not have hardcoded dark background anymore)
    const modalContent = errorModal.locator('.modal-content');

    // Wait for any transitions to finish
    await page.waitForTimeout(300);

    const bgColor = await modalContent.evaluate(el => window.getComputedStyle(el).backgroundColor);
    console.log(`[DEBUG] Light Mode Modal BG: ${bgColor}`);
    // Some browsers might return 'rgb' instead of 'rgba' if opacity is 1, or different spacing
    expect(bgColor.replace(/\s+/g, '')).toContain('255,255,255');

    await page.screenshot({ path: 'modal-error-light.png' });
    await page.click('.modal.show .btn-danger'); // Close

    // 2. Dark Mode Check
    await setDarkMode(page);
    await page.click('button[type="submit"]');
    await expect(errorModal).toBeVisible();

    const darkBgColor = await modalContent.evaluate(el => window.getComputedStyle(el).backgroundColor);
    console.log(`[DEBUG] Dark Mode Modal BG: ${darkBgColor}`);
    // rgba(16, 16, 16, 0.55) is the dark mode --glass-bg-modal
    expect(darkBgColor.replace(/\s+/g, '')).toContain('16,16,16');

    await page.screenshot({ path: 'modal-error-dark.png' });
  });

  test('Confirm Modal (Logout) Theme Awareness', async ({ page }) => {
    // Login first to access logout
    await page.fill('input[type="email"]', 'admin@forkmyfolio.com');
    await page.fill('input[type="password"]', process.env.E2E_ADMIN_PASSWORD ?? '');
    await page.click('button[type="submit"]');
    await page.waitForURL(url => url.pathname.includes('/dashboard'));

    // Open User Dropdown
    await page.click('#navbarUserDropdown');

    // 1. Light Mode Logout Confirm
    await setLightMode(page);
    await page.click('text=Logout');

    const confirmModal = page.locator('.modal.show');
    await expect(confirmModal).toBeVisible();
    await expect(confirmModal).toContainText('Are you sure you want to logout?');

    const modalContent = confirmModal.locator('.modal-content');
    const bgColor = await modalContent.evaluate(el => window.getComputedStyle(el).backgroundColor);
    expect(bgColor).toContain('255, 255, 255');

    await page.screenshot({ path: 'modal-confirm-light.png' });

    // Wait for animation and click cancel to test dark mode
    await page.click('.modal.show .btn-outline-secondary');
    await expect(confirmModal).toBeHidden();

    // 2. Dark Mode Logout Confirm
    await setDarkMode(page);
    await page.click('#navbarUserDropdown');
    await page.click('text=Logout');
    await expect(confirmModal).toBeVisible();

    const darkBgColor = await modalContent.evaluate(el => window.getComputedStyle(el).backgroundColor);
    expect(darkBgColor).toContain('16, 16, 16');

    await page.screenshot({ path: 'modal-confirm-dark.png' });
  });
});
