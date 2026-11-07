import { describe, it, expect, vi, beforeEach } from 'vitest';
import { guestPassService } from './guestPassService';
import {
  getMyGuestPasses,
  createGuestPass,
  revokeGuestPass
} from './api/guestPass.api';
import type { GuestPass } from '@/types';

vi.mock('./api/guestPass.api', () => ({
  getMyGuestPasses: vi.fn(),
  createGuestPass: vi.fn(),
  revokeGuestPass: vi.fn(),
  loginAsGuest: vi.fn(),
}));

describe('GuestPassService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getMyPasses should return data from api', async () => {
    const mockData = [{ uuid: '123', label: 'Test' }] as unknown as GuestPass[];
    vi.mocked(getMyGuestPasses).mockResolvedValue(mockData);

    const result = await guestPassService.getMyPasses();

    expect(result).toEqual(mockData);
    expect(getMyGuestPasses).toHaveBeenCalledTimes(1);
  });

  it('create should call api with label and duration', async () => {
    const mockPass = { label: 'New' } as unknown as GuestPass;
    vi.mocked(createGuestPass).mockResolvedValue(mockPass);

    await guestPassService.create('New', 24);

    expect(createGuestPass).toHaveBeenCalledWith('New', 24);
  });

  it('revoke should call api with uuid', async () => {
    vi.mocked(revokeGuestPass).mockResolvedValue(undefined);

    await guestPassService.revoke('uuid-123');

    expect(revokeGuestPass).toHaveBeenCalledWith('uuid-123');
  });
});
