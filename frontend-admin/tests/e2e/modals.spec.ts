import { test, expect } from '@playwright/test';

// Cluster A: this spec drives its own login, so it must start from a clean
// slate — an inherited session redirects /login away from the form.
test.use({ storageState: { cookies: [], origins: [] } });

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

    await page.screenshot({ path: 'tests/e2e/screenshots/modal-error-light.png' });
    await page.click('.modal.show .btn-danger'); // Close

    // 2. Dark Mode Check
    await setDarkMode(page);
    // setDarkMode() reloads the page, which resets the login form — re-fill it
    // before submitting or the empty form never produces an error modal.
    await page.fill('input[type="email"]', 'wrong@email.com');
    await page.fill('input[type="password"]', 'wrongpass');
    await page.click('button[type="submit"]');
    await expect(errorModal).toBeVisible();

    const darkBgColor = await modalContent.evaluate(el => window.getComputedStyle(el).backgroundColor);
    console.log(`[DEBUG] Dark Mode Modal BG: ${darkBgColor}`);
    // rgba(24, 24, 27, 0.88) is the dark mode --glass-bg-modal (glass.css L40)
    expect(darkBgColor.replace(/\s+/g, '')).toContain('24,24,27');

    await page.screenshot({ path: 'tests/e2e/screenshots/modal-error-dark.png' });
  });

  test('Confirm Modal (Logout) Theme Awareness', async ({ page }) => {
    // Login first to access logout
    await page.fill('input[type="email"]', 'admin@forkmyfolio.com');
    await page.fill('input[type="password"]', process.env.E2E_ADMIN_PASSWORD ?? '');
    await page.click('button[type="submit"]');
    await page.waitForURL(url => url.pathname.includes('/dashboard'));

    // 1. Light Mode Logout Confirm
    // setLightMode() reloads the page, which closes the dropdown — so the
    // trigger has to be (re)opened AFTER the reload, not before it.
    await setLightMode(page);
    await page.click('#navbarUserDropdown');
    await page.click('text=Logout');

    const confirmModal = page.locator('.modal.show');
    await expect(confirmModal).toBeVisible();
    await expect(confirmModal).toContainText('Are you sure you want to logout?');

    const modalContent = confirmModal.locator('.modal-content');
    const bgColor = await modalContent.evaluate(el => window.getComputedStyle(el).backgroundColor);
    expect(bgColor).toContain('255, 255, 255');

    await page.screenshot({ path: 'tests/e2e/screenshots/modal-confirm-light.png' });

    // Wait for animation and click cancel to test dark mode
    await page.click('.modal.show .btn-outline-secondary');
    await expect(confirmModal).toBeHidden();

    // 2. Dark Mode Logout Confirm
    await setDarkMode(page);
    await page.click('#navbarUserDropdown');
    await page.click('text=Logout');
    await expect(confirmModal).toBeVisible();

    const darkBgColor = await modalContent.evaluate(el => window.getComputedStyle(el).backgroundColor);
    // rgba(24, 24, 27, 0.88) — dark --glass-bg-modal (glass.css L40)
    expect(darkBgColor).toContain('24, 24, 27');

    await page.screenshot({ path: 'tests/e2e/screenshots/modal-confirm-dark.png' });
  });
});
