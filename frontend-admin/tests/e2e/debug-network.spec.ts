import { test } from '@playwright/test';

test('Debug Network Requests', async ({ page }) => {
  const projectsUrl = '/admin-user/projects';

  page.on('request', request =>
    console.log('>>', request.method(), request.url())
  );
  page.on('response', response =>
    console.log('<<', response.status(), response.url())
  );

  // Log console messages too, just in case
  page.on('console', msg => console.log(`[Browser Console] ${msg.text()}`));

  console.log('Navigating to ' + projectsUrl);
  await page.goto(projectsUrl);

  // Wait for a bit to capture everything
  await page.waitForTimeout(10000);
});
