import { test, expect } from '@playwright/test';

test.describe('Registration Flow', () => {
  test('should allow a user to register, accept terms, and see their new profile', async ({ page }) => {
    // Collect browser logs
    page.on('console', msg => console.log('BROWSER:', msg.text()));

    // 1. Navigate to Registration Page
    await page.goto('/register');
    await expect(page).toHaveTitle(/ForkMyFolio/);
    console.log('Navigated to /register');

    // 2. Fill out the form
    const uniqueId = Date.now();
    const email = `e2e_user_${uniqueId}@example.com`;
    const firstName = 'E2E';
    const lastName = 'Tester';
    await page.fill('#firstName', firstName);
    await page.fill('#lastName', lastName);
    await page.fill('#email', email);
    await page.fill('#password', 'Password123!');
    await page.fill('#confirmPassword', 'Password123!');
    console.log('Form filled.');

    // 3. Submit Form
    const submitBtn = page.locator('button[type="submit"]');
    await expect(submitBtn).toBeEnabled();
    console.log('Clicking Submit...');
    await submitBtn.click();

    // 4. Verify T&C Modal Appears
    console.log('Waiting for Modal...');
    const modal = page.locator('.modal.show').filter({ hasText: 'Terms of Service' });
    await expect(modal).toBeVisible();
    await expect(modal.locator('.modal-title')).toContainText('Terms of Service');
    console.log('Modal visible.');

    await page.waitForTimeout(1000);
    await expect(modal).toBeVisible();

    // 5. Accept Terms
    console.log('Checking checkbox...');
    const checkbox = page.locator('#modalTermsCheck');
    await checkbox.check();

    const confirmBtn = page.locator('button', { hasText: 'Confirm & Create Account' });
    await expect(confirmBtn).toBeEnabled();
    console.log('Clicking Confirm...');
    await confirmBtn.click();

    // 6. Verify Redirection
    console.log('Waiting for success modal...');
    const successModal = page.locator('.modal.show').filter({ hasText: 'Registration Successful' });
    await expect(successModal).toBeVisible();
    console.log('Success modal visible.');

    await successModal.locator('button:has-text("Great!")').click();
    console.log('Clicked Great! on success modal.');

    console.log('Waiting for redirect...');
    await page.waitForURL(/.*\/manage\/dashboard/, { timeout: 20000 });
    const dashboardUrl = page.url();
    console.log('URL transitioned to dashboard:', dashboardUrl);

    // Get Slug from URL
    const slug = dashboardUrl.split('/')[3];
    console.log('Detected Slug:', slug);

    // 7. Navigate to public home (as Owner) to check placeholder content
    await page.goto(`/${slug}`);
    console.log('Navigated to public home (Owner view):', page.url());

    // Verify Placeholder Data
    console.log('Checking for placeholder content...');
    await expect(page.getByText('Welcome to Your New Portfolio!')).toBeVisible();
    await expect(page.getByText('This is your new portfolio summary')).toBeVisible();
    console.log('Content verified for Owner.');

    // 8. Verify Privacy (Logout and view as Guest)
    console.log('Logging out...');
    await page.evaluate(() => localStorage.clear());
    await page.context().clearCookies();

    // RELOAD to clear in-memory store state
    console.log('Reloading page as Guest...');
    await page.reload();

    // Should see Private message
    console.log('Checking for private message...');
    try {
        await expect(page.getByText('This portfolio has been set to private by the owner')).toBeVisible({ timeout: 15000 });
        console.log('Privacy verified for Guest.');
    } catch (e) {
        console.log('FAILED to find Private message. Page content was:');
        const text = await page.innerText('body');
        console.log(text);
        throw e;
    }
  });
});
