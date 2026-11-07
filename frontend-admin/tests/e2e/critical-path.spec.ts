import { test, expect, Page } from '@playwright/test';

test.describe('Critical Path: Multi-User Profile Visibility', () => {

  // --- Helper Functions ---

  interface User {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }

  const registerUser = async (page: Page, user: User) => {
    console.log(`[${user.email}] Starting registration...`);
    await page.goto('/register');
    await page.fill('#firstName', user.firstName);
    await page.fill('#lastName', user.lastName);
    await page.fill('#email', user.email);
    await page.fill('#password', user.password);
    await page.fill('#confirmPassword', user.password);
    await page.click('button[type="submit"]');

    // Handle T&C Modal
    const termsModal = page.locator('.modal.show').filter({ hasText: 'Terms of Service' });
    await expect(termsModal).toBeVisible({ timeout: 10000 });
    await page.locator('#modalTermsCheck').check();
    await termsModal.locator('button', { hasText: 'Confirm & Create Account' }).click();

    // Handle Success Modal
    const successModal = page.locator('.modal.show').filter({ hasText: 'Registration Successful' });
    await expect(successModal).toBeVisible({ timeout: 10000 });
    await successModal.locator('button:has-text("Great!")').click();

    // Wait for Dashboard Redirect and extract slug
    await page.waitForURL(url => url.pathname.includes('/manage/dashboard'), { timeout: 15000 });
    const pathParts = new URL(page.url()).pathname.split('/');
    console.log(`[${user.email}] Registered with slug: ${pathParts[1]}`);
    return pathParts[1]; // /:slug/manage/dashboard
  };

  const logout = async (page: Page) => {
    console.log('Logging out...');
    const userDropdown = page.locator('#navbarUserDropdown');

    // Ensure dropdown is visible and interactive
    await expect(userDropdown).toBeVisible();
    await userDropdown.click();

    // Click logout in dropdown
    const logoutItem = page.locator('.dropdown-item:has-text("Logout")');
    await expect(logoutItem).toBeVisible();
    await logoutItem.click();

    // Confirm Logout Modal
    const confirmModal = page.locator('.modal.show').filter({ hasText: 'Confirm Logout' });
    await expect(confirmModal).toBeVisible();
    await confirmModal.locator('button:has-text("Logout")').click();

    // Wait for redirect to Login
    await page.waitForURL(url => url.pathname === '/login', { timeout: 10000 });
    console.log('Logout complete.');
  };

  const login = async (page: Page, email: string, password: string) => {
    console.log(`Logging in as ${email}...`);
    await page.goto('/login');
    await page.fill('#email', email);
    await page.fill('#password', password);
    await page.click('button[type="submit"]');
    // Wait for successful redirect to dashboard
    await page.waitForURL(url => url.pathname.includes('/manage/dashboard'), { timeout: 15000 });
    console.log('Login successful.');
  };

  const setPublicVisibility = async (page: Page, slug: string, isPublic: boolean) => {
    console.log(`Setting visibility for ${slug} to ${isPublic}...`);
    await page.goto(`/${slug}/manage/settings/display`);
    const masterSwitch = page.locator('#visibilityToggle');

    await expect(masterSwitch).toBeVisible();
    const isChecked = await masterSwitch.isChecked();
    if (isChecked !== isPublic) {
      // Toggle logic
      await masterSwitch.click();

      // Verification logic: Wait for the visual state to change or toast
      // Instead of relying on a fragile toast text, we check the label/icon
      if (isPublic) {
        // Wait for the SUCCESS TOAST to confirm backend persistence
        await expect(page.locator('.toast-body', { hasText: /portfolio is now public/i })).toBeVisible({ timeout: 10000 });
      } else {
         await expect(page.locator('.toast-body', { hasText: /portfolio is now private/i })).toBeVisible({ timeout: 10000 });
      }

      // CRITICAL: Reload the page to ensure the setting was actually PERSISTED to the backend
      // and not just updated in the local frontend store/UI.
      await page.reload();
      const masterSwitchReloaded = page.locator('#visibilityToggle');
      await expect(masterSwitchReloaded).toBeVisible();
      // Re-verify state after reload
      if (isPublic) {
          await expect(masterSwitchReloaded).toBeChecked();
      } else {
          await expect(masterSwitchReloaded).not.toBeChecked();
      }
    }
  };

  const addProject = async (page: Page, slug: string, title: string) => {
    console.log(`Adding project "${title}" for ${slug}...`);
    await page.goto(`/${slug}/manage/projects`);

    // Explicitly wait for the list or empty state to load to ensure hydration
    await expect(page.locator('.user-projects-page')).toBeVisible();

    // RELOAD to ensure clean modal state (mitigate SPA ghosting)
    await page.reload();
    await expect(page.locator('.user-projects-page')).toBeVisible();

    // Explicit wait for hydration/animations (Pragmatic fix for flaky CI interaction)
    await page.waitForTimeout(2000);

    // Try to find ANY add button and ensure it's visible
    const addBtn = page.locator('button.btn-primary').filter({ hasText: /Add.*Project/ }).first();
    await expect(addBtn).toBeVisible({ timeout: 10000 });
    await addBtn.hover(); // Hover to trigger any css transitions
    await addBtn.click();
    console.log(`Clicked Add Project button.`);

    // Simplified Locator: ID is unique
    const titleInput = page.locator('input[id="projectTitle"]');
    await expect(titleInput).toBeVisible({ timeout: 10000 });
    await titleInput.fill(title);
    await page.fill('#projectDescription', 'This is a valid project description for testing purposes.');

    // Submit via Button click (more reliable than Enter key in some environments)
    const submitBtn = page.locator('.modal-footer .btn-primary').filter({ hasText: /Add.*Project/ }).first();
    await expect(submitBtn).toBeVisible();
    await submitBtn.click();

    // Handle optional Success Modal
    const successModal = page.locator('.modal.show').filter({ hasText: 'Success' });
    try {
      await expect(successModal).toBeVisible({ timeout: 5000 });
      await successModal.locator('button:has-text("Great!")').click();
    } catch {
      // Ignore if it doesn't appear (though it seems it does)
      console.log('Success modal did not appear or was not needed.');
    }

    // Wait for ALL modals to disappear
    await expect(page.locator('.modal.show')).not.toBeVisible();

    // Verify addition
    await expect(page.locator('.card-title', { hasText: title }).first()).toBeVisible();
    console.log(`Project "${title}" added.`);
  };

  // --- Test Flow ---

  test('Complete Multi-User Visibility Flow', async ({ page }) => {
    test.slow(); // Triple the default timeout

    // Defines Users
    const timestamp = Date.now();
    const userA = {
      firstName: 'Public', lastName: 'UserA',
      email: `userA-${timestamp}@example.com`, password: 'Password123!'
    };
    const userB = {
      firstName: 'Private', lastName: 'UserB',
      email: `userB-${timestamp}@example.com`, password: 'Password123!'
    };

    // 1. Setup User A (Public)
    const slugA = await registerUser(page, userA);
    // Ensure Public
    await setPublicVisibility(page, slugA, true);
    // Add Project
    await addProject(page, slugA, `Project A (${slugA})`);
    await logout(page);

    // 2. Setup User B (Private)
    const slugB = await registerUser(page, userB);
    // Ensure Private (Default usually private, but enforce it)
    await setPublicVisibility(page, slugB, false);
    // Add Project
    await addProject(page, slugB, `Project B (${slugB})`);
    await logout(page);

    // 3. User A logs in -> Views B (Private)
    console.log('--- User A viewing User B (Private) ---');
    await login(page, userA.email, userA.password);

    // Visit B's public project page
    await page.goto(`/${slugB}/projects`);

    // Expect 'Portfolio Restricted' page (since B is private)
    await expect(page.locator('h2', { hasText: 'Portfolio Restricted' })).toBeVisible();
    await expect(page.locator('text=Project B')).not.toBeVisible();

    await logout(page);

    // 4. User B logs in -> Views A (Public)
    console.log('--- User B viewing User A (Public) ---');
    await login(page, userB.email, userB.password);

    // Visit A's public project page
    await page.goto(`/${slugA}/projects`);

    // Expect Success (Visible)
    await expect(page.locator('h1').filter({ hasText: 'Projects' })).toBeVisible();
    await expect(page.locator('.card-title', { hasText: `Project A (${slugA})` }).first()).toBeVisible();

    await logout(page);

    // 5. Anonymous User -> Views A (Public) & B (Private)
    console.log('--- Anonymous Checks ---');

    // Check A (Public) from Anonymous
    await page.goto(`/${slugA}/projects`);
    await expect(page.locator('.card-title', { hasText: `Project A (${slugA})` })).toBeVisible();

    // Check B (Private) from Anonymous
    await page.goto(`/${slugB}/projects`);
    await expect(page.locator('h2', { hasText: 'Portfolio Restricted' })).toBeVisible();
    await expect(page.locator('text=Project B')).not.toBeVisible();

  });

});
