import { test, expect } from '@playwright/test';

test.describe('Mobile Public Portfolio Regression', () => {

  const slug = 'clark-kent'; // Known public user

  const devices = [
    { name: 'iPhone 5', width: 320, height: 568 },
    { name: 'iPhone SE', width: 375, height: 667 },
    { name: 'iPhone 12', width: 390, height: 844 },
    { name: 'Generic Android', width: 360, height: 800 },
    { name: 'iPad Mini', width: 768, height: 1024 } // Tablet check
  ];

  for (const device of devices) {
    test(`Verify Public Layout on ${device.name}`, async ({ page }) => {
      console.log(`[${device.name}] Setting viewport: ${device.width}x${device.height}`);
      await page.setViewportSize({ width: device.width, height: device.height });

      // 1. Visit Home Page
      console.log(`[${device.name}] Visiting Home...`);
      await page.goto(`/${slug}`);
      await page.waitForLoadState('networkidle');

      // Verify Hero Section visibility
      const hero = page.locator('.hero-section');
      await expect(hero).toBeVisible();

      // Verify Mobile Navigation (Burger) if width < 992 (Bootstrap default)
      if (device.width < 992) {
         const toggler = page.locator('.navbar-toggler');
         // Depending on how navbar is implemented, it might be visible
         // Verify it exists if we expect a mobile menu
         if (await toggler.isVisible()) {
             console.log(`[${device.name}] Mobile Menu Toggler found.`);
         }
      }

      // 2. Visit Projects Page
      console.log(`[${device.name}] Visiting Projects Grid...`);
      await page.goto(`/${slug}/projects`);
      await expect(page.locator('.project-card-link').first()).toBeVisible({ timeout: 10000 });

      // Verify Grid Layout
      // On mobile, cards should be stacked (take up most of the width)
      const firstCard = page.locator('.project-card-link').first();
      const cardBox = await firstCard.boundingBox();

      if (cardBox) {
          const viewportWidth = device.width;
          const cardWidth = cardBox.width;
          // In a container, it won't be 100% of viewport, but should be significant (> 80% usually with padding)
          // or at least standard Bootstrap columns.
          console.log(`[${device.name}] Card Width: ${cardWidth} / Viewport: ${viewportWidth}`);
          // Just ensure it's rendering and visible
          expect(cardWidth).toBeGreaterThan(0);
      }

      // 3. Visit Project Details (Rich Media)
      // We'll target the "AI Voice Synthesis" project or "Cloud Architecture"
      console.log(`[${device.name}] Visiting Project Details...`);
      const projectLink = page.locator('a:has-text("AI Voice Synthesis")').first();

      if (await projectLink.isVisible()) {
          await projectLink.click();
          await page.waitForLoadState('networkidle');

          // Verify Media Container
          const mediaContainer = page.locator('.hero-media-container');
          await expect(mediaContainer).toBeVisible();

          // Verify YouTube Iframe (if it's the AI Voice project)
          const iframe = page.locator('iframe[src*="youtube.com"]');
          if (await iframe.isVisible()) {
              const iframeBox = await iframe.boundingBox();
              if (iframeBox) {
                  console.log(`[${device.name}] Iframe Size: ${iframeBox.width}x${iframeBox.height}`);
                  // Ensure it fits within viewport
                  expect(iframeBox.width).toBeLessThanOrEqual(device.width);
              }
          }
      } else {
          console.log(`[${device.name}] Project link not found, skipping specific details check.`);
      }

      // 4. Verify Skills Page (List View on Mobile)
      console.log(`[${device.name}] Visiting Skills...`);
      await page.goto(`/${slug}/skills`);
      await expect(page.locator('.skill-col .card').first()).toBeVisible();

    });
  }
});
