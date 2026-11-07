import { fetchWithAuth } from './apiClient';
import { authService } from '@/services/authService';
import type {
  User,
  Profile,
  Project,
  Skill,
  Experience,
  Qualification,
  Testimonial,
  Portfolio,
  AdminSetting
} from '@/types';

/**
 * @file src/services/api/public.api.ts
 * @description API functions for publicly accessible data and portfolio-related actions.
 */

// --- Named Exports for use with the barrel file (index.ts) ---

/**
 * Fetches the core profile and user data for a public portfolio.
 */
export const getPortfolioBySlug = (slug: string): Promise<Portfolio> => {
  return fetchWithAuth<Portfolio>(`/portfolios/${slug}`, { method: 'GET' }, true);
};

/**
 * Fetches a user's public projects by their slug.
 */
export const getPortfolioProjects = (slug: string): Promise<Project[]> =>
  fetchWithAuth<Project[]>(`/portfolios/${slug}/projects`, { method: 'GET' }, true);

/**
 * Fetches a user's public skills by their slug.
 */
export const getPortfolioSkills = (slug: string): Promise<Skill[]> =>
  fetchWithAuth<Skill[]>(`/portfolios/${slug}/skills`, { method: 'GET' }, true);

/**
 * Fetches a user's public experiences by their slug.
 */
export const getPortfolioExperience = (slug: string): Promise<Experience[]> =>
  fetchWithAuth<Experience[]>(`/portfolios/${slug}/experience`, { method: 'GET' }, true);

/**
 * Fetches a user's public qualifications by their slug.
 */
export const getPortfolioQualifications = (slug: string): Promise<Qualification[]> =>
  fetchWithAuth<Qualification[]>(`/portfolios/${slug}/qualifications`, { method: 'GET' }, true);

/**
 * Fetches a user's public testimonials by their slug.
 */
export const getPortfolioTestimonials = (slug: string): Promise<Testimonial[]> =>
  fetchWithAuth<Testimonial[]>(`/portfolios/${slug}/testimonials`, { method: 'GET' }, true);

/**
 * Fetches the public settings for a portfolio by its slug.
 */
export const getPortfolioSettings = (slug: string): Promise<AdminSetting[]> => {
  return fetchWithAuth<AdminSetting[]>(`/portfolios/${slug}/settings`, { method: 'GET' }, true);
};

/**
 * Fetches the global, unauthenticated application settings.
 */
export const getGlobalSettings = (): Promise<AdminSetting[]> => {
  return fetchWithAuth<AdminSetting[]>('/settings', { method: 'GET' }, false);
};

/**
 * Sends a contact message to a portfolio owner.
 */
export const sendContactMessage = (slug: string, messageData: Record<string, unknown>): Promise<void> => {
  return fetchWithAuth<void>(`/portfolios/${slug}/contact-messages`, {
    method: 'POST',
    body: messageData
  }, false);
};

/**
 * Fetches the list of available PDF templates.
 */
export const getAvailablePdfTemplates = (): Promise<string[]> => {
  return fetchWithAuth<string[]>('/settings/pdf-templates', { method: 'GET' }, false);
};

/**
 * Fetches a single public project by its UUID.
 */
export const getPublicProjectById = (uuid: string): Promise<Project> => {
  return fetchWithAuth<Project>(`/projects/${uuid}`, { method: 'GET' }, false);
};

/**
 * Downloads a public portfolio as a PDF using its slug.
 */
export const downloadPublicPortfolioBySlug = (slug: string): Promise<Response> => {
  return fetchWithAuth<Response>(`/portfolios/${slug}/pdf`, {
    method: 'GET',
    responseType: 'raw'
  }, false) as unknown as Promise<Response>;
};

/**
 * Downloads a public portfolio as a Markdown file.
 */
export const downloadMarkdownBySlug = (slug: string): Promise<Response> => {
  return fetchWithAuth<Response>(`/portfolios/${slug}/markdown`, {
    method: 'GET',
    responseType: 'raw'
  }, false) as unknown as Promise<Response>;
};

/**
 * Downloads a public portfolio as a vCard file.
 */
export const downloadVCardBySlug = (slug: string): Promise<Response> => {
  return fetchWithAuth<Response>(`/portfolios/${slug}/vcard`, {
    method: 'GET',
    responseType: 'raw'
  }, false) as unknown as Promise<Response>;
};

export type FlattenedProfile = User & Profile & Record<string, unknown>;

/**
 * Fetches the profile data for the homepage.
 */
export const getPublicProfile = async (): Promise<FlattenedProfile | null> => {
  if (authService.isAuthenticated.value && authService.user.value?.slug) {
    const userSlug = authService.user.value.slug;
    try {
      const portfolioData = await getPortfolioBySlug(userSlug);

      if (portfolioData && portfolioData.user && portfolioData.profile) {
        const flatProfile = {
          ...portfolioData,
          ...portfolioData.user,
          ...portfolioData.profile
        } as FlattenedProfile;

        delete flatProfile.user;
        delete flatProfile.profile;

        return flatProfile;
      }
      return null;
    } catch (error) {
      console.error(`[Public Profile] Failed to fetch portfolio for authenticated user slug '${userSlug}'.`, error);
      throw error;
    }
  } else {
    return null;
  }
};

/**
 * Downloads the authenticated user's portfolio as a PDF.
 */
export const downloadPortfolioAsPdf = (templateName: string): Promise<Blob> => {
  return fetchWithAuth<Blob>(`/me/portfolio/download?template=${encodeURIComponent(templateName)}`, {
    method: 'GET',
    responseType: 'blob'
  }, true);
};

/**
 * An object containing all public API functions.
 */
export const publicApi = {
  getPortfolioBySlug,
  getPortfolioProjects,
  getPortfolioSkills,
  getPortfolioExperience,
  getPortfolioQualifications,
  getPortfolioTestimonials,
  getPortfolioSettings,
  getGlobalSettings,
  sendContactMessage,
  getAvailablePdfTemplates,
  getPublicProjectById,
  getPublicProfile,
  downloadPortfolioAsPdf,
  downloadPublicPortfolioBySlug,
  downloadMarkdownBySlug,
  downloadVCardBySlug,
};
