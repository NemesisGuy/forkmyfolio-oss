import { test, expect } from '@playwright/test';

// Cluster A: this spec logs in itself, so it must start from a clean slate —
// an inherited session redirects /login away from the form.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Rich Media Verification', () => {

  test('YouTube video should be embedded in project details', async ({ page }) => {
    const adminEmail = 'admin@forkmyfolio.com';
    const adminPass = process.env.E2E_ADMIN_PASSWORD ?? '';
    const youtubeUrl = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';
    const timestamp = Date.now();

    // 1. Login
    console.log('Logging in...');
    await page.goto('/login');
    await page.fill('#email', adminEmail);
    await page.fill('#password', adminPass);
    await page.click('button[type="submit"]');

    // Wait for Dashboard
    await page.waitForURL(url => url.pathname.includes('/manage/dashboard'), { timeout: 15000 });
    const slug = page.url().split('/')[3];
    console.log(`Logged in. Slug: ${slug}`);

    // 2. Go to Projects Management
    await page.goto(`/${slug}/manage/projects`);
    await expect(page.locator('.user-projects-page')).toBeVisible();

    // 3. Handle Projects List
    const uniqueTitle = `Media Test ${timestamp}`;
    const emptyState = page.locator('.text-center', { hasText: /No Projects Yet/i });

    if (await emptyState.isVisible()) {
      console.log('No projects found. Creating a new one...');
      await page.click('button:has-text("Add Project")');
    } else {
      console.log('Checking for existing media test project...');
      const existingProject = page.locator('.card', { hasText: /Media Test/i }).first();
      if (await existingProject.isVisible()) {
        console.log('Editing existing media test project...');
        await existingProject.locator('button[title="Edit Project"]').click();
      } else {
        console.log('Creating new media test project...');
        await page.click('button:has-text("Add Project")');
      }
    }

    // Fill Title
    const titleInput = page.locator('input[id="projectTitle"]');
    await expect(titleInput).toBeVisible({ timeout: 10000 });
    await titleInput.clear();
    await titleInput.fill(uniqueTitle);

    // Fill Description — REQUIRED: CreateProjectRequest.description is
    // @NotBlank @Size(min=10), so submitting with an empty textarea makes
    // POST /me/projects fail with 400 (run D: both projects' save came back
    // 400 and no project was ever created).
    const descriptionInput = page.locator('textarea[id="projectDescription"]');
    await expect(descriptionInput).toBeVisible({ timeout: 10000 });
    await descriptionInput.clear();
    await descriptionInput.fill(
      `Rich media verification fixture ${timestamp} with an embedded YouTube player.`);

    // Fill Media URL - ID is projectMediaUrl
    const mediaUrlInput = page.locator('input[id="projectMediaUrl"]');
    await expect(mediaUrlInput).toBeVisible({ timeout: 10000 });
    await mediaUrlInput.clear();
    await mediaUrlInput.fill(youtubeUrl);

    // Save — scope to the OPEN modal: the page keeps more than one
    // .modal-footer button in the DOM and the bare selector is ambiguous.
    // Await the save RESPONSE before navigating away: handleSaveProject hides
    // the modal before awaiting the POST, so page.goto() would otherwise cancel
    // the in-flight request (499) and the project never persists.
    const submitBtn = page.locator('.modal.show .modal-footer button.btn-primary');
    const saveResponse = page.waitForResponse(
      resp =>
        /\/me\/projects/.test(resp.url()) &&
        (resp.request().method() === 'POST' || resp.request().method() === 'PUT') &&
        resp.status() < 500
    );
    await submitBtn.click();
    const saved = await saveResponse;
    console.log(`Save response: ${saved.status()} ${saved.request().method()} ${saved.url()}`);
    if (!saved.ok()) {
      // Surface the validation message (e.g. which field was rejected) in the
      // test log instead of only the bare status code.
      console.log(`Save error body: ${await saved.text()}`);
    }
    expect(saved.ok()).toBeTruthy();
    // The PROJECT FORM modal must be closed after save. Scope the assertion
    // to it: a bare '.modal.show' also matches the save SUCCESS modal
    // (SuccessModal, id success-modal-*), which is EXPECTED to be open here —
    // nothing dismisses it before the next page.goto(), and its render time
    // races this poll (run H passed by timing, run I caught it open).
    await expect(page.locator('#projectFormModal')).not.toBeVisible();
    console.log(`Project "${uniqueTitle}" saved with Media URL.`);

    // 4. View Public Details
    console.log('Navigating to public projects page...');
    await page.goto(`/${slug}/projects`);

    const projectCard = page.locator('.card', { hasText: uniqueTitle }).first();
    await expect(projectCard).toBeVisible({ timeout: 15000 });

    // Click on the card or details button to go to Details Page.
    // force: the cards carry `glass-card-floating` -> `floatingGlow 8s infinite`
    // keyframes that TRANSLATE the card (up to -8px + scale) forever, so the
    // h5's bounding box changes on every frame and Playwright's "stable"
    // actionability check never passes (run E: 30s timeout on both projects
    // while the element was resolved and visible the whole time). The click
    // lands on the h5 at its current animated position, which is exactly what
    // a human does.
    const detailsLink = projectCard.locator('h5.card-title');
    await detailsLink.click({ force: true });

    // 5. Verify Embed
    console.log(`Verifying embed at ${page.url()}`);
    // Check for iframe with youtube in src
    const iframe = page.locator('iframe[src*="youtube.com"]');
    await expect(iframe).toBeVisible({ timeout: 20000 });

    // Capture Visual Proof
    const screenshotPath = `tests/e2e/screenshots/rich_media_verification_${timestamp}.png`;
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`Verification screenshot saved as ${screenshotPath}`);
  });

});
