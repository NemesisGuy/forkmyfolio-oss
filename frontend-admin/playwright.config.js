import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    // NOTE (Phase 7.3): default points at the local staging deployment
    // (docker proxy, port 8189). Override with BASE_URL for other targets.
    baseURL: process.env.BASE_URL || 'http://localhost:8189',
    trace: 'on-first-retry',
  },
  projects: [
    // Setup project
    { name: 'setup', testMatch: /auth\.setup\.ts/ },

    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
      },
      // The setup project also seeds the shared e2e fixtures (projects/skills
      // on the admin portfolio), so BOTH projects must wait for it — not just
      // `authenticated`. Without this the anonymous project raced the seeder
      // and specs such as verify-samples saw an empty projects grid.
      dependencies: ['setup'],
    },

    // Authenticated project
    {
      name: 'authenticated',
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'playwright/.auth/user.json',
      },
      dependencies: ['setup'],
    },
  ],
});
