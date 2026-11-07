import { describe, it, expect, vi, beforeEach } from 'vitest';
import { usePublicPortfolioStore } from '@/stores/publicPortfolioStore';
import { publicApi } from '@/services/api/public.api.js';
import { ApiError } from '@/services/api/ApiError.js';

vi.mock('@/services/api/public.api.js', () => ({
  publicApi: {
    getPortfolioBySlug: vi.fn(),
    getPortfolioProjects: vi.fn(),
    getPortfolioSkills: vi.fn(),
    getPortfolioExperience: vi.fn(),
    getPortfolioQualifications: vi.fn(),
    getPortfolioTestimonials: vi.fn(),
  },
}));

// NOTE (Phase 7.3): this composable-store keeps SINGLETON module state, so
// every test must start from a clean state via clearPortfolio().
describe('publicPortfolioStore', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(publicApi.getPortfolioBySlug).mockResolvedValue({ user: { slug: 'alice' }, headline: 'Hi' });
    vi.mocked(publicApi.getPortfolioProjects).mockResolvedValue([{ title: 'P1' }]);
    vi.mocked(publicApi.getPortfolioSkills).mockResolvedValue([{ name: 'Java' }]);
    vi.mocked(publicApi.getPortfolioExperience).mockResolvedValue([{ jobTitle: 'Dev' }]);
    vi.mocked(publicApi.getPortfolioQualifications).mockResolvedValue([{ title: 'BSc' }]);
    vi.mocked(publicApi.getPortfolioTestimonials).mockResolvedValue([{ quote: 'Great' }]);
    usePublicPortfolioStore().clearPortfolio();
  });

  it('fetches and combines all portfolio sections', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('alice');

    expect(store.isLoading.value).toBe(false);
    expect(store.currentSlug.value).toBe('alice');
    expect(store.error.value).toBeNull();
    expect(store.isPrivate.value).toBe(false);
    expect(store.portfolio.value).toMatchObject({
      user: { slug: 'alice' },
      headline: 'Hi',
      projects: [{ title: 'P1' }],
      userSkills: [{ name: 'Java' }],
      experiences: [{ jobTitle: 'Dev' }],
      qualifications: [{ title: 'BSc' }],
      testimonials: [{ quote: 'Great' }],
    });
  });

  it('does not re-fetch when the same slug is already loaded', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('alice');
    expect(publicApi.getPortfolioBySlug).toHaveBeenCalledTimes(1);

    await store.fetchPortfolio('alice');
    expect(publicApi.getPortfolioBySlug).toHaveBeenCalledTimes(1);
    expect(store.currentSlug.value).toBe('alice');
  });

  it('force=true bypasses the cache and refetches', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('alice');

    await store.fetchPortfolio('alice', true);
    expect(publicApi.getPortfolioBySlug).toHaveBeenCalledTimes(2);
  });

  it('rejects an empty slug with an ApiError and never calls the API', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('');

    expect(store.error.value).toBeInstanceOf(ApiError);
    expect(store.error.value.message).toBe('No portfolio slug provided.');
    expect(store.currentSlug.value).toBeNull();
    expect(publicApi.getPortfolioBySlug).not.toHaveBeenCalled();
  });

  it('treats 404 as an unavailable portfolio (private is concealed as 404) and clears stale data', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('alice'); // seed known-good state first
    expect(store.portfolio.value).not.toBeNull();

    // Phase 7.4 (403/404 policy): the backend must return 404 for a private
    // portfolio; the same lock-screen state covers "not found or private".
    vi.mocked(publicApi.getPortfolioBySlug).mockRejectedValue(new ApiError('Not Found', 404));
    await store.fetchPortfolio('bob', true);

    expect(store.isPrivate.value).toBe(true);
    expect(store.error.value.httpStatus).toBe(404);
    // stale data must be cleared on error
    expect(store.portfolio.value).toBeNull();
    expect(store.currentSlug.value).toBeNull();
  });

  it('still treats 403 defensively as unavailable (isPrivate) and clears stale data', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('alice'); // seed known-good state first
    expect(store.portfolio.value).not.toBeNull();

    vi.mocked(publicApi.getPortfolioBySlug).mockRejectedValue(new ApiError('Forbidden', 403));
    await store.fetchPortfolio('bob', true);

    expect(store.isPrivate.value).toBe(true);
    expect(store.error.value.httpStatus).toBe(403);
    // stale data must be cleared on error
    expect(store.portfolio.value).toBeNull();
    expect(store.currentSlug.value).toBeNull();
  });

  it('marks non-403/404 errors as generic (not private) and clears stale data', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('alice');

    vi.mocked(publicApi.getPortfolioBySlug).mockRejectedValue(new ApiError('Down', 503));
    await store.fetchPortfolio('bob', true);

    expect(store.isPrivate.value).toBe(false);
    expect(store.error.value.httpStatus).toBe(503);
    expect(store.portfolio.value).toBeNull();
  });

  it('clearPortfolio resets everything', async () => {
    const store = usePublicPortfolioStore();
    await store.fetchPortfolio('alice');

    store.clearPortfolio();
    expect(store.portfolio.value).toBeNull();
    expect(store.error.value).toBeNull();
    expect(store.currentSlug.value).toBeNull();
    expect(store.isPrivate.value).toBe(false);
    expect(store.isLoading.value).toBe(false);
  });

  it('recovers from a 403 (private) to a fresh public fetch — the router-guard path after a master toggle', async () => {
    const store = usePublicPortfolioStore();

    vi.mocked(publicApi.getPortfolioBySlug).mockRejectedValue(new ApiError('Forbidden', 403));
    await store.fetchPortfolio('alice');
    expect(store.isPrivate.value).toBe(true);
    expect(store.currentSlug.value).toBeNull(); // dedupe must NOT stick after failure

    // Settings master toggle fix clears the store; the next guard navigation
    // re-fetches the same slug WITHOUT force and must fully recover.
    vi.mocked(publicApi.getPortfolioBySlug).mockResolvedValue({ user: { slug: 'alice' }, headline: 'Hi' });
    await store.fetchPortfolio('alice');

    expect(store.isPrivate.value).toBe(false);
    expect(store.error.value).toBeNull();
    expect(store.currentSlug.value).toBe('alice');
    expect(store.portfolio.value).toMatchObject({ headline: 'Hi' });
  });
});
