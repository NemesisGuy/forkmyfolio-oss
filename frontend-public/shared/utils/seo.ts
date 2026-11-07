/**
 * @file shared/utils/seo.ts
 * @description Pure, framework-free SEO building blocks for ForkMyFolio.
 *
 * Shared between:
 *   - the Nuxt app (composable `usePortfolioSeo`, page-level metadata), and
 *   - the Nitro server routes (`/robots.txt`, `/sitemap.xml`).
 *
 * Everything here is a pure function of its inputs so it is trivially
 * unit-testable and carries zero Nuxt/Vue imports. Nuxt-side concerns
 * (reactivity, request headers, head injection) live in
 * `app/composables/usePortfolioSeo.ts` and `server/routes/*`.
 *
 * Multi-user principle: SEO metadata is part of each user's PUBLISHED
 * portfolio. Every builder derives its output exclusively from the public
 * payload already fetched for the page — never from private fields — and
 * every builder has a sensible fallback so no empty/garbage meta is emitted.
 */

/** Brand constants — single source of truth for all metadata. */
export const SITE_NAME = 'ForkMyFolio';
/** Local fallback social image (served by Nuxt from public/). */
export const FALLBACK_IMAGE_PATH = '/forkmyfolio_logo_icon.png';

/** Target caps (search engines truncate ~60 chars titles / ~160 chars descriptions). */
export const TITLE_MAX = 70;
export const DESCRIPTION_MAX = 160;

/** Public section labels, keyed by route section. */
export type PortfolioSection =
  | 'home'
  | 'projects'
  | 'experience'
  | 'qualifications'
  | 'skills'
  | 'testimonials'
  | 'contact'
  | 'project-details';

const SECTION_LABELS: Record<Exclude<PortfolioSection, 'home' | 'project-details'>, string> = {
  projects: 'Projects',
  experience: 'Experience',
  qualifications: 'Qualifications',
  skills: 'Skills',
  testimonials: 'Testimonials',
  contact: 'Contact',
};

// ---------------------------------------------------------------------------
// URL / origin helpers
// ---------------------------------------------------------------------------

export interface ForwardedHeaders {
  host?: string;
  'x-forwarded-proto'?: string;
}

/**
 * Derives the public origin from SSR request headers. Behind the nginx proxy
 * (and Cloudflare in prod) `x-forwarded-proto` carries the real scheme; the
 * Host header carries the real domain. Falls back to null when headers are
 * missing so callers can substitute a safe default.
 */
export function originFromHeaders(headers: ForwardedHeaders): string | null {
  const host = headers?.host?.trim();
  if (!host) return null;
  const proto = (headers['x-forwarded-proto'] || 'http').split(',')[0].trim() || 'http';
  return `${proto}://${host}`;
}

/** Strips trailing slashes (root "/" is preserved as "/"). */
export function normalizePath(path: string): string {
  if (!path) return '/';
  const p = path.endsWith('/') && path !== '/' ? path.replace(/\/+$/, '') : path;
  return p || '/';
}

/**
 * Returns an absolute URL for a possibly-relative path, or null for empty
 * input. Already-absolute URLs (http/https/protocol-relative data) pass
 * through unchanged — profile/project images are user-supplied absolute URLs
 * by contract (verified against the live API).
 */
