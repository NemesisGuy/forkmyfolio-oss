-- V13: Per-user search-engine indexing preference (Phase 10, Workstream 2).
--
-- Adds a user-controlled crawlability opt-in to the portfolio profile.
-- Conservative by design: the NOT NULL DEFAULT FALSE means every EXISTING
-- user opts OUT of search-engine indexing until they explicitly enable it,
-- and no previously-unpublished portfolio can become discoverable through
-- this migration alone. This preference is independent of is_public: a
-- portfolio can be publicly accessible via its direct URL while opting out
-- of search indexing.
--
-- Enforcement points (all server-side):
--   * GET /api/v1/portfolios (sitemap source) filters on this column.
--   * The Nuxt SSR layer emits per-user noindex directives from the public
--     payload when the global NUXT_PUBLIC_INDEXABLE switch is enabled.
-- The global switch always wins: when it is false, nothing is indexable
-- regardless of this preference.

ALTER TABLE portfolio_profiles
    ADD COLUMN search_indexable BOOLEAN NOT NULL DEFAULT FALSE;
