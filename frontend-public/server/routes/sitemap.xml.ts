import { buildSitemapXml, type SitemapEntry } from '#shared/utils/seo';

/**
 * @file server/routes/sitemap.xml.ts
 * @description Dynamic sitemap for publicly accessible portfolios.
 *
 * Source: GET {backend}/portfolios (v3.1.2 guest endpoint) — returns ONLY the
 * slugs + updatedAt of ACTIVE users with publiclyVisible profiles, so private
 * content can never leak into the sitemap by construction.
 *
 * Served only when the deployment is indexable (NUXT_PUBLIC_INDEXABLE=true);
 * otherwise responds 404 — a non-indexable deployment must not publish a
 * sitemap inviting crawlers in.
 *
 * Caching: a small in-process cache (10 min) shields the backend from crawler
 * bursts. The payload is global (not user-specific), so caching cannot leak
 * one user's data into another user's response.
 */

const CACHE_TTL_MS = 10 * 60 * 1000;
let cache: { at: number; entries: SitemapEntry[] } | null = null;

async function fetchPublicSummaries(): Promise<SitemapEntry[]> {
  if (cache && Date.now() - cache.at < CACHE_TTL_MS) return cache.entries;
  const config = useRuntimeConfig();
  const base = config.apiBaseUrl; // server-side base (http://app:8080/api/v1)
  try {
    const res = await fetch(`${base}/portfolios`, { headers: { Accept: 'application/json' } });
    if (!res.ok) throw new Error(`API error ${res.status}`);
    const json = await res.json();
    const list = Array.isArray(json?.data) ? json.data : [];
    const entries = list
      .map((e: any) => ({ slug: String(e?.slug || ''), updatedAt: e?.updatedAt || null }))
      .filter((e: SitemapEntry) => !!e.slug);
    cache = { at: Date.now(), entries };
    return entries;
  } catch {
    // Graceful degradation: serve at least the marketing landing page rather
    // than failing the whole sitemap. Do NOT cache failures.
    return cache?.entries ?? [];
  }
}

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event);
  const indexable = String(config.public.indexable ?? 'false').toLowerCase() === 'true';
  if (!indexable) {
    throw createError({ statusCode: 404, statusMessage: 'Sitemap disabled (non-indexable deployment)' });
  }

  const host = getRequestHost(event, { xForwardedHost: true });
  const proto = getRequestProtocol(event, { xForwardedProto: true });
  const origin = `${proto}://${host}`;

  const entries = await fetchPublicSummaries();

  setHeader(event, 'Content-Type', 'application/xml; charset=utf-8');
  setHeader(event, 'Cache-Control', 'public, max-age=3600');
  return buildSitemapXml(entries, origin);
});