export function absoluteUrl(url: string | null | undefined, origin: string): string | null {
  if (!url || !url.trim()) return null;
  const u = url.trim();
  if (/^https?:\/\//i.test(u)) return u;
  if (u.startsWith('//')) return `https:${u}`;
  if (u.startsWith('/')) return `${origin}${u}`;
  return `${origin}/${u}`;
}

// ---------------------------------------------------------------------------
// Text building blocks
// ---------------------------------------------------------------------------

/** Collapses whitespace (incl. newlines) and hard-truncates with an ellipsis. */
export function truncate(text: string | null | undefined, max: number): string {
  if (!text) return '';
  const flat = text.replace(/\s+/g, ' ').trim();
  if (flat.length <= max) return flat;
  return `${flat.slice(0, Math.max(0, max - 1)).trimEnd()}…`;
}

/** "nemesis" -> "Nemesis" (fallback display name when names are missing). */
export function titleCaseSlug(slug: string): string {
  return (slug || '')
    .split(/[-_\s]+/)
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

/** Display name from the public profile, falling back to the slug. */
export function buildDisplayName(profile: any, slug: string): string {
  const first = profile?.firstName?.trim() || '';
  const last = profile?.lastName?.trim() || '';
  const name = `${first} ${last}`.trim();
  return name || titleCaseSlug(slug) || 'Portfolio';
}

/**
 * Landing-page title: "Jane Doe — Headline | ForkMyFolio".
 * Length-conscious: the headline is truncated or dropped before the name
 * ever is, and the brand suffix is always present exactly once.
 */
export function buildLandingTitle(name: string, headline: string | null | undefined): string {
  const suffix = ` | ${SITE_NAME}`;
  const base = name || 'Portfolio';
  const budget = TITLE_MAX - suffix.length - base.length;
  if (budget < 0) return `${truncate(base, TITLE_MAX - suffix.length)}${suffix}`;
  const head = (headline || '').trim();
  if (!head) return `${base}${suffix}`;
  if (budget >= head.length + 3) return `${base} — ${head}${suffix}`;
  const truncated = truncate(head, budget - 3);
  if (truncated && truncated.length > 8) return `${base} — ${truncated}${suffix}`;
  return `${base}${suffix}`;
}

/** Section title: "Jane Doe — Projects | ForkMyFolio". */
export function buildSectionTitle(name: string, section: PortfolioSection): string {
  const label = SECTION_LABELS[section as Exclude<PortfolioSection, 'home' | 'project-details'>] || 'Portfolio';
  const suffix = ` | ${SITE_NAME}`;
  const base = `${name || 'Portfolio'} — ${label}`;
  return `${truncate(base, TITLE_MAX - suffix.length)}${suffix}`;
}

/** Item (project details) title: "My App — Jane Doe | ForkMyFolio". */
export function buildItemTitle(itemTitle: string, name: string): string {
  const suffix = ` | ${SITE_NAME}`;
  const who = name || 'Portfolio';
  const t = (itemTitle || '').trim();
  if (!t) return `${truncate(who, TITLE_MAX - suffix.length)}${suffix}`;
  const base = `${t} — ${who}`;
  return `${truncate(base, TITLE_MAX - suffix.length)}${suffix}`;
}

/**
 * Landing description: the published biography when available, otherwise a
 * headline-derived or generic sentence. Never empty.
 */
export function buildLandingDescription(profile: any, name: string, slug: string): string {
  const summary = (profile?.summary || '').trim();
  if (summary) return truncate(summary, DESCRIPTION_MAX);
  const headline = (profile?.headline || '').trim();
  if (headline) return truncate(`${name} — ${headline}. View the full portfolio on ${SITE_NAME}.`, DESCRIPTION_MAX);
  return `View the professional portfolio of ${name || titleCaseSlug(slug)} on ${SITE_NAME}.`;
}

/**
 * Section description: a section-specific lead (optionally enriched by the
 * page's own data via `dataLead`) plus a trimmed biography snippet when one
 * exists — so section pages are never identical to the landing page and
 * never empty.
 */
export function buildSectionDescription(
  section: PortfolioSection,
  profile: any,
  name: string,
  dataLead?: string | null,
): string {
  const lead = dataLead?.trim();
  if (lead) return truncate(lead, DESCRIPTION_MAX);
  const summary = (profile?.summary || '').trim();
  const snippet = summary ? ` ${truncate(summary, 110)}` : '';
  const n = name || 'this member';
  switch (section) {
    case 'projects':
      return truncate(`Projects and technical work by ${n}.${snippet}`, DESCRIPTION_MAX);
    case 'experience':
      return truncate(`Professional experience and career history of ${n}.${snippet}`, DESCRIPTION_MAX);
    case 'qualifications':
      return truncate(`Education, qualifications and certifications of ${n}.${snippet}`, DESCRIPTION_MAX);
    case 'skills':
      return truncate(`Technical skills and expertise of ${n}.${snippet}`, DESCRIPTION_MAX);
    case 'testimonials':
      return truncate(`What colleagues and clients say about working with ${n}.${snippet}`, DESCRIPTION_MAX);
    case 'contact':
      return truncate(`Get in touch with ${n} through their portfolio on ${SITE_NAME}.`, DESCRIPTION_MAX);
    default:
      return truncate(`View the portfolio of ${n} on ${SITE_NAME}.${snippet}`, DESCRIPTION_MAX);
  }
}

// ---------------------------------------------------------------------------
// Social image resolution
// ---------------------------------------------------------------------------

export interface SocialImage {
  url: string;
  alt: string;
  /** true when the image is a real photo/screenshot (large card suits it). */
  isRich: boolean;
}

/**
 * Resolves the social sharing image. The public model (verified against the
 * live API) exposes user-supplied ABSOLUTE image URLs only — there is no
 * cover-image or dedicated social-image field, so the priority is:
 *   1. the provided image (caller passes profileImageUrl, project imageUrl,
 *      or a media thumbnail it already computed),
 *   2. the application-wide fallback logo (absolutized).
 * `isRich=false` (the tiny local logo) makes callers pick a `summary`
 * Twitter card instead of `summary_large_image`.
 */
export function resolveSocialImage(
  imageUrl: string | null | undefined,
  origin: string,
  alt: string,
): SocialImage {
  const abs = absoluteUrl(imageUrl, origin);
  if (abs) return { url: abs, alt: truncate(alt, 120) || alt, isRich: true };
  return {
    url: absoluteUrl(FALLBACK_IMAGE_PATH, origin) || FALLBACK_IMAGE_PATH,
    alt: `${SITE_NAME} logo`,
    isRich: false,
  };
}

// ---------------------------------------------------------------------------
// JSON-LD structured data
// ---------------------------------------------------------------------------

/**
 * Safely serializes a JSON-LD graph. `JSON.stringify` does NOT escape `<`,
 * so a user-supplied string like `</script><script>…` could break out of the
 * script tag — every `<` is escaped to `\u003c` (standard practice, also
 * used by Next.js/Nuxt ecosystem tooling).
 */
export function toJsonLd(data: unknown): string {
  return JSON.stringify(data).replace(/</g, '\\u003c');
}

/** Strips control characters that would make JSON-LD/HTML invalid. */
function clean(value: string | null | undefined): string | undefined {
  if (!value) return undefined;
  // eslint-disable-next-line no-control-regex
  const v = value.replace(/[\u0000-\u001f\u007f]/g, ' ').replace(/\s+/g, ' ').trim();
  return v || undefined;
}

/**
 * Person schema — ONLY fields actually present in the public payload.
 * No email (publicEmail is deliberately excluded to avoid address
 * harvesting), no invented credentials, no private data.
 */
export function buildPersonJsonLd(opts: {
  name: string;
  slug: string;
  origin: string;
  profile: any;
  user?: any;
}): Record<string, unknown> {
  const { name, slug, origin, profile, user } = opts;
  const url = `${origin}${normalizePath(`/${slug}`)}`;
  const person: Record<string, unknown> = {
    '@type': 'Person',
    name,
    url,
  };
  const image = absoluteUrl(profile?.profileImageUrl, origin);
  if (image) person.image = image;
  const jobTitle = clean(profile?.headline);
  if (jobTitle) person.jobTitle = jobTitle;
  const description = clean(profile?.summary);
  if (description) person.description = truncate(description, 300);
  const sameAs = [profile?.linkedinUrl, profile?.githubUrl, profile?.websiteUrl]
    .map((u) => (typeof u === 'string' ? u.trim() : ''))
    .filter((u) => /^https?:\/\//i.test(u));
  if (sameAs.length) person.sameAs = sameAs;
  const modified = clean(user?.updatedAt);
  if (modified) person.dateModified = modified;
  return { '@context': 'https://schema.org', '@type': 'Person', ...person };
}

/** ProfilePage + mainEntity Person for the portfolio landing page. */
export function buildProfilePageJsonLd(opts: {
  name: string;
  slug: string;
  origin: string;
  profile: any;
  user?: any;
}): string {
  const person = buildPersonJsonLd(opts);
  const graph = {
    '@context': 'https://schema.org',
    '@type': 'ProfilePage',
    mainEntity: person,
  };
  return toJsonLd(graph);
}

/** WebSite schema for the marketing landing page at "/". */
export function buildWebSiteJsonLd(origin: string): string {
  return toJsonLd({
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: SITE_NAME,
    url: origin,
  });
}

/**
 * BreadcrumbList for section pages. `items` are label/URL pairs ordered
 * root-first; only genuinely public URLs are passed in by callers.
 */
export function buildBreadcrumbJsonLd(items: Array<{ label: string; url: string }>): string {
  return toJsonLd({
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      name: item.label,
      item: item.url,
    })),
  });
}

