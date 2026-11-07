
import { fetchWithAuth } from '@/services/api/apiClient';
import type { JobApplication, CreateJobApplicationRequest, UpdateJobApplicationRequest, JobApplicationNote } from '@/types';

const BASE_URL = '/me/job-applications';

class JobApplicationService {
  async getAll(): Promise<JobApplication[]> {
    return await fetchWithAuth(BASE_URL);
  }

  async getByUuid(uuid: string): Promise<JobApplication> {
    return await fetchWithAuth(`${BASE_URL}/${uuid}`);
  }

  async create(data: CreateJobApplicationRequest): Promise<JobApplication> {
    return await fetchWithAuth(BASE_URL, {
      method: 'POST',
      body: data
    });
  }

  async update(uuid: string, data: UpdateJobApplicationRequest): Promise<JobApplication> {
    return await fetchWithAuth(`${BASE_URL}/${uuid}`, {
      method: 'PUT',
      body: data
    });
  }

  async delete(uuid: string): Promise<void> {
    await fetchWithAuth(`${BASE_URL}/${uuid}`, {
      method: 'DELETE'
    });
  }

  async addNote(applicationUuid: string, content: string): Promise<JobApplicationNote> {
    // Note: The backend expects a String body.
    // fetchWithAuth will JSON.stringify this string (e.g., "content") and set application/json.
    // Ideally, the backend should handle this or we pass an object if the API changes.
    // For now, passing the string directly.
    return await fetchWithAuth(`${BASE_URL}/${applicationUuid}/notes`, {
      method: 'POST',
      body: content
    });
  }

  async deleteNote(applicationUuid: string, noteUuid: string): Promise<void> {
    await fetchWithAuth(`${BASE_URL}/${applicationUuid}/notes/${noteUuid}`, {
      method: 'DELETE'
    });
  }
}

export const jobApplicationService = new JobApplicationService();
