import { test, expect, Page } from '@playwright/test';

test.describe('GuestPass System E2E', () => {

  const registerUser = async (page: Page) => {
    page.on('console', msg => console.log('BROWSER:', msg.text()));
    const timestamp = Date.now();
    const email = `owner-${timestamp}@example.com`;
    console.log(`E2E: Registering user ${email}`);
    await page.goto('/register');
    await page.fill('#firstName', 'Guest');
    await page.fill('#lastName', `Owner-${timestamp}`);
    await page.fill('#email', email);
    await page.fill('#password', 'Password123!');
    await page.fill('#confirmPassword', 'Password123!');
    await page.click('button[type="submit"]');

    // Handle T&C Modal
    const termsModal = page.locator('.modal.show').filter({ hasText: 'Terms of Service' });
    await expect(termsModal).toBeVisible({ timeout: 10000 });
    await page.locator('#modalTermsCheck').check();

    // Catch the registration response
    const registerResponsePromise = page.waitForResponse(resp => resp.url().includes('/auth/register') && resp.request().method() === 'POST');

    await termsModal.locator('button', { hasText: 'Confirm & Create Account' }).click();

    const registerResponse = await registerResponsePromise;
    console.log(`E2E: Registration response status: ${registerResponse.status()}`);
    if (registerResponse.status() !== 201) {
      const body = await registerResponse.json();
      console.error('E2E: Registration FAILED!', body);
    }

    // Handle Success Modal
    const successModal = page.locator('.modal.show').filter({ hasText: 'Registration Successful' });
    await expect(successModal).toBeVisible({ timeout: 15000 });
    await successModal.locator('button:has-text("Great!")').click();

    await page.waitForURL(url => url.pathname.includes('/manage/dashboard'), { timeout: 15000 });
    const slug = page.url().split('/')[3];
    console.log(`E2E: Registration successful. Slug: ${slug}`);
    return { email, slug };
  };

  test('Owner can create a pass and Recruiter can view portfolio with it', async ({ browser, page }) => {
    test.slow();

    // 1. Owner registers and navigates to Guest Passes
    const { slug } = await registerUser(page);
    await page.goto(`/${slug}/manage/guest-passes`);
    await expect(page.locator('h1', { hasText: 'Guest Pass Management' })).toBeVisible();

    // 2. Owner creates a pass
    await page.click('button:has-text("Create New Pass")');
    await page.fill('input[placeholder*="Google Recruiter"]', 'E2E Test Pass');
    await page.selectOption('select', '24'); // 24 Hours
    await page.click('button:has-text("Create Pass")');

    // Ensure modal and backdrop are gone
    await expect(page.locator('#createPassModal')).not.toBeVisible({ timeout: 10000 });
    await expect(page.locator('.modal-backdrop')).not.toBeVisible({ timeout: 10000 });
    await expect(page.locator('body')).not.toHaveClass(/modal-open/);
    await page.waitForTimeout(1000); // Standardizing pause

    // Wait for pass to appear in table
    const passRow = page.locator('tr', { hasText: 'E2E Test Pass' });
    await expect(passRow).toBeVisible();

    // Extract token
    const token = await passRow.locator('code').innerText();
    console.log(`E2E Token generated: ${token}`);

    // 3. Simulate Recruiter
    const recruiterContext = await browser.newContext();
    const recruiterPage = await recruiterContext.newPage();
    await recruiterPage.goto(`/guest-access?token=${token}`);
    await recruiterPage.waitForURL(url => url.pathname.endsWith(`/${slug}`));

    // 4. Verify Guest Experience
    await expect(recruiterPage.locator('h1')).toBeVisible();

    // 5. Verify Security
    await recruiterPage.click('#navbarUserDropdown');
    const dropdown = recruiterPage.locator('.glass-dropdown');
    await expect(dropdown.locator('text=My Dashboard')).not.toBeVisible();
    await expect(dropdown.locator('text=My Profile')).not.toBeVisible();
    await expect(dropdown.locator('text=Guest Passes')).not.toBeVisible();

    await recruiterPage.click('body');

    // 6. Verify absolute restriction
    await recruiterPage.goto(`/${slug}/projects`);
    const addProjectBtn = recruiterPage.locator('button:has-text("Add Project")');
    await expect(addProjectBtn).not.toBeVisible();

    await recruiterContext.close();
  });

  test('Accessing with invalid token shows error', async ({ page }) => {
    await page.goto('/guest-access?token=invalid-garbage-token');
    await expect(page.locator('h2', { hasText: 'Invalid Token' })).toBeVisible();
    await expect(page.locator('text=not recognized by our system')).toBeVisible();
  });

  test('Owner can revoke access and Recruiter loses access immediately', async ({ browser, page }) => {
    test.slow();

    // 1. Owner registers and creates a pass
    const { slug } = await registerUser(page);
    console.log(`E2E: Testing revocation for slug ${slug}`);
    await page.goto(`/${slug}/manage/guest-passes`);

    await page.click('button:has-text("Create New Pass")');
    await page.fill('input[placeholder*="Google Recruiter"]', 'Revocation Test Pass');
    await page.click('button:has-text("Create Pass")');

    // Ensure modal and backdrop are gone
    await expect(page.locator('#createPassModal')).not.toBeVisible({ timeout: 10000 });
    await expect(page.locator('.modal-backdrop')).not.toBeVisible({ timeout: 10000 });
    await expect(page.locator('body')).not.toHaveClass(/modal-open/);
    await page.waitForTimeout(1000);

    const passRow = page.locator('tr', { hasText: 'Revocation Test Pass' });
    await expect(passRow).toBeVisible();
    const token = await passRow.locator('code').innerText();
    console.log(`E2E: Created pass. Token: ${token}`);

    // 2. Recruiter enters the scene
    const recruiterContext = await browser.newContext();
    const recruiterPage = await recruiterContext.newPage();
    recruiterPage.on('console', msg => console.log('RECRUITER BROWSER:', msg.text()));
    await recruiterPage.goto(`/guest-access?token=${token}`);
    await recruiterPage.waitForURL(url => url.pathname.endsWith(`/${slug}`));
    console.log('E2E: Recruiter is in');

    // 3. Owner revokes the pass
    console.log('E2E: Owner revoking pass...');

    // Set up response listeners
    const deleteResponsePromise = page.waitForResponse(resp =>
      resp.request().method() === 'DELETE' &&
      resp.url().includes('/management/guest-passes'),
      { timeout: 10000 }
    );

    const dialogHandler = async (dialog: import('@playwright/test').Dialog) => {
      console.log(`E2E: Dialog appeared: ${dialog.message()}`);
      await dialog.accept();
    };
    page.on('dialog', dialogHandler);

    try {
      console.log('E2E: Clicking delete button...');
      await passRow.locator('button.btn-outline-danger').click();
      const deleteResp = await deleteResponsePromise;
      console.log(`E2E: DELETE status: ${deleteResp.status()}`);

      const status = deleteResp.status();
      if (status !== 200 && status !== 204) {
        let body = 'no body';
        try {
          body = await deleteResp.text();
        } catch (e: unknown) {
          console.warn('E2E: Could not read error body', e);
        }
        console.error(`E2E: Revocation reported FAILURE: ${status} - ${body}`);
      }
    } finally {
      page.off('dialog', dialogHandler);
    }

    console.log('E2E: Waiting for row to disappear...');
    await expect(passRow).not.toBeVisible({ timeout: 10000 });
    console.log('E2E: Pass row removed from UI');

    // 4. Recruiter tries to navigate and should be kicked out
    console.log('E2E: Verifying recruiter is kicked...');
    await recruiterPage.goto(`/${slug}/projects`);
    // Wait for redirect
    await recruiterPage.waitForURL(url => url.pathname.includes('/guest-access'), { timeout: 20000 });
    await expect(recruiterPage.getByTestId('error-title')).toHaveText('Invalid Token', { timeout: 15000 });
    console.log('E2E: Recruiter successfully kicked');

    await recruiterContext.close();
  });
});
