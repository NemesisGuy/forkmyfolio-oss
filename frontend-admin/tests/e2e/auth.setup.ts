import { test as setup, expect } from '@playwright/test';

const authFile = 'playwright/.auth/user.json';

setup('authenticate', async ({ page }) => {
  console.log('[Auth Setup] Starting robust session initialization for Clark Kent...');

  // 1. Navigate to login
  await page.goto('/login');

  // 2. Fill credentials (supplied via environment; no credentials in source)
  await page.fill('#email', process.env.E2E_ADMIN_EMAIL ?? 'admin@forkmyfolio.com');
  await page.fill('#password', process.env.E2E_ADMIN_PASSWORD ?? '');
  await page.click('button[type="submit"]');

  // 3. Wait for redirect
  console.log('[Auth Setup] Waiting for redirect...');
  await expect(page).toHaveURL(/\/(dashboard|admin)/, { timeout: 15000 });
  console.log('[Auth Setup] Landed on dashboard.');

  // 4. Debug: Check cookies
  const cookies = await page.context().cookies();
  console.log('[Auth Setup] Captured Cookies:', JSON.stringify(cookies, null, 2));

  if (cookies.length === 0) {
    console.warn('[Auth Setup] WARNING: No cookies captured! Testing if we have localStorage...');
  }

  // 5. Save storage state
  await page.context().storageState({ path: authFile });
  console.log(`[Auth Setup] Auth state saved to ${authFile}`);

  // ---------------------------------------------------------------------------
  // 6. Idempotent fixture seeding (Phase 0.33 e2e revival).
  //
  // Several media/navigation specs assert against three canonical projects and
  // at least one skill on the admin portfolio, but `admin-user` starts empty in
  // a fresh DEV database (the old `clark-kent` sample data no longer exists).
  // Seed them through the authenticated API so every project (chromium +
  // authenticated) sees the same fixture set, creating only what is missing.
  // ---------------------------------------------------------------------------
  const token = await page.evaluate(() => localStorage.getItem('accessToken'));
  if (!token) {
    throw new Error('[Auth Setup] No accessToken in localStorage after login — cannot seed fixtures.');
  }

  const api = (method: string, path: string, body?: unknown) =>
    page.evaluate(
      ({ method, path, body, token }) =>
        fetch(path, {
          method,
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: body === undefined ? undefined : JSON.stringify(body),
        }).then(async (res) => ({ status: res.status, body: await res.text() })),
      { method, path, body, token }
    );

  // 6a. Hygiene: media-verification.spec.ts self-provisions throwaway projects
  //     titled "Media Test {ts}" and never deletes them, so they accumulate in
  //     DEV across runs. Sweep them at the start of every run (idempotent).
  const projectsForCleanup = await api('GET', '/api/v1/me/projects');
  try {
    const list: Array<{ uuid?: string; title?: string }> =
      JSON.parse(projectsForCleanup.body)?.data ?? [];
    for (const project of list) {
      if (project.uuid && project.title?.startsWith('Media Test')) {
        const removed = await api('DELETE', `/api/v1/me/projects/${project.uuid}`);
        console.log(
          `[Auth Setup] Swept leftover test project "${project.title}" (HTTP ${removed.status})`
        );
      }
    }
  } catch {
    console.warn('[Auth Setup] Project cleanup sweep failed, continuing with seeding.');
  }

  // 6b. Three projects covering every rich-media branch of mediaUtils.js:
  //     youtube embed, direct image (gif) and vimeo embed.
  const wantedProjects = [
    {
      title: 'AI Voice Synthesis Demo',
      description: 'A synthetic voice pipeline demo used by the e2e media suite.',
      mediaUrl: 'https://www.youtube.com/watch?v=ScMzIvxBSi4',
      displayOrder: 1,
    },
    {
      title: 'Glassmorphic UI Kit',
      description: 'A glassmorphic component kit rendered as a animated GIF.',
      mediaUrl: 'https://media.giphy.com/media/3o7TKTnXz5y6zY5P6E/giphy.gif',
      displayOrder: 2,
    },
    {
      title: 'Cloud Architecture Visualizer',
      description: 'An animated cloud architecture walkthrough hosted on Vimeo.',
      mediaUrl: 'https://vimeo.com/22439234',
      displayOrder: 3,
    },
  ];

  const existingProjects = await api('GET', '/api/v1/me/projects');
  let projectList: Array<{ title?: string }> = [];
  try {
    projectList = JSON.parse(existingProjects.body)?.data ?? [];
  } catch {
    projectList = [];
  }
  if (existingProjects.status >= 300) {
    console.warn(`[Auth Setup] GET /me/projects returned ${existingProjects.status}: ${existingProjects.body}`);
  }

  for (const project of wantedProjects) {
    if (projectList.some((p) => p.title === project.title)) {
      console.log(`[Auth Setup] Project already present: ${project.title}`);
      continue;
    }
    const created = await api('POST', '/api/v1/me/projects', { ...project, visible: true });
    if (created.status >= 300) {
      console.warn(`[Auth Setup] Failed to create project "${project.title}": ${created.status} ${created.body}`);
    } else {
      console.log(`[Auth Setup] Seeded project: ${project.title}`);
    }
  }

  // 6c. At least one visible skill, so the public Skills page renders a card
  //     (mobile-public-regression asserts `.skill-col .card`).
  const existingSkills = await api('GET', '/api/v1/user/skills');
  let skillList: Array<{ name?: string }> = [];
  try {
    skillList = JSON.parse(existingSkills.body)?.data ?? [];
  } catch {
    skillList = [];
  }
  if (skillList.length === 0) {
    const createdSkill = await api('POST', '/api/v1/user/skills', {
      name: 'Playwright',
      category: 'Testing',
      level: 'ADVANCED',
      visible: true,
    });
    if (createdSkill.status >= 300) {
      console.warn(`[Auth Setup] Failed to seed skill: ${createdSkill.status} ${createdSkill.body}`);
    } else {
      console.log('[Auth Setup] Seeded skill: Playwright');
    }
  } else {
    console.log(`[Auth Setup] Skills already present: ${skillList.length}`);
  }
});
