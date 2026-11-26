import { test, expect } from '@playwright/test';

// Cluster A: this spec registers its own user, so it must start logged out —
// an inherited session redirects /register away from the registration form.
test.use({ storageState: { cookies: [], origins: [] } });

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

    // 6b. New portfolios are PRIVATE by default, and the Nuxt owner view never
    //     carries credentials (useApi.fetchJson attaches no Authorization
    //     header), so a private portfolio is concealed from everyone — owner
    //     included. Publish it through the API to exercise the placeholder
    //     content, then restore private before the guest check in step 8.
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    const setVisibility = (isPublic: boolean) =>
      page.evaluate(
        async ({ isPublic, accessToken }) => {
          const res = await fetch('/api/v1/me/profile/visibility', {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${accessToken}`,
            },
            body: JSON.stringify({ isPublic }),
          });
          if (!res.ok) {
            throw new Error(`visibility update failed: ${res.status} ${await res.text()}`);
          }
        },
        { isPublic, accessToken }
      );

    await setVisibility(true);

    // 7. Navigate to public home (as Owner) to check placeholder content
    await page.goto(`/${slug}`);
    console.log('Navigated to public home (Owner view):', page.url());

    // Verify Placeholder Data
    // exact:true — Nuxt's route announcer echoes the document title ("E2E Tester
    // — Welcome to Your New Portfolio! | …") into an aria-live span, which a
    // substring match would also resolve to (strict-mode violation).
    console.log('Checking for placeholder content...');
    await expect(page.getByText('Welcome to Your New Portfolio!', { exact: true })).toBeVisible();
    await expect(page.getByText('This is your new portfolio summary')).toBeVisible();
    console.log('Content verified for Owner.');

    // 8. Verify Privacy (Logout and view as Guest)
    console.log('Restoring private visibility...');
    await setVisibility(false);

    console.log('Logging out...');
    await page.evaluate(() => localStorage.clear());
    await page.context().clearCookies();

    // RELOAD to clear in-memory store state
    console.log('Reloading page as Guest...');
    await page.reload();

    // Phase 7.4: a private portfolio is concealed behind a real 404 (there is
    // no bespoke "set to private" screen), so assert the 404 title and that
    // the (now-private) headline is no longer exposed.
    console.log('Checking for concealed portfolio as Guest...');
    try {
        await expect(page).toHaveTitle(/Portfolio not found/, { timeout: 15000 });
        await expect(page.getByText('Welcome to Your New Portfolio!', { exact: true })).not.toBeVisible();
        console.log('Privacy verified for Guest.');
    } catch (e) {
        console.log('FAILED to find the concealed-portfolio state. Page content was:');
        const text = await page.innerText('body');
        console.log(text);
        throw e;
    }
  });
});
