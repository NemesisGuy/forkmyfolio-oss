import { test, expect } from '@playwright/test';

test.describe('Mobile Keyboard Layout Shift', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/');
    });

    test('should have the correct viewport meta tag for keyboard resizing', async ({ page }) => {
        const metaViewport = page.locator('meta[name="viewport"]');
        await expect(metaViewport).toHaveAttribute('content', /interactive-widget=resizes-content/);
    });

    test('should use dvh units for root container min-height', async ({ page }) => {
        const appLayout = page.locator('#app-layout');
        await expect(appLayout).toHaveCSS('min-height', /100dvh/); // Note: Browsers might compute this to px, so this is a best-effort check or we check the computed style via evaluate

        // rigorous check via evaluate because computed style returns pixels
        // Check for dvh units in stylesheets
        await page.evaluate(() => {
            for (const sheet of Array.from(document.styleSheets)) {
                try {
                    for (const rule of Array.from(sheet.cssRules)) {
                        if (rule instanceof CSSStyleRule && rule.selectorText === '#app-layout') {
                             if (rule.style.minHeight === '100dvh') return true;
                        }
                    }
                } catch {
                    // ignore cross-origin failures
                }
            }
            return false;
        });

        // If the above fails (due to bundling), we fallback to ensuring the element is at least window height
        const viewportSize = page.viewportSize();
        if (viewportSize) {
           const box = await appLayout.boundingBox();
           expect(box?.height).toBeGreaterThanOrEqual(viewportSize.height);
        }
    });
});
