import { computed } from 'vue';
import { useApi } from './useApi';

/**
 * @file app/composables/useAnalyticsTracking.ts
 * @description Public portfolio analytics tracking (DEF-1).
 *
 * Fetches the non-credential tracking configuration for the current portfolio
 * slug from the backend (which resolves ownership server-side) and injects
 * the matching provider tracking script(s) via `useHead`.
 *
 * Guarantees:
 *  - SSR-safe: `useHead` renders the script tag(s) in the server-rendered
 *    HTML; Nuxt deduplicates them by key during hydration and client-side
 *    navigation, so no duplicate injection occurs.
 *  - No privileged credentials: the endpoint only returns public tracking
 *    values (script URL, public website/measurement ID, recorder flag) — the
 *    same values that are visible in the rendered page anyway.
 *  - Provider-agnostic: the backend tells us which provider applies and the
 *    full script URL (including any non-standard script path), so changing a
 *    deployment's provider, base URL or paths in the admin GUI never requires
 *    rebuilding this frontend.
 *  - Fails soft: any error fetching the config (private portfolio 404, backend
 *    downtime, disabled analytics) simply results in no tracking — public
 *    rendering is never affected.
 *  - Privacy: `data-do-not-track="true"` makes the Umami tracker respect the
 *    browser's Do Not Track signal; the tracker is cookieless by design.
 *
 * Known limitation: navigating
 * client-side from one owner's portfolio to a different owner's portfolio
 * keeps attributing pageviews to the first owner's website, because the
 * Umami tracker binds its website ID once at script load. Full page loads
 * (the common case for cross-owner entry) attribute correctly.
 */
interface PublicTrackingConfig {
  provider: 'NONE' | 'UMAMI' | 'GA4';
  enabled: boolean;
  scriptUrl: string | null;
  websiteId: string | null;
  recorderEnabled: boolean;
  recorderUrl: string | null;
}

export function useAnalyticsTracking() {
  const route = useRoute();
  const { fetchJson } = useApi();

  const slug = computed(() => String(route.params.slug || ''));

  const { data: trackingConfig } = useAsyncData<PublicTrackingConfig | null>(
    `analytics-tracking-${slug.value}`,
    async (): Promise<PublicTrackingConfig | null> => {
      if (!slug.value) return null;
      try {
        return await fetchJson<PublicTrackingConfig>(`/public/analytics/${slug.value}/tracking-config`);
      } catch {
        // Disabled analytics, private/unknown slug or provider downtime:
        // tracking silently off — public rendering must never be affected.
        return null;
      }
    },
    { watch: [slug] }
  );

  useHead(() => {
    const cfg = trackingConfig.value;
    if (!cfg || !cfg.enabled || !cfg.scriptUrl || !cfg.websiteId) {
      return {};
    }

    if (cfg.provider === 'GA4') {
      // GA4: gtag.js plus the inline config snippet binding the Measurement ID.
      const measurementId = cfg.websiteId;
      return {
        script: [
          {
            key: 'ga4-gtag',
            src: cfg.scriptUrl,
            defer: true,
          },
          {
            key: 'ga4-config',
            innerHTML:
              `window.dataLayer=window.dataLayer||[];` +
              `function gtag(){dataLayer.push(arguments);}` +
              `gtag('js',new Date());` +
              `gtag('config','${measurementId}');`,
          },
        ],
      };
    }

    // Umami (default): tracker script bound to the public website ID, with an
    // optional session recorder (only when explicitly enabled server-side).
    const scripts: Array<Record<string, unknown>> = [
      {
        key: 'umami-tracker',
        src: cfg.scriptUrl,
        'data-website-id': cfg.websiteId,
        'data-do-not-track': 'true',
        defer: true,
      },
    ];
    if (cfg.recorderEnabled && cfg.recorderUrl) {
      scripts.push({
        key: 'umami-recorder',
        src: cfg.recorderUrl,
        'data-website-id': cfg.websiteId,
        defer: true,
      });
    }
    return { script: scripts };
  });
}
