import { test, expect } from '@playwright/test';

test('Verify Rich Media with Mocked API', async ({ page }) => {
  const projectsUrl = '/admin-user/projects';

  // Mock the Projects API
  await page.route('**/api/v1/portfolios/admin-user/projects', async route => {
    console.log('Mocking Projects API Hit!');
    const json = [
      {
        uuid: 'p1',
        title: 'AI Voice Synthesis Demo',
        displayOrder: 1,
        visible: true,
        mediaUrl: 'https://www.youtube.com/watch?v=ScMzIvxBSi4',
        skills: []
      },
      {
        uuid: 'p2',
        title: 'Glassmorphic UI Kit',
        displayOrder: 2,
        visible: true,
        mediaUrl: 'https://giphy.com/gifs/code-ui-animation-3o7TKTnXz5y6zY5P6E',
        skills: []
      },
      {
        uuid: 'p3',
        title: 'Cloud Architecture Visualizer',
        displayOrder: 3,
        visible: true,
        mediaUrl: 'https://vimeo.com/22439234',
        skills: []
      }
    ];
    await route.fulfill({ json: { status: 'success', data: json } });
  });

  // Mock the Headers calls if necessary (Profile, Skills etc) - assuming they work or don't block
  // But wait, fetchPortfolio calls ALL of them. If one fails, it errors.
  // I must mock ALL of them to be safe.

  await page.route('**/api/v1/portfolios/admin-user', async route => {
     await route.fulfill({ json: { status: 'success', data: {
        firstName: 'Test', lastName: 'User', slug: 'admin-user',
        profile: { tagline: 'Tester' }
     }} });
  });
  await page.route('**/api/v1/portfolios/admin-user/skills', async route => route.fulfill({ json: { status: 'success', data: [] } }));
  await page.route('**/api/v1/portfolios/admin-user/experience', async route => route.fulfill({ json: { status: 'success', data: [] } }));
  await page.route('**/api/v1/portfolios/admin-user/qualifications', async route => route.fulfill({ json: { status: 'success', data: [] } }));
  await page.route('**/api/v1/portfolios/admin-user/testimonials', async route => route.fulfill({ json: { status: 'success', data: [] } }));


  // 1. Navigate to the public projects list
  await page.goto(projectsUrl);
  await expect(page.locator('.project-card-link').first()).toBeVisible({ timeout: 10000 });

  // 2. Verify YouTube
  await page.locator('.project-card-link', { hasText: 'AI Voice Synthesis Demo' }).click();
  // We need to mock the SINGLE project fetch too if it re-fetches (ProjectDetails fallback)
  // But store has it.
  await expect(page.locator('iframe[src*="youtube.com"]')).toBeVisible();

  // Back
  await page.goto(projectsUrl);
  await expect(page.locator('.project-card-link').first()).toBeVisible();

  // 3. Verify GIF
  await page.locator('.project-card-link', { hasText: 'Glassmorphic UI Kit' }).click();
  await expect(page.locator('img[src*="giphy.com"]')).toBeVisible();

  // Back
   await page.goto(projectsUrl);
   await expect(page.locator('.project-card-link').first()).toBeVisible();

   // 4. Verify Vimeo
    await page.locator('.project-card-link', { hasText: 'Cloud Architecture Visualizer' }).click();
    await expect(page.locator('iframe[src*="vimeo.com"]')).toBeVisible();

});
