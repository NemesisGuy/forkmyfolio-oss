import { describe, it, expect } from 'vitest';
import {
  SITE_NAME,
  originFromHeaders,
  normalizePath,
  absoluteUrl,
  truncate,
  titleCaseSlug,
  buildDisplayName,
  buildLandingTitle,
  buildSectionTitle,
  buildItemTitle,
  buildLandingDescription,
  buildSectionDescription,
  resolveSocialImage,
  toJsonLd,
  buildProfilePageJsonLd,
  buildWebSiteJsonLd,
  buildBreadcrumbJsonLd,
  buildRobotsTxt,
  buildSitemapXml,
  resolveRobotsDirective,
} from '#shared/utils/seo';

/**
 * SEO v3.1.2 — unit tests for the pure metadata builders in
 * shared/utils/seo.ts. These functions are the single source of truth for
 * every public page's metadata, so the spec's Phase 8 checklist maps onto
 * them directly (two-user differentiation, canonical shape, OG fallbacks,
 * never-empty meta, JSON-LD safety, public-only sitemap, robots content).
 */

describe('originFromHeaders', () => {
  it('derives origin from Host + X-Forwarded-Proto (proxy-aware)', () => {
    expect(originFromHeaders({ host: 'example.com', 'x-forwarded-proto': 'https' }))
      .toBe('https://example.com');
  });

  it('uses the first entry of a comma-separated proto list', () => {
    expect(originFromHeaders({ host: 'example.com', 'x-forwarded-proto': 'https, http' }))
      .toBe('https://example.com');
  });

  it('falls back to http when no proto header exists', () => {
    expect(originFromHeaders({ host: 'localhost:8189' })).toBe('http://localhost:8189');
  });

  it('returns null without a host (caller substitutes a safe default)', () => {
    expect(originFromHeaders({})).toBeNull();
  });
});

describe('normalizePath / absoluteUrl', () => {
  it('strips trailing slashes but preserves root', () => {
    expect(normalizePath('/nemesis/')).toBe('/nemesis');
    expect(normalizePath('/')).toBe('/');
    expect(normalizePath('')).toBe('/');
  });

  it('passes user-supplied absolute image URLs through unchanged', () => {
    expect(absoluteUrl('https://cdn.example.com/x.webp', 'https://site.com'))
      .toBe('https://cdn.example.com/x.webp');
  });

  it('absolutizes relative paths against the origin', () => {
    expect(absoluteUrl('/forkmyfolio_logo_icon.png', 'https://site.com'))
      .toBe('https://site.com/forkmyfolio_logo_icon.png');
  });

  it('returns null for empty input (no empty meta emitted)', () => {
    expect(absoluteUrl(null, 'https://site.com')).toBeNull();
    expect(absoluteUrl('   ', 'https://site.com')).toBeNull();
  });
});

