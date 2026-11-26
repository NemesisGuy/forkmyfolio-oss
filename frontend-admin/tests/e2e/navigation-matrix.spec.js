import { test, expect } from '@playwright/test';

// Cluster A: this spec registers its own user, so it must start logged out —
// an inherited session redirects /register away from the registration form.
test.use({ storageState: { cookies: [], origins: [] } });

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

    // Await the register RESPONSE before asserting the success modal: under a
    // fullyParallel run the POST can take longer than the default 5s expect
    // window (run F: registration succeeded — slug test-user-18, dialog on
    // screen in the failure snapshot — but the modal rendered just after the
    // window closed). Logging the status/body diagnoses future failures.
    const registerResponse = page.waitForResponse(
      resp => /\/auth\/register/.test(resp.url()) && resp.request().method() === 'POST'
    );
    await page.locator('button', { hasText: 'Confirm & Create Account' }).click();
    const registered = await registerResponse;
    console.log(`Register response: ${registered.status()} ${registered.url()}`);
    if (!registered.ok()) {
      console.log(`Register error body: ${await registered.text()}`);
    }
    expect(registered.ok()).toBeTruthy();

    // Handle Success Modal — 10s, not the 5s default: the modal renders after
    // the 201 + auto-login round-trip, which is load-sensitive (see
    // TESTING_POLICY §4.1 "load-sensitive specs").
    const successModal = page.locator('.modal.show').filter({ hasText: 'Registration Successful' });
    await expect(successModal).toBeVisible({ timeout: 10000 });
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
