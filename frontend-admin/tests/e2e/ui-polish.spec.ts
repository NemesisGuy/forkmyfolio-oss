import { test, expect } from '@playwright/test';

test.describe('UI & Performance Regression', () => {

  test('Admin Navigation Performance (Cold vs Warm)', async ({ page }) => {
    console.log('[Test] Starting Admin Performance Audit...');

    // 1. LOGIN
    await page.goto('/login');
    await page.fill('input[type="email"]', 'admin@forkmyfolio.com');
    await page.fill('input[type="password"]', process.env.E2E_ADMIN_PASSWORD ?? '');
    await page.click('button[type="submit"]');

    try {
      await page.waitForURL(url => url.pathname.includes('/manage/dashboard'), { timeout: 15000 });
      console.log('[Test] Logged in successfully');
    } catch (e) {
      await page.screenshot({ path: 'login-failure.png' });
      throw e;
    }

    const adminStart = Date.now();

    // COLD LOAD: Initial Dashboard entry
    const coldStart = Date.now();
    await page.goto('/admin');
    await page.waitForSelector('.admin-dashboard-page', { state: 'visible' });
    const coldLoadTime = Date.now() - coldStart;

    // WARM TRANSITIONS
    const navItems = [
      { name: 'User Management', selector: '.admin-users-page', buttonText: 'Go to User Management' },
      { name: 'Application Settings', selector: '.admin-settings-page', buttonText: 'Configure Settings' },
      { name: 'Statistics', selector: '.admin-stats-page', buttonText: 'Go to Statistics' },
      { name: 'Messages', selector: '.admin-messages-page', buttonText: 'Go to Messages' },
      { name: 'System Tools', selector: '.admin-system-backup-page', buttonText: 'Go to System Tools' }
    ];

    const warmTimes: Record<string, number> = {};

    for (const item of navItems) {
      const clickStart = Date.now();
      await page.click(`.interactive-card-lift:has-text("${item.buttonText}") .btn`);
      await page.waitForSelector(item.selector, { state: 'visible' });
      warmTimes[item.name] = Date.now() - clickStart;

      // Go back to Dashboard using a specific CSS selector to avoid ambiguity
      await page.click('nav.navbar a[href="/admin"]');
      await page.waitForSelector('.admin-dashboard-page', { state: 'visible' });
    }

    const adminTotal = Date.now() - adminStart;

    console.log('\n=== PERFORMANCE AUDIT REPORT ===');
    console.log(`Cold Load (Initial): ${coldLoadTime}ms`);
    console.log('Warm Transitions (TTI):');
    for (const [name, time] of Object.entries(warmTimes)) {
      console.log(`  - ${name.padEnd(20)}: ${time}ms`);
    }
    console.log('-------------------------------------------');
    console.log(`TOTAL ADMIN FLOW TIME  : ${adminTotal}ms`);
    console.log('===========================================\n');

    expect(coldLoadTime).toBeLessThan(1500);
    for (const time of Object.values(warmTimes)) {
      expect(time).toBeLessThan(600);
    }
  });

  test('Public Guest Experience Performance', async ({ page }) => {
    const publicStart = Date.now();
    await page.goto('/');
    await page.waitForSelector('.landing-page', { state: 'visible' });
    const landingTime = Date.now() - publicStart;

    console.log(`Public Landing Page (Cold): ${landingTime}ms`);
    expect(landingTime).toBeLessThan(1200);
  });
});