describe('title/description builders (two-user differentiation)', () => {
  it('two different users produce different titles and descriptions', () => {
    const john = buildLandingTitle('John Smith', 'Software Engineer');
    const jane = buildLandingTitle('Jane Doe', 'Data Scientist');
    expect(john).toBe('John Smith — Software Engineer | ForkMyFolio');
    expect(jane).toBe('Jane Doe — Data Scientist | ForkMyFolio');
    expect(john).not.toBe(jane);

    const dJohn = buildLandingDescription({ summary: 'Backend specialist.' }, 'John Smith', 'john-smith');
    const dJane = buildLandingDescription({ summary: 'ML researcher.' }, 'Jane Doe', 'jane-doe');
    expect(dJohn).not.toBe(dJane);
  });

  it('landing title is length-capped and keeps the brand suffix exactly once', () => {
    const longHeadline = 'A'.repeat(300);
    const title = buildLandingTitle('Jane Doe', longHeadline);
    expect(title.length).toBeLessThanOrEqual(70);
    expect(title.endsWith(` | ${SITE_NAME}`)).toBe(true);
    expect(title.match(/ForkMyFolio/g)).toHaveLength(1);
  });

  it('missing headline produces a clean name-only title (no dangling dash)', () => {
    expect(buildLandingTitle('Jane Doe', null)).toBe('Jane Doe | ForkMyFolio');
    expect(buildLandingTitle('Jane Doe', '   ')).toBe('Jane Doe | ForkMyFolio');
  });

  it('section titles are per-section and length-capped', () => {
    expect(buildSectionTitle('Jane Doe', 'projects')).toBe('Jane Doe — Projects | ForkMyFolio');
    expect(buildSectionTitle('Jane Doe', 'experience')).toBe('Jane Doe — Experience | ForkMyFolio');
    const long = buildSectionTitle('X'.repeat(120), 'skills');
    expect(long.length).toBeLessThanOrEqual(70);
  });

  it('item titles lead with the project and fall back to the owner name', () => {
    expect(buildItemTitle('My App', 'Jane Doe')).toBe('My App — Jane Doe | ForkMyFolio');
    expect(buildItemTitle('', 'Jane Doe')).toBe('Jane Doe | ForkMyFolio');
  });

  it('display name falls back to a title-cased slug', () => {
    expect(buildDisplayName({}, 'jane-doe')).toBe('Jane Doe');
    expect(buildDisplayName({ firstName: 'Jane', lastName: 'Doe' }, 'x')).toBe('Jane Doe');
  });

  it('descriptions are never empty — summary, headline, then generic fallback', () => {
    expect(buildLandingDescription({ summary: 'Bio here.' }, 'Jane', 'jane')).toBe('Bio here.');
    expect(buildLandingDescription({ headline: 'Dev' }, 'Jane', 'jane'))
      .toContain('Jane — Dev');
    expect(buildLandingDescription({}, '', 'jane-doe'))
      .toBe('View the professional portfolio of Jane Doe on ForkMyFolio.');
  });

  it('section descriptions differ per section and honour dataLead', () => {
    const base = buildSectionDescription('projects', {}, 'Jane');
    const contact = buildSectionDescription('contact', {}, 'Jane');
    expect(base).not.toBe(contact);
    expect(buildSectionDescription('projects', {}, 'Jane', 'Custom lead.')).toBe('Custom lead.');
  });

  it('truncate collapses whitespace and ellipsizes', () => {
    expect(truncate('  a\n\n  b  ', 10)).toBe('a b');
    expect(truncate('x'.repeat(50), 10)).toHaveLength(10);
    expect(truncate('x'.repeat(50), 10).endsWith('…')).toBe(true);
  });
});

describe('resolveSocialImage (OG/Twitter image priority)', () => {
  it('uses the provided image and marks it rich (large card)', () => {
    const img = resolveSocialImage('https://cdn.example.com/me.jpg', 'https://site.com', 'Jane');
    expect(img.url).toBe('https://cdn.example.com/me.jpg');
    expect(img.isRich).toBe(true);
  });

  it('falls back to the local logo (absolutized) when no image exists', () => {
    const img = resolveSocialImage(null, 'https://site.com', 'Jane');
    expect(img.url).toBe('https://site.com/forkmyfolio_logo_icon.png');
    expect(img.isRich).toBe(false);
  });
});

describe('JSON-LD safety and privacy', () => {
  it('escapes < so user content cannot break out of the script tag', () => {
    const out = toJsonLd({ name: '</script><script>alert(1)</script>' });
    expect(out).not.toContain('</script>');
    expect(out).toContain('\\u003c');
  });

  it('ProfilePage JSON-LD excludes email and private fields', () => {
    const out = buildProfilePageJsonLd({
      name: 'Jane Doe',
      slug: 'jane-doe',
      origin: 'https://site.com',
      profile: {
        publicEmail: 'jane@secret.com',
        summary: 'Bio',
        headline: 'Dev',
        profileImageUrl: 'https://cdn.example.com/me.jpg',
        linkedinUrl: 'https://linkedin.com/in/jane',
        somePrivateField: 'secret',
      },
      user: { email: 'jane@secret.com', updatedAt: '2026-01-02T03:04:05' },
    });
    const parsed = JSON.parse(out);
    expect(parsed['@type']).toBe('ProfilePage');
    expect(parsed.mainEntity['@type']).toBe('Person');
    // regression: Person URL must not double-slash (origin + normalizePath)
    expect(parsed.mainEntity.url).toBe('https://site.com/jane-doe');
    expect(JSON.stringify(parsed)).not.toContain('secret.com');
    expect(JSON.stringify(parsed)).not.toContain('somePrivateField');
    expect(parsed.mainEntity.sameAs).toContain('https://linkedin.com/in/jane');
    expect(parsed.mainEntity.image).toBe('https://cdn.example.com/me.jpg');
  });

  it('WebSite JSON-LD carries the brand and origin', () => {
    const parsed = JSON.parse(buildWebSiteJsonLd('https://site.com'));
    expect(parsed['@type']).toBe('WebSite');
    expect(parsed.url).toBe('https://site.com');
  });

  it('BreadcrumbList positions are 1-based and ordered', () => {
    const parsed = JSON.parse(buildBreadcrumbJsonLd([
      { label: 'Jane', url: 'https://site.com/jane' },
      { label: 'Projects', url: 'https://site.com/jane/projects' },
    ]));
    expect(parsed.itemListElement[0].position).toBe(1);
    expect(parsed.itemListElement[1].position).toBe(2);
  });
});

