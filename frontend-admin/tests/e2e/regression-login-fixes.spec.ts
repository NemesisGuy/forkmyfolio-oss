import { test, expect, Page } from '@playwright/test';

/**
 * @name Regression Login Fixes
 * @description Verifies fixes for the 500 portfolio error, announcement crashes, and guest pass modal hang.
 */
test.describe('Regression: Post-Login Stability', () => {

  const login = async (page: Page, email: string, password: string) => {
    await page.goto('/login');
    await page.fill('#email', email);
    await page.fill('#password', password);
    await page.click('button[type="submit"]');
    await page.waitForURL(url => url.pathname.includes('/manage/dashboard'), { timeout: 15000 });
  };

  test('Portfolio, Announcements, and Guest Pass Modal should be stable after login', async ({ page }) => {
    console.log('Step 1: Logging in...');
    await login(page, 'admin@forkmyfolio.com', process.env.E2E_ADMIN_PASSWORD ?? '');
    console.log('Login logic finished.');

    console.log('Step 2: Checking Announcements...');
    const announcementTrigger = page.locator('#announcementDropdown');
    await expect(announcementTrigger).toBeVisible({ timeout: 15000 });

    await announcementTrigger.click();
    console.log('Announcement dropdown clicked.');
    const dropdownMenu = page.locator('.dropdown-menu.show');
    await expect(dropdownMenu).toBeVisible({ timeout: 10000 });

    await expect(page.locator('.announcement-list')).toBeVisible();
    console.log('Announcements list is visible.');

    console.log('Step 3: Checking Portfolio Fetch...');
    const userSlug = 'admin-user';
    await page.goto(`/${userSlug}`, { waitUntil: 'networkidle' });
    console.log(`Navigated to /${userSlug}`);

    // Verify the name appears (Wait for the fetch to complete)
    const fullName = page.locator('.text-gradient');
    await expect(fullName).toBeVisible({ timeout: 20000 });
    const textValue = await fullName.innerText();
    console.log(`Full Name found: "${textValue}"`);
    expect(textValue.trim().length).toBeGreaterThan(0);

    console.log('Step 4: Checking Guest Pass Modal...');
    await page.goto(`/${userSlug}/manage/guest-passes`, { waitUntil: 'networkidle' });
    console.log('Navigated to Guest Passes page.');

    const createButton = page.locator('button:has-text("Create New Pass")');
    await expect(createButton).toBeVisible({ timeout: 15000 });

    await createButton.click();
    console.log('Create Pass button clicked.');

    const modal = page.locator('#createPassModal.show');
    await expect(modal).toBeVisible({ timeout: 10000 });
    console.log('Guest Pass modal is visible.');

    await page.fill('input[placeholder*="Recruiter"]', 'Test Regression Pass');
    const cancelButton = modal.locator('button:has-text("Cancel")');
    await cancelButton.click();
    console.log('Cancel button clicked.');

    await expect(modal).not.toBeVisible({ timeout: 10000 });
    await expect(page.locator('.modal-backdrop')).not.toBeVisible({ timeout: 10000 });

    await expect(page.locator('body')).not.toHaveClass(/modal-open/);
    console.log('Regression test: ALL PASSED');
  });
});
