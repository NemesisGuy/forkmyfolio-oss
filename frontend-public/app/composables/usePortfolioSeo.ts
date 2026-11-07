import {
  SITE_NAME,
  buildDisplayName,
  buildItemTitle,
  buildLandingDescription,
  buildLandingTitle,
  buildProfilePageJsonLd,
  buildBreadcrumbJsonLd,
  buildSectionDescription,
  buildSectionTitle,
  originFromHeaders,
  resolveRobotsDirective,
  resolveSocialImage,
  normalizePath,
  truncate,
  type PortfolioSection,
} from '#shared/utils/seo';

/**
 * @file app/composables/usePortfolioSeo.ts
 * @description Single wiring point for all public-page SEO metadata.
 *
 * Derives EVERYTHING from the public portfolio payload the page already
 * fetched (zero extra backend requests) plus the current route, and emits:
 *   - title / description (length-capped, per-section, never empty)
 *   - canonical URL (origin derived from SSR request headers — correct on
 *     localhost, staging and prod without configuration)
 *   - robots directives (noindex for private/missing portfolios; global
 *     noindex is handled by app/plugins/seo-indexable.ts when the deployment
 *     is marked non-indexable)
 *   - Open Graph + Twitter/X card metadata
 *   - JSON-LD (ProfilePage/Person on the landing page, BreadcrumbList on
 *     sections — only from published, visible data)
 *
 * IMPORTANT (TDZ): like the raw `useSeoMeta` calls this composable replaces,
 * it must be called BELOW the reactive declarations it reads (the
 * `useAsyncData` result refs) — unhead resolves the getters eagerly during
 * setup.
 */

export interface PortfolioSeoItem {
  title?: string | null;
  description?: string | null;
  /** Direct image URL (e.g. project imageUrl). */
  image?: string | null;
  /** Rich media URL — YouTube thumbnails are used as OG image when present. */
  mediaUrl?: string | null;
}

export interface PortfolioSeoInput {
  /** Public slug from the route. */
  slug: string;
  section: PortfolioSection;
  /** Getter for the public portfolio payload (may be null while loading/failed). */
  portfolio: () => any | null;
  /** Getter for the featured item (project details page). */
  item?: () => PortfolioSeoItem | null;
  /**
   * Optional section-specific description lead computed from the page's own
   * list data (e.g. "5 projects including Portfolio Platform") — keeps
   * section descriptions from duplicating the landing description. Receives
   * the resolved display name.
   */
  dataLead?: (name: string) => string | null;
  /** HTTP status implied by the page's load error, if any (404/403/500…). */
  httpErrorStatus?: () => number | null;
}

/** Extracts the HTTP status from the useApi error message contracts. */
export function apiErrorStatus(message: unknown): number | null {
  const s = String(message || '');
  const m = /API error (\d{3})/.exec(s) || /Status: (\d{3})/.exec(s);
  return m ? Number(m[1]) : null;
}

