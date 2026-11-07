
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { jobApplicationService } from '@/services/jobApplicationService';
import * as apiClient from '@/services/api/apiClient';

// Mock the fetchWithAuth function
const fetchWithAuthMock = vi.spyOn(apiClient, 'fetchWithAuth');

describe('jobApplicationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockApp = {
    uuid: '123',
    jobTitle: 'Dev',
    companyName: 'Tech',
    status: 'APPLIED',
    createdAt: '2023-01-01'
  };

  it('getAll calls correct endpoint', async () => {
    fetchWithAuthMock.mockResolvedValue([mockApp]);
    const result = await jobApplicationService.getAll();
    expect(fetchWithAuthMock).toHaveBeenCalledWith('/me/job-applications');
    expect(result).toEqual([mockApp]);
  });

  it('getByUuid calls correct endpoint', async () => {
    fetchWithAuthMock.mockResolvedValue(mockApp);
    const result = await jobApplicationService.getByUuid('123');
    expect(fetchWithAuthMock).toHaveBeenCalledWith('/me/job-applications/123');
    expect(result).toEqual(mockApp);
  });

  it('create posts data correctly', async () => {
    const payload = { jobTitle: 'New', companyName: 'Co', jobLocation: 'Remote' };
    fetchWithAuthMock.mockResolvedValue({ ...mockApp, ...payload });

    await jobApplicationService.create(payload);

    expect(fetchWithAuthMock).toHaveBeenCalledWith('/me/job-applications', {
      method: 'POST',
      body: payload
    });
  });

  it('update puts data correctly', async () => {
    const payload = { status: 'INTERVIEWING' as const };

    await jobApplicationService.update('123', payload);

    expect(fetchWithAuthMock).toHaveBeenCalledWith('/me/job-applications/123', {
      method: 'PUT',
      body: payload
    });
  });

  it('delete sends correct request', async () => {
    await jobApplicationService.delete('123');
    expect(fetchWithAuthMock).toHaveBeenCalledWith('/me/job-applications/123', {
      method: 'DELETE'
    });
  });

  it('addNote sends content as body', async () => {
    await jobApplicationService.addNote('123', 'My Note');
    expect(fetchWithAuthMock).toHaveBeenCalledWith('/me/job-applications/123/notes', {
      method: 'POST',
      body: 'My Note'
    });
  });

  it('deleteNote sends correct request', async () => {
    await jobApplicationService.deleteNote('123', 'note-456');
    expect(fetchWithAuthMock).toHaveBeenCalledWith('/me/job-applications/123/notes/note-456', {
      method: 'DELETE'
    });
  });

  // ---------------------------------------------------------------------
  // TRK-400 regression (2026-11-04)
  // POST /api/v1/me/job-applications returned HTTP 400 "Job title cannot
  // be blank" because the frontend sent `title`/`location` while the
  // backend contract (CreateJobApplicationRequest) requires
  // `jobTitle`/`jobLocation`. These tests pin the corrected contract.
  // ---------------------------------------------------------------------
  it('create sends the backend contract keys (jobTitle/jobLocation), never legacy keys', async () => {
    const payload = {
      jobTitle: 'Mid Java dev',
      companyName: 'TestCo Ltd',
      jobLocation: 'Remote',
      initialNote: 'Repro of user-reported 400'
    };
    fetchWithAuthMock.mockResolvedValue({ ...mockApp, ...payload, status: 'DRAFT' });

    await jobApplicationService.create(payload);

    const call = fetchWithAuthMock.mock.calls.find(([url]) => url === '/me/job-applications');
    expect(call).toBeDefined();
    const body = call![1]!.body as Record<string, unknown>;
    expect(body).toHaveProperty('jobTitle', 'Mid Java dev');
    expect(body).toHaveProperty('jobLocation', 'Remote');
    expect(body).not.toHaveProperty('title');     // legacy key must never reappear
    expect(body).not.toHaveProperty('location');  // legacy key must never reappear
    expect(body).not.toHaveProperty('status');    // create contract has no status
  });

  it('update sends the backend contract keys for reads and writes', async () => {
    const payload = { jobTitle: 'Updated title', status: 'OFFER' as const };
    fetchWithAuthMock.mockResolvedValue({ ...mockApp, ...payload });

    await jobApplicationService.update('123', payload);

    const call = fetchWithAuthMock.mock.calls.find(([url]) => url === '/me/job-applications/123');
    expect(call).toBeDefined();
    const body = call![1]!.body as Record<string, unknown>;
    expect(body).toHaveProperty('jobTitle', 'Updated title');
    expect(body).not.toHaveProperty('title');
  });
});
