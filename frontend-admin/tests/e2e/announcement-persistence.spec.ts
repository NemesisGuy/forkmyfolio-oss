import { test, expect } from '@playwright/test';

test.describe('Announcement Persistence', () => {

    // Mock data
    const mockAnnouncements = [
        {
            uuid: '123e4567-e89b-12d3-a456-426614174000',
            title: 'System Maintenance',
            message: 'We will be down for maintenance.',
            createdAt: new Date().toISOString(),
            senderName: 'Admin',
            targetRole: 'ALL'
        },
        {
            uuid: '223e4567-e89b-12d3-a456-426614174001',
            title: 'New Feature',
            message: 'Dark mode is here!',
            createdAt: new Date().toISOString(),
            senderName: 'Dev Team',
            targetRole: 'USER'
        }
    ];

    // Authenticate the test
    test.use({ storageState: 'playwright/.auth/user.json' });

    test.beforeEach(async ({ page }) => {
        // Clear local storage before each test to ensure clean state
        await page.addInitScript(() => {
            localStorage.removeItem('forkmyfolio_read_announcements');
        });

        // Mock the API call with Regex to be safe
        await page.route(/.*\/announcements/, async route => {
            console.log('Intercepted request to:', route.request().url());
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    status: 'success',
                    data: mockAnnouncements
                })
            });
        });
    });

    test('should persist read state across reloads', async ({ page }) => {
        // Capture browser logs
        page.on('console', msg => console.log('BROWSER LOG:', msg.text()));

        // 1. Go to homepage (using the authenticated state if available, or just public if announcements show there)
        // Assuming announcements are visible to everyone or we need to login.
        // Based on the component, it calls `getMyAnnouncements`. If unauthorized, it might fail or return empty.
        // Let's assume we are testing as a logged in user or the endpoint allows it.
        // For safety, we'll go to the login page first if needed, but let's try homepage first.
        await page.goto('/');

        // 2. Initial State: Badge should show 2
        const badge = page.locator('.announcement-dropdown .badge.bg-danger');
        await expect(badge).toBeVisible();
        await expect(badge).toHaveText('2');

        // 3. Open Dropdown
        await page.locator('#announcementDropdown').click();

        // 4. Click "Mark all as read"
        await page.getByText('Mark all as read').click();

        // 5. Verify Badge Gone (Immediate UI update)
        await expect(badge).not.toBeVisible();

        // CHECK 1: Verify data is in LocalStorage BEFORE reload
        const preReloadState = await page.evaluate(() => localStorage.getItem('forkmyfolio_read_announcements'));
        if (!preReloadState || !preReloadState.includes('123e4567')) {
             throw new Error(`PRE-RELOAD FAIL: LocalStorage was '${preReloadState}'`);
        }

        // 6. RELOAD PAGE
        await page.reload();

        // 7. Verify Persistence
        // Rely on expect retry mechanism instead of strict network idle


        // CHECK 2: Verify data is in LocalStorage AFTER reload
        const postReloadState = await page.evaluate(() => localStorage.getItem('forkmyfolio_read_announcements'));
        if (!postReloadState || !postReloadState.includes('123e4567')) {
             throw new Error(`POST-RELOAD FAIL: LocalStorage was '${postReloadState}'`);
        }

        await expect(badge).not.toBeVisible();
    });
});

