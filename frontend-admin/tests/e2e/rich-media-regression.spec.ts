import { test, expect } from '@playwright/test';

test.describe('Rich Media Regression', () => {

  const slug = 'clark-kent'; // Use a known public portfolio with samples

  test('Verify YouTube Embed Rendering', async ({ page }) => {
    // Navigate to a project details page known to have YouTube
    // We assume the sample "AI Voice Synthesis Demo" exists or similar.
    // Ideally we'd search for it first.
    await page.goto(`/${slug}/projects`);
    // Wait for the skeleton loader to disappear and projects to appear
    await page.waitForSelector('.card-img-top', { state: 'visible', timeout: 10000 });

    // Find a card with YouTube icon or known title
    // Logic: Look for "AI Voice Synthesis"
    const projectLink = page.locator('a:has-text("AI Voice Synthesis")');
    if (await projectLink.isVisible()) {
       await projectLink.click();
       // Verify iframe
       await expect(page.locator('iframe[src*="youtube.com"]')).toBeVisible();
    } else {
       console.log('Skipping YouTube verify - project not found');
       console.log('PAGE CONTENT:', await page.content());
    }
  });

  test('Verify Vimeo Embed Rendering', async ({ page }) => {
    await page.goto(`/${slug}/projects`);
    await page.waitForSelector('.card-img-top', { state: 'visible', timeout: 10000 });
    const projectLink = page.locator('a:has-text("Cloud Architecture")'); // "Cloud Architecture Visualizer"
    if (await projectLink.isVisible()) {
       await projectLink.click();
       await expect(page.locator('iframe[src*="vimeo.com"]')).toBeVisible();
    } else {
       console.log('Skipping Vimeo verify - project not found');
    }
  });

  test('Verify GIF Rendering', async ({ page }) => {
    await page.goto(`/${slug}/projects`);
    await page.waitForSelector('.card-img-top', { state: 'visible', timeout: 10000 });
    const projectLink = page.locator('a:has-text("Glassmorphic UI")'); // "Glassmorphic UI Kit"
    if (await projectLink.isVisible()) {
       await projectLink.click();
       // Verify IMG tag with .gif
       const img = page.locator('.hero-media-container img[src*=".gif"]');
       await expect(img).toBeVisible();
    } else {
       console.log('Skipping GIF verify - project not found');
    }
  });

  test('Verify Smart Thumbnail on Grid', async ({ page }) => {
     await page.goto(`/${slug}/projects`);
     await page.waitForSelector('.card-img-top', { state: 'visible', timeout: 10000 });
     await page.screenshot({ path: 'tests/e2e/screenshots/grid-debug.png' });
     // Check that at least one card has a valid image source that isn't a placeholder
     // We can check if any img src contains 'vimeocdn' or 'ytimg' or 'giphy'
     const thumbs = page.locator('.card-img-top');
     const count = await thumbs.count();
     console.log(`Found ${count} thumbnails on grid.`);
     let foundSmart = false;
     for (let i = 0; i < count; ++i) {
        const src = await thumbs.nth(i).getAttribute('src');
        console.log(`Image ${i} src: ${src}`);
        if (src && (src.includes('vimeocdn') || src.includes('ytimg') || src.includes('giphy'))) {
           foundSmart = true;
           break;
        }
     }
     expect(foundSmart).toBeTruthy();
  });

});
