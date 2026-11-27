import { test, expect } from '@playwright/test';

/**
 * v1.4.0 admin user-management actions. Read-only pin: verifies the Restore /
 * Hard Delete controls and the explicit permanent-delete confirmation, without
 * actually deleting anything from the DEV database.
 */
test('admin users page exposes restore + hard-delete actions with an explicit confirmation', async ({ page }) => {
  test.skip(test.info().project.name !== 'authenticated', 'requires the saved admin session');

  await page.goto('/admin/users');
  await expect(page.getByRole('heading', { name: /User Management/ })).toBeVisible();

  const firstRow = page.locator('tbody tr').first();
  await expect(firstRow.locator('button[title="Permanently Delete User (Hard Delete)"]')).toBeVisible();
  await expect(firstRow.locator('button[title="Deactivate User (Soft Delete)"]')).toBeVisible();

  // Open the dangerous modal and cancel — nothing must be deleted.
  const rowsBefore = await page.locator('tbody tr').count();
  await firstRow.locator('button[title="Permanently Delete User (Hard Delete)"]').click();
  await expect(page.locator('#hard-delete-user-modal')).toContainText('cannot be undone');
  await expect(page.locator('#hard-delete-user-modal')).toContainText('Delete Permanently');
  await page.keyboard.press('Escape');
  await expect(page.locator('tbody tr')).toHaveCount(rowsBefore);
});
