import { test, expect } from '@playwright/test';

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

    // Fill Media URL - ID is projectMediaUrl
    const mediaUrlInput = page.locator('input[id="projectMediaUrl"]');
    await expect(mediaUrlInput).toBeVisible({ timeout: 10000 });
    await mediaUrlInput.clear();
    await mediaUrlInput.fill(youtubeUrl);

    // Save
    const submitBtn = page.locator('.modal-footer button.btn-primary');
    await submitBtn.click();
    await expect(page.locator('.modal.show')).not.toBeVisible();
    console.log(`Project "${uniqueTitle}" saved with Media URL.`);

    // 4. View Public Details
    console.log('Navigating to public projects page...');
    await page.goto(`/${slug}/projects`);

    const projectCard = page.locator('.card', { hasText: uniqueTitle }).first();
    await expect(projectCard).toBeVisible({ timeout: 15000 });

    // Click on the card or details button to go to Details Page
    // Usually the card title or an image is a link
    const detailsLink = projectCard.locator('h5.card-title');
    await detailsLink.click();

    // 5. Verify Embed
    console.log(`Verifying embed at ${page.url()}`);
    // Check for iframe with youtube in src
    const iframe = page.locator('iframe[src*="youtube.com"]');
    await expect(iframe).toBeVisible({ timeout: 20000 });

    // Capture Visual Proof
    const screenshotPath = `rich_media_verification_${timestamp}.png`;
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`Verification screenshot saved as ${screenshotPath}`);
  });

});
