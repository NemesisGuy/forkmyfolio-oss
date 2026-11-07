
import { test, expect } from '@playwright/test';

test('Basic Performance Sanity Check', async ({ page }) => {
  // Navigate to proper page
  await page.goto('/');

  // Wait for load
  await page.waitForLoadState('networkidle');

  // Measure vital web vitals approximations
  const performanceTiming = await page.evaluate(() => {
    const paint = performance.getEntriesByType('paint');
    const fcp = paint.find(entry => entry.name === 'first-contentful-paint')?.startTime;
    return { fcp };
  });

  console.log(`FCP: ${performanceTiming.fcp}ms`);

  // Basic assertion - FCP should be decent (< 1.8s is fast, < 3s is okay)
  // We'll set a loose 3s limit for the test environment
  expect(performanceTiming.fcp).toBeLessThan(3000);

  // Check for large layout shifts (CLS approximation not easily available via simple API without observer,
  // but we can check if content is stable).
  // For now, FCP is a good proxy for "is it fast enough to be usable".
});
