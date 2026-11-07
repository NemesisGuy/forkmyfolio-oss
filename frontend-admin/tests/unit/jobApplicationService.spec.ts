import { describe, it, expect, vi, beforeEach } from 'vitest';
import { jobApplicationService } from '@/services/jobApplicationService';
import { fetchWithAuth } from '@/services/api/apiClient';
import type { JobApplication, CreateJobApplicationRequest, UpdateJobApplicationRequest } from '@/types';

// Mock the API client
vi.mock('@/services/api/apiClient');

describe('JobApplicationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const BASE_URL = '/me/job-applications';

  it('getAll should fetch all applications', async () => {
    const mockData: JobApplication[] = [
      { uuid: '1', title: 'Test Job', companyName: 'Test Corp', status: 'APPLIED', createdAt: new Date().toISOString() }
    ];
    vi.mocked(fetchWithAuth).mockResolvedValue(mockData);

    const result = await jobApplicationService.getAll();

    expect(fetchWithAuth).toHaveBeenCalledWith(BASE_URL);
    expect(result).toEqual(mockData);
  });

  it('getByUuid should fetch a single application', async () => {
    const mockData: JobApplication = {
      uuid: '1', title: 'Test Job', companyName: 'Test Corp', status: 'APPLIED', createdAt: new Date().toISOString()
    };
    vi.mocked(fetchWithAuth).mockResolvedValue(mockData);

    const result = await jobApplicationService.getByUuid('1');

    expect(fetchWithAuth).toHaveBeenCalledWith(`${BASE_URL}/1`);
    expect(result).toEqual(mockData);
  });

  it('create should post new application data', async () => {
    const mockRequest: CreateJobApplicationRequest = { title: 'New Job', companyName: 'Corp', status: 'APPLIED' };
    const mockResponse = { uuid: '2', ...mockRequest } as unknown as JobApplication;
    vi.mocked(fetchWithAuth).mockResolvedValue(mockResponse);

    const result = await jobApplicationService.create(mockRequest);

    expect(fetchWithAuth).toHaveBeenCalledWith(BASE_URL, {
      method: 'POST',
      body: mockRequest,
    });
    expect(result).toEqual(mockResponse);
  });

  it('update should put updated data', async () => {
    const mockRequest: UpdateJobApplicationRequest = { title: 'Updated Job' };
    const mockResponse = { uuid: '1', ...mockRequest } as unknown as JobApplication;
    vi.mocked(fetchWithAuth).mockResolvedValue(mockResponse);

    const result = await jobApplicationService.update('1', mockRequest);

    expect(fetchWithAuth).toHaveBeenCalledWith(`${BASE_URL}/1`, {
      method: 'PUT',
      body: mockRequest,
    });
    expect(result).toEqual(mockResponse);
  });

  it('delete should call delete endpoint', async () => {
    vi.mocked(fetchWithAuth).mockResolvedValue(undefined);

    await jobApplicationService.delete('1');

    expect(fetchWithAuth).toHaveBeenCalledWith(`${BASE_URL}/1`, {
      method: 'DELETE',
    });
  });

  it('addNote should post note content', async () => {
    const content = 'Test Note';
    const mockResponse = { uuid: 'note-1', content };
    vi.mocked(fetchWithAuth).mockResolvedValue(mockResponse);

    const result = await jobApplicationService.addNote('app-1', content);

    expect(fetchWithAuth).toHaveBeenCalledWith(`${BASE_URL}/app-1/notes`, {
      method: 'POST',
      body: content,
    });
    expect(result).toEqual(mockResponse);
  });

  it('deleteNote should call delete endpoint for note', async () => {
    vi.mocked(fetchWithAuth).mockResolvedValue(undefined);

    await jobApplicationService.deleteNote('app-1', 'note-1');

    expect(fetchWithAuth).toHaveBeenCalledWith(`${BASE_URL}/app-1/notes/note-1`, {
      method: 'DELETE',
    });
  });
});