export function usePortfolioSeo(input: PortfolioSeoInput) {
  const config = useRuntimeConfig();
  const route = useRoute();

  // Deny-by-default: only an explicit "true" makes the deployment indexable.
  const indexable = String(config.public.indexable ?? 'false').toLowerCase() === 'true';

  // Origin: SSR request headers first (proxy/Cloudflare aware), client origin
  // as the CSR fallback. Only relevant server-side for crawlers.
  const headers = import.meta.server ? useRequestHeaders(['host', 'x-forwarded-proto']) : {};
  const origin = computed(() => {
    const fromHeaders = originFromHeaders(headers as any);
    if (fromHeaders) return fromHeaders;
    if (!import.meta.server && typeof window !== 'undefined') return window.location.origin;
    return 'http://localhost:3000';
  });

  const canonical = computed(() => `${origin.value}${normalizePath(route.path)}`);

  const pf = computed(() => input.portfolio() || null);
  const profile = computed(() => pf.value?.profile || null);
  const name = computed(() => buildDisplayName(profile.value, input.slug));
  const notFoundStatus = computed(() => input.httpErrorStatus?.() ?? null);
  const isNotFound = computed(() => !!notFoundStatus.value && notFoundStatus.value >= 400);

  // --- Title / description ---
  const title = computed(() => {
    if (isNotFound.value) return `Portfolio not found | ${SITE_NAME}`;
    const item = input.item?.() || null;
    if (input.section === 'home') {
      return buildLandingTitle(name.value, profile.value?.headline);
    }
    if (input.section === 'project-details') {
      return buildItemTitle(item?.title || '', name.value);
    }
    return buildSectionTitle(name.value, input.section);
  });

  const description = computed(() => {
    if (isNotFound.value) {
      return `This portfolio is not available. Discover other portfolios on ${SITE_NAME}.`;
    }
    const item = input.item?.() || null;
    if (input.section === 'home') {
      return buildLandingDescription(profile.value, name.value, input.slug);
    }
    if (input.section === 'project-details') {
      const d = (item?.description || '').trim();
      if (d) return truncate(d, 160);
      const headline = (profile.value?.headline || '').trim();
      if (item?.title && headline) {
        return truncate(`${item.title} — a project by ${name.value}, ${headline}.`, 160);
      }
      return truncate(`View the project ${item?.title || ''} by ${name.value} on ${SITE_NAME}.`.trim(), 160);
    }
    return buildSectionDescription(input.section, profile.value, name.value, input.dataLead?.(name.value) || null);
  });

  // --- Social image ---
  const socialImage = computed(() => {
    const item = input.item?.() || null;
    // YouTube thumbnails (and direct image media) are public, stable and make
    // good cards — reuses the app's existing mediaUtils logic.
    const ytThumb = item?.mediaUrl ? getThumbnailUrl(item.mediaUrl) : null;
    const candidate = ytThumb || item?.image || profile.value?.profileImageUrl || null;
    const altBase = item?.title
      ? `${item.title} — ${name.value}`
      : `${name.value} — profile photo`;
    return resolveSocialImage(candidate, origin.value, altBase);
  });

  // --- Robots ---
  // P10 (Workstream 2.3): decision extracted to the shared, unit-tested
  // resolveRobotsDirective helper (precedence: global switch > not-found >
  // per-user opt-out with a strict !== true check > index).
  const robots = computed(() =>
    resolveRobotsDirective(indexable, isNotFound.value, profile.value?.isSearchIndexable)
  );

  // --- HTTP status for crawlers (private/missing portfolios must 404) ---
  if (import.meta.server && notFoundStatus.value) {
    setResponseStatus(notFoundStatus.value);
  }

  // --- Head wiring ---
  useSeoMeta({
    title: () => title.value,
    description: () => description.value,
    ogTitle: () => title.value,
    ogDescription: () => description.value,
    ogType: () => (input.section === 'home' ? 'profile' : 'website'),
    ogUrl: () => canonical.value,
    ogSiteName: () => SITE_NAME,
    ogImage: () => socialImage.value.url,
    ogImageAlt: () => socialImage.value.alt,
    twitterCard: () => (socialImage.value.isRich ? 'summary_large_image' : 'summary'),
    twitterTitle: () => title.value,
    twitterDescription: () => description.value,
    twitterImage: () => socialImage.value.url,
    ...(robots.value ? { robots: robots.value } : {}),
  });

  // --- Canonical + JSON-LD (structured data only for successfully loaded,
  //     genuinely public pages) ---
  const jsonLd = computed<string | null>(() => {
    if (isNotFound.value || !pf.value) return null;
    if (input.section === 'home') {
      return buildProfilePageJsonLd({
        name: name.value,
        slug: input.slug,
        origin: origin.value,
        profile: profile.value,
        user: pf.value?.user,
      });
    }
    const item = input.item?.() || null;
    const crumbs: Array<{ label: string; url: string }> = [
      { label: name.value, url: `${origin.value}${normalizePath(`/${input.slug}`)}` },
    ];
    if (input.section === 'project-details') {
      crumbs.push({ label: 'Projects', url: `${origin.value}/${input.slug}/projects` });
      crumbs.push({ label: item?.title || 'Project', url: canonical.value });
    } else {
      const label = title.value.split(' — ')[1]?.split(` | ${SITE_NAME}`)[0] || 'Section';
      crumbs.push({ label, url: canonical.value });
    }
    return buildBreadcrumbJsonLd(crumbs);
  });

  useHead(() => {
    const head: Record<string, any> = {
      link: [{ rel: 'canonical', href: canonical.value }],
    };
    if (jsonLd.value) {
      head.script = [{ type: 'application/ld+json', innerHTML: jsonLd.value }];
    }
    return head;
  });
}