describe('robots.txt (crawler switch)', () => {
  it('disallows everything when non-indexable (deny-by-default)', () => {
    const txt = buildRobotsTxt(false, 'https://site.com');
    expect(txt).toContain('Disallow: /');
    expect(txt).not.toContain('Sitemap:');
  });

  it('allows crawling but blocks auth/admin paths when indexable', () => {
    const txt = buildRobotsTxt(true, 'https://site.com');
    expect(txt).toContain('Allow: /');
    expect(txt).toContain('Disallow: /login');
    expect(txt).toContain('Disallow: /admin');
    expect(txt).toContain('Sitemap: https://site.com/sitemap.xml');
  });
});

describe('sitemap.xml (public-only URLs)', () => {
  it('contains the landing page and only public slugs with lastmod dates', () => {
    const xml = buildSitemapXml(
      [
        { slug: 'jane-doe', updatedAt: '2026-01-02T03:04:05' },
        { slug: 'john-smith', updatedAt: null },
      ],
      'https://site.com',
    );
    expect(xml).toContain('<loc>https://site.com/</loc>');
    expect(xml).toContain('<loc>https://site.com/jane-doe</loc>');
    expect(xml).toContain('<lastmod>2026-01-02</lastmod>');
    expect(xml).toContain('<loc>https://site.com/john-smith</loc>');
    expect(xml).not.toContain('lastmod></lastmod>'); // no empty lastmod tags
  });

  it('skips malformed slugs and deduplicates entries (never leaks junk)', () => {
    const xml = buildSitemapXml(
      [
        { slug: 'ok-slug' },
        { slug: '../etc/passwd' },
        { slug: '' },
        { slug: 'ok-slug' },
      ],
      'https://site.com',
    );
    expect(xml.match(/ok-slug/g)).toHaveLength(1);
    expect(xml).not.toContain('..');
  });

  it('skips slugs with non-URL-safe characters outright (stronger than escaping)', () => {
    const xml = buildSitemapXml([{ slug: 'a&b' }], 'https://site.com');
    expect(xml).not.toContain('a&amp;b');
    expect(xml).not.toContain('a&b');
  });
});

// --- Phase 10 (Workstream 2.3): per-user robots directive resolution ---

describe('resolveRobotsDirective (P10 per-user indexing)', () => {
  it('returns null when the global switch is off (plugin owns the site-wide noindex)', () => {
    expect(resolveRobotsDirective(false, false, true)).toBeNull();
    expect(resolveRobotsDirective(false, true, false)).toBeNull();
  });

  it('emits noindex,nofollow for not-found pages regardless of preference', () => {
    expect(resolveRobotsDirective(true, true, true)).toBe('noindex, nofollow');
    expect(resolveRobotsDirective(true, true, false)).toBe('noindex, nofollow');
  });

  it('treats anything but explicit true as opted-out (conservative strict check)', () => {
    expect(resolveRobotsDirective(true, false, false)).toBe('noindex, nofollow');
    expect(resolveRobotsDirective(true, false, undefined)).toBe('noindex, nofollow');
    expect(resolveRobotsDirective(true, false, null)).toBe('noindex, nofollow');
    expect(resolveRobotsDirective(true, false, 'true')).toBe('noindex, nofollow'); // string, not boolean
    expect(resolveRobotsDirective(true, false, 1)).toBe('noindex, nofollow');
  });

  it('allows indexing only when globally enabled, found, and explicitly opted in', () => {
    expect(resolveRobotsDirective(true, false, true)).toBe('index, follow');
  });
});
