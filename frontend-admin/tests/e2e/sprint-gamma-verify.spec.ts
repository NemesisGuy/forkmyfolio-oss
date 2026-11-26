import { test, expect } from '@playwright/test';

// Cluster A: this spec registers its own user, so it must start logged out —
// an inherited session redirects /register away from the registration form.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Sprint Gamma Verification', () => {

  // NOTE (run D de-race): the privacy test used to toggle the SHARED admin-user
  // portfolio private → public while the suite runs fullyParallel. Every other
  // spec reading /admin-user/* during that window got the SPA's branded 404
  // (run D: verify-samples saw "Fetch failed with status 404 for slug:
  // admin-user" and an empty projects grid; contact-visibility and
  // regression-login-fixes Step 3 sat in the same blast radius). This test now
  // registers its OWN throwaway owner, so it mutates a portfolio nobody else
  // reads — and since fresh portfolios are private by default, no reset step
  // is needed either: the throwaway simply stays private forever.

  test('Privacy Lock Screen should appear for private portfolio', async ({ page }) => {
    // 1. Register a throwaway owner (fresh portfolios are PRIVATE by default —
    //    see registration.spec.ts step 6b).
    const timestamp = Date.now();
    const email = `gamma-${timestamp}@example.com`;

    await page.goto('/register');
    await page.fill('#firstName', 'Gamma');
    await page.fill('#lastName', 'Tester');
    await page.fill('#email', email);
    await page.fill('#password', 'Password123!');
    await page.fill('#confirmPassword', 'Password123!');
    await page.locator('button[type="submit"]').click();

    // T&C modal
    const termsModal = page.locator('.modal.show').filter({ hasText: 'Terms of Service' });
    await expect(termsModal).toBeVisible();
    await page.locator('#modalTermsCheck').check();
    await page.locator('button', { hasText: 'Confirm & Create Account' }).click();

    // Success modal -> dashboard
    const successModal = page.locator('.modal.show').filter({ hasText: 'Registration Successful' });
    await expect(successModal).toBeVisible();
    await successModal.locator('button:has-text("Great!")').click();
    await page.waitForURL(/.*\/manage\/dashboard/, { timeout: 20000 });
    const slug = page.url().split('/')[3];
    console.log(`Registered throwaway owner with slug: ${slug}`);

    // 2. Drive the master switch to PRIVATE. The switch saves automatically —
    //    no Save button. Fresh accounts start private, so flip it public first
    //    to exercise BOTH toast paths, then back to private.
    await page.goto(`/${slug}/manage/settings/display`);
    const visibilityToggle = page.locator('#visibilityToggle');
    // The page is gated by v-if="!isLoading", so a visible toggle means the
    // profile status has loaded and the switch reflects the database.
    await expect(visibilityToggle).toBeVisible({ timeout: 10000 });

    if (await visibilityToggle.isChecked()) {
        await visibilityToggle.click();
        await expect(page.locator('.toast-body', { hasText: /portfolio is now private/i })).toBeVisible({ timeout: 10000 });
    } else {
        await visibilityToggle.click();
        await expect(page.locator('.toast-body', { hasText: /portfolio is now public/i })).toBeVisible({ timeout: 10000 });
        await visibilityToggle.click();
        await expect(page.locator('.toast-body', { hasText: /portfolio is now private/i })).toBeVisible({ timeout: 10000 });
    }

    // Reload to confirm the change actually persisted backend-side.
    await page.reload();
    await expect(page.locator('#visibilityToggle')).not.toBeChecked({ timeout: 10000 });

    // 3. Logout
    await page.locator('#navbarUserDropdown').click();
    await page.locator('.dropdown-item:has-text("Logout")').click();
    await page.locator('.modal.show button:has-text("Logout")').click();
    await page.waitForURL('/login');

    // 4. Visit the throwaway portfolio as guest
    //
    // Nuxt serves this slug (it does NOT start with `admin`, so the nginx
    // auth-regex deviation of KNOWN_ISSUES §19 does not apply): the URL is
    // intentionally NOT rewritten to /not-found — the observable contract is
    // a real HTTP 404 plus the SEO "Portfolio not found" title, exactly as
    // slug-guard.spec.js pins for missing slugs. The backend conceals the
    // private portfolio as 404 (Phase 7.4 403/404 policy), so this is the
    // same state as a nonexistent slug.
    const portfolioResponse = await page.goto(`/${slug}`);
    expect(portfolioResponse?.status()).toBe(404);
    await expect(page).toHaveTitle(/Portfolio not found/, { timeout: 10000 });

    // No reset step: the throwaway portfolio is never read by any other spec,
    // so it stays private without touching shared state.
    console.log('Privacy concealment verified on throwaway portfolio.');
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
