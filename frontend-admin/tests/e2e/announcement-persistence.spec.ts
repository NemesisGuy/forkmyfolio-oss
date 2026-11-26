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
        // Read state lives on the BACKEND, not in localStorage: the dropdown
        // fetches GET /announcements (per-item `read` flag) and persists
        // "Mark all as read" via POST /announcements/read/all. The old spec
        // asserted `forkmyfolio_read_announcements`, a key that exists nowhere
        // in the application — so it could only ever fail (run D).
        //
        // Stateful mock: the POST flips every item to read, and the GET (after
        // a reload) reflects it — proving the same round-trip the real backend
        // provides, since route handlers survive navigation/reload.
        let readAll = false;

        await page.route(/.*\/announcements.*/, async route => {
            const request = route.request();
            const url = request.url();
            console.log('Intercepted request to:', url);

            if (request.method() === 'POST' && /\/announcements\/read\/all\/?$/.test(url)) {
                readAll = true;
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({ status: 'success', data: null })
                });
                return;
            }

            if (request.method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        status: 'success',
                        data: mockAnnouncements.map(a => ({ ...a, read: readAll }))
                    })
                });
                return;
            }

            // Any other announcements call (e.g. per-item read) — accept harmlessly.
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ status: 'success', data: null })
            });
        });
    });

    test('should persist read state across reloads', async ({ page }) => {
        // Capture browser logs
        page.on('console', msg => console.log('BROWSER LOG:', msg.text()));

        // 1. Go to an authenticated SPA page. The Nuxt landing page (/) has no
        // AnnouncementDropdown, so the badge can only be asserted on a page
        // served by the Vue SPA with a logged-in navbar.
        await page.goto('/admin-user/manage/dashboard');

        // 2. Initial State: Badge should show 2
        // toContainText — the badge renders the count plus visually-hidden
        // screen-reader text ("2 unread announcements"), so an exact toHaveText
        // fails on the full accessible string.
        const badge = page.locator('.announcement-dropdown .badge.bg-danger');
        await expect(badge).toBeVisible();
        await expect(badge).toContainText('2');

        // 3. Open Dropdown — both items carry the unread dot
        await page.locator('#announcementDropdown').click();
        const unreadDots = page.locator('.announcement-dropdown .unread-dot');
        await expect(unreadDots).toHaveCount(2);

        // 4. Click "Mark all as read" -> POST /announcements/read/all (stateful mock)
        await page.getByText('Mark all as read').click();

        // 5. Verify Badge Gone (immediate UI update from the API round-trip)
        await expect(badge).not.toBeVisible();
        await expect(unreadDots).toHaveCount(0);

        // 6. RELOAD PAGE — the route mock (and its readAll state) survives
        // navigation, so the GET that follows is the "after" snapshot.
        await page.reload();

        // 7. Verify Persistence: read state came back from the API, so the
        // badge and the unread dots must both stay gone after a fresh load.
        await expect(badge).not.toBeVisible();
        await expect(unreadDots).toHaveCount(0);
    });
});
