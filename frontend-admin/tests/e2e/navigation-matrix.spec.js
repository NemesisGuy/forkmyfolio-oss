import { test, expect } from '@playwright/test';

test.describe('Navigation State Matrix', () => {
  const testUser = {
    email: `test-${Date.now()}@example.com`,
    password: 'Password123!',
    firstName: 'Test',
    lastName: 'User',
    slug: `test-user-${Date.now()}`
  };

  test('Guest View: should not see management links', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('text=Login').first()).toBeVisible();
    await expect(page.locator('text=Dashboard').first()).not.toBeVisible();
    await expect(page.locator('text=Logout').first()).not.toBeVisible();
  });

  test('Owner View & Logout', async ({ page }) => {
    // 1. Register
    await page.goto('/register');
    await page.fill('#firstName', testUser.firstName);
    await page.fill('#lastName', testUser.lastName);
    await page.fill('#email', testUser.email);
    await page.fill('#password', testUser.password);
    await page.fill('#confirmPassword', testUser.password);
    await page.click('button[type="submit"]');

    // Handle T&C Modal
    const termsModal = page.locator('.modal.show').filter({ hasText: 'Terms of Service' });
    await expect(termsModal).toBeVisible();
    await page.locator('#modalTermsCheck').check();
    await page.locator('button', { hasText: 'Confirm & Create Account' }).click();

    // Handle Success Modal
    const successModal = page.locator('.modal.show').filter({ hasText: 'Registration Successful' });
    await expect(successModal).toBeVisible();
    await successModal.locator('button:has-text("Great!")').click();

    // Wait for redirect
    await page.waitForURL(url => url.pathname.includes('/manage/dashboard'));

    // 2. Verify Nav (User Menu)
    const userDropdown = page.locator('#navbarUserDropdown');
    await expect(userDropdown).toBeVisible();
    await userDropdown.click();

    await expect(page.locator('.dropdown-item:has-text("My Dashboard")')).toBeVisible();
    await expect(page.locator('.dropdown-item:has-text("Logout")')).toBeVisible();

    // 3. Logout
    await page.locator('.dropdown-item:has-text("Logout")').click();

    // Handle Logout Confirmation Modal
    await expect(page.locator('.modal.show').filter({ hasText: 'Confirm Logout' })).toBeVisible();
    await page.click('button:has-text("Logout")');
    await page.waitForURL(url => url.pathname === '/login');
    await expect(page.locator('text=Login').first()).toBeVisible();
  });
});
