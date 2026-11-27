import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fetchWithAuth } from './apiClient';
import { restoreUser, hardDeleteUser, deleteAdminUser } from './admin.api';

vi.mock('./apiClient', () => ({
  fetchWithAuth: vi.fn(),
}));

describe('admin.api user lifecycle actions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('restoreUser POSTs to the restore endpoint and returns the restored user', async () => {
    const backendUser = {
      id: 1,
      username: 'grace',
      email: 'grace@example.com',
      firstName: 'Grace',
      lastName: 'Hopper',
      slug: 'grace',
      roles: ['ROLE_USER'],
      active: true,
    };
    vi.mocked(fetchWithAuth).mockResolvedValue(backendUser);

    const result = await restoreUser('user-123');

    expect(fetchWithAuth).toHaveBeenCalledTimes(1);
    expect(fetchWithAuth).toHaveBeenCalledWith('/admin/users/user-123/restore', {
      method: 'POST',
    });
    expect(result).toEqual(expect.objectContaining({ id: 1, active: true }));
    expect(result.roles).toEqual(['USER']); // same normalization as getAdminUsers
  });

  it('hardDeleteUser DELETEs the permanent-deletion endpoint for the given user', async () => {
    vi.mocked(fetchWithAuth).mockResolvedValue(undefined);

    await expect(hardDeleteUser('user-456')).resolves.toBeUndefined();

    expect(fetchWithAuth).toHaveBeenCalledTimes(1);
    expect(fetchWithAuth).toHaveBeenCalledWith('/admin/users/user-456/hard', {
      method: 'DELETE',
    });
  });

  it('does not confuse hard delete with the existing soft-delete endpoint', async () => {
    vi.mocked(fetchWithAuth).mockResolvedValue(undefined);

    await deleteAdminUser('user-789');
    await hardDeleteUser('user-789');

    expect(fetchWithAuth).toHaveBeenNthCalledWith(1, '/admin/users/user-789', {
      method: 'DELETE',
    });
    expect(fetchWithAuth).toHaveBeenNthCalledWith(2, '/admin/users/user-789/hard', {
      method: 'DELETE',
    });
  });

  it('propagates API errors from restore and hard delete to the caller', async () => {
    const failure = new Error('Forbidden');
    vi.mocked(fetchWithAuth).mockRejectedValue(failure);

    await expect(restoreUser('user-1')).rejects.toBe(failure);
    await expect(hardDeleteUser('user-1')).rejects.toBe(failure);
  });
});