// ---------------------------------------------------------------------------
// robots.txt / sitemap.xml
// ---------------------------------------------------------------------------

/** Auth/admin/manage paths owned by the Vue SPA — never indexable. */
export const NON_INDEXABLE_PATHS = [
  '/login',
  '/register',
  '/force-password-change',
  '/accept-terms',
  '/guest-access',
  '/admin',
  '/*/manage/',
];

/**
 * robots.txt. When `indexable` is false (dev/staging or explicit opt-out)
 * the entire host is disallowed — deny-by-default so a forgotten env var can
 * never leak a staging instance into search indexes.
 */
export function buildRobotsTxt(indexable: boolean, origin: string | null): string {
  if (!indexable) {
    return ['User-agent: *', 'Disallow: /', ''].join('\n');
  }
  const lines = ['User-agent: *', 'Allow: /'];
  for (const p of NON_INDEXABLE_PATHS) lines.push(`Disallow: ${p}`);
  if (origin) lines.push('', `Sitemap: ${origin}/sitemap.xml`);
  lines.push('');
  return lines.join('\n');
}

export interface SitemapEntry {
  slug: string;
  updatedAt?: string | null;
}

/** W3C datetime (YYYY-MM-DD) for sitemap <lastmod>. */
function sitemapDate(value: string | null | undefined): string | null {
  if (!value) return null;
  const m = /^(\d{4}-\d{2}-\d{2})/.exec(value);
  return m ? m[1] : null;
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

/**
 * sitemap.xml containing ONLY genuinely public, indexable URLs: the
 * marketing landing page and each public portfolio's landing page. Section
 * pages are crawlable via links and intentionally omitted (thin content,
 * keeps the sitemap clean). Entries are XML-escaped and deduplicated.
 */
export function buildSitemapXml(entries: SitemapEntry[], origin: string): string {
  const seen = new Set<string>();
  const urls: string[] = [];

  const push = (loc: string, lastmod: string | null) => {
    if (seen.has(loc)) return;
    seen.add(loc);
    const lm = lastmod ? `<lastmod>${lastmod}</lastmod>` : '';
    urls.push(`<url><loc>${escapeXml(loc)}</loc>${lm}</url>`);
  };

  push(`${origin}/`, null);
  for (const e of entries || []) {
    const slug = (e?.slug || '').trim();
    if (!slug || /[^a-zA-Z0-9-_]/.test(slug)) continue; // defensive: slugs are URL-safe by contract
    push(`${origin}/${encodeURIComponent(slug)}`, sitemapDate(e.updatedAt));
  }

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...urls.map((u) => `  ${u}`),
    '</urlset>',
    '',
  ].join('\n');
}

/**
 * Phase 10 (Workstream 2.3): resolves the per-page robots directive for a
 * public portfolio page.
 *
 * Precedence (most to least restrictive):
 *   1. Global switch off  -> null (the seo-indexable plugin emits the
 *      site-wide noindex; the page must not double-emit a contradicting
 *      directive).
 *   2. Page not found     -> 'noindex, nofollow'.
 *   3. Per-user opt-out   -> 'noindex, nofollow'. STRICT check: anything
 *      other than an explicit true (missing field on an old backend payload,
 *      false, undefined) is treated as opted-out — conservative by default,
 *      matching the DB column default (FALSE).
 *   4. Otherwise          -> 'index, follow'.
 */
export function resolveRobotsDirective(
  globalIndexable: boolean,
  isNotFound: boolean,
  isSearchIndexable: unknown
): string | null {
  if (!globalIndexable) return null;
  if (isNotFound) return 'noindex, nofollow';
  if (isSearchIndexable !== true) return 'noindex, nofollow';
  return 'index, follow';
}
