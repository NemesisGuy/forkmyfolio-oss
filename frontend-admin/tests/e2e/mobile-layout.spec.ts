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
        await expect(appLayout).toBeVisible();

        // 1. The stylesheet must DECLARE min-height in dvh units. Browsers
        //    resolve dvh to px in getComputedStyle, and the layout's <style> is
        //    scoped (so the selector is `#app-layout[data-v-…]`, not exactly
        //    `#app-layout`) — therefore read the CSSOM declaration block rather
        //    than asserting on the computed value.
        const declaredBlock = await page.evaluate(() => {
            for (const sheet of Array.from(document.styleSheets)) {
                try {
                    for (const rule of Array.from(sheet.cssRules)) {
                        if (rule instanceof CSSStyleRule && rule.selectorText.includes('#app-layout')) {
                            return rule.style.cssText;
                        }
                    }
                } catch {
                    // ignore cross-origin failures
                }
            }
            return null;
        });
        expect(declaredBlock).toMatch(/min-height:\s*100dvh/);

        // 2. The computed min-height must resolve to at least the viewport
        //    height (100dvh === viewport height in px on desktop).
        const computedMinHeight = await appLayout.evaluate(
            el => window.getComputedStyle(el).minHeight
        );
        const viewportSize = page.viewportSize();
        expect(parseFloat(computedMinHeight)).toBeGreaterThanOrEqual(viewportSize?.height ?? 0);
    });
});
