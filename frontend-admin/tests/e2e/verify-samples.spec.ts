import { test, expect } from '@playwright/test';

test.describe('Rich Media Project Verification', () => {

  const projectsUrl = '/admin-user/projects';

  test('Verify Rich Media Samples render correctly on details page', async ({ page }) => {
    // Stream console logs from the browser to the Playwright runner
    page.on('console', msg => console.log(`[Browser Console] ${msg.text()}`));

    // 1. Navigate to the public projects list
    await page.goto(projectsUrl);

    // Wait for at least one project card to ensure list is loaded
    await expect(page.locator('.project-card-link').first()).toBeVisible({ timeout: 15000 });

    // 2. Verify AI Voice Synthesis Demo (YouTube)
    console.log('Verifying YouTube Project...');
    await page.locator('.project-card-link', { hasText: 'AI Voice Synthesis Demo' }).click();

    // Expect details page to load
    await expect(page.locator('h1', { hasText: 'AI Voice Synthesis Demo' })).toBeVisible();

    // Check for YouTube Iframe
    const youtubeFrame = page.locator('iframe[src*="youtube.com"]');
    await expect(youtubeFrame).toBeVisible();
    await expect(youtubeFrame).toHaveAttribute('src', /ScMzIvxBSi4/);
    // Verify Autoplay param
    await expect(youtubeFrame).toHaveAttribute('src', /autoplay=1/);

    // Verify Share Button
    const shareBtn = page.locator('button', { hasText: 'Share Project' });
    await expect(shareBtn).toBeVisible();

    // Mock clipboard API to ensure it succeeds in headless mode
    await page.evaluate(() => {
        Object.defineProperty(navigator, 'clipboard', {
            value: {
                writeText: async () => Promise.resolve(),
            },
        });
    });

    // Click and verify text change
    await shareBtn.click();
    await expect(page.locator('button', { hasText: 'Copied!' })).toBeVisible();

    // Navigate back to list
    await page.goto(projectsUrl);
    await expect(page.locator('.project-card-link').first()).toBeVisible({ timeout: 15000 });

    // Verify Thumbnails in List View
    // The YT project should have a YT thumbnail
    const ytCardValues = page.locator('.project-card-link', { hasText: 'AI Voice Synthesis Demo' });
    const ytImg = ytCardValues.locator('img');
    await expect(ytImg).toHaveAttribute('src', /img\.youtube\.com/);


    // 3. Verify Glassmorphic UI Kit (GIF)
    console.log('Verifying GIF Project...');
    await page.locator('.project-card-link', { hasText: 'Glassmorphic UI Kit' }).click();
    await expect(page.locator('h1', { hasText: 'Glassmorphic UI Kit' })).toBeVisible();
    // Check for GIF Image (now supported via mediaUrl)
    const gifImage = page.locator('img[src*="giphy.com"]');
    await expect(gifImage).toBeVisible();

    // Navigate back
    await page.goto(projectsUrl);
    await expect(page.locator('.project-card-link').first()).toBeVisible({ timeout: 15000 });

    // 4. Verify Cloud Architecture Visualizer (Vimeo)
    console.log('Verifying Vimeo Project...');
    await page.locator('.project-card-link', { hasText: 'Cloud Architecture Visualizer' }).click();
    await expect(page.locator('h1', { hasText: 'Cloud Architecture Visualizer' })).toBeVisible();
    // Check for Vimeo Iframe
    const vimeoFrame = page.locator('iframe[src*="vimeo.com"]');
    await expect(vimeoFrame).toBeVisible();
    await expect(vimeoFrame).toHaveAttribute('src', /22439234/);
    // Verify Autoplay
    await expect(vimeoFrame).toHaveAttribute('src', /autoplay=1/);

    console.log('All rich media samples verified successfully.');
  });

});
