/**
 * @file src/stores/publicPortfolioStore.js
 * @description A simple, composable-based store for managing the state of a publicly viewed portfolio.
 * This follows a singleton pattern where the state is defined once and shared across all
 * components that use the `usePublicPortfolioStore` composable.
 */
import {ref} from 'vue';
import {publicApi} from '@/services/api/public.api.js';
import {ApiError} from '@/services/api/ApiError.js';

/**
 * The reactive portfolio object, containing all combined data for the current user.
 * @type {import('vue').Ref<object|null>}
 */
const portfolio = ref(null);
/**
 * A reactive flag indicating if a fetch operation is in progress.
 * @type {import('vue').Ref<boolean>}
 */
const isLoading = ref(false);
/**
 * A reactive container for any API errors that occur during fetching.
 * @type {import('vue').Ref<ApiError|null>}
 */
const error = ref(null);
/**
 * The slug of the portfolio currently loaded in the store.
 * @type {import('vue').Ref<string|null>}
 */
const currentSlug = ref(null);
/**
 * Tracks the current active fetch promise to allow concurrent callers to await the same request.
 * @type {Promise<void>|null}
 */
let activeFetchPromise = null;

/**
 * A reactive flag to specifically track if a portfolio is inaccessible because it's private.
 * This allows the UI to show a specific "Private" message instead of a generic error.
 * @type {import('vue').Ref<boolean>}
 */
const isPrivate = ref(false);

/**
 * A composable that acts as a simple store for the publicly viewed portfolio.
 * It holds the portfolio data and prevents redundant API calls.
 * @returns {{portfolio: import('vue').Ref<object|null>, isLoading: import('vue').Ref<boolean>, error: import('vue').Ref<ApiError|null>, isPrivate: import('vue').Ref<boolean>, currentSlug: import('vue').Ref<string|null>, fetchPortfolio: function, clearPortfolio: function}}
 */
export function usePublicPortfolioStore() {

  /**
   * Fetches the public portfolio for a given slug.
   * It's smart enough to not re-fetch if the data for the current slug is already loaded.
   * It now also handles the 403 Forbidden error for private portfolios.
   * @param {string} slug The user's public portfolio slug.
   * @param {boolean} force - If true, bypasses the cache and re-fetches data.
   * @returns {Promise<void>}
   */
  const fetchPortfolio = async (slug, force = false) => {
    if (!slug) {
      error.value = new ApiError('No portfolio slug provided.', 400);
      portfolio.value = null;
      currentSlug.value = null;
      return;
    }

    // If we are already loading, returning the existing promise instead of starting a new one.
    if (activeFetchPromise && !force) {
      console.log(`[PublicPortfolioStore] Reusing active fetch promise for slug: ${slug}`);
      return activeFetchPromise;
    }

    // If not forcing a refresh, and we already have the data for this slug, do nothing.
    if (!force && currentSlug.value === slug && portfolio.value) {
      return;
    }

    // Reset state for the new request
    isLoading.value = true;
    error.value = null;
    isPrivate.value = false; // Reset privacy flag on each new fetch

    activeFetchPromise = (async () => {
      console.log(`[PublicPortfolioStore] Starting new fetch for slug: ${slug}`);
      try {
        // Use Promise.all to fetch all parts of the portfolio in parallel for performance.
        const [
          profileData,
          projects,
          skills,
          experiences,
          qualifications,
          testimonials,
        ] = await Promise.all([
          publicApi.getPortfolioBySlug(slug), // Gets the base user/profile info
          publicApi.getPortfolioProjects(slug),
          publicApi.getPortfolioSkills(slug),
          publicApi.getPortfolioExperience(slug),
          publicApi.getPortfolioQualifications(slug),
          publicApi.getPortfolioTestimonials(slug),
        ]);

        // Combine all the fetched data into a single portfolio object in the store.
        portfolio.value = {
          ...profileData, // Contains the base user and profile details
          projects,
          userSkills: skills, // The backend returns skills, but components might expect userSkills
          experiences,
          qualifications,
          testimonials,
        };

        currentSlug.value = slug; // Set the new slug after a successful fetch
      } catch (e) {
        const status = e.httpStatus || e.status || e.response?.status;
        console.warn(`[PublicPortfolioStore] Fetch failed with status ${status} for slug: ${slug}`);

        // Phase 7.4 (403/404 policy): the backend now conceals private
        // portfolios as 404 (indistinguishable from a nonexistent slug), so
        // the "unavailable" lock-screen variant triggers on BOTH 404 and 403.
        // 403 is kept as a defensive fallback for future role-based denials
        // on this surface.
        if (status === 404 || status === 403) {
          isPrivate.value = true;
          error.value = e; // Still set error for the router guard to catch
        } else {
          console.error(`Failed to fetch portfolio for slug: ${slug}`, e);
          error.value = e;
        }
        // Clear stale data on any error
        portfolio.value = null;
        currentSlug.value = null;
      } finally {
        isLoading.value = false;
        activeFetchPromise = null;
      }
    })();

    return activeFetchPromise;
  };

  /**
   * Clears the store's state, useful when logging out or navigating away.
   * @returns {void}
   */
  const clearPortfolio = () => {
    portfolio.value = null;
    error.value = null;
    isLoading.value = false;
    currentSlug.value = null;
    isPrivate.value = false;
  };

  // Expose the state and actions for any component to use.
  return {
    portfolio,
    isLoading,
    error,
    isPrivate, // Expose the new state
    currentSlug,
    fetchPortfolio,
    clearPortfolio,
  };
}
