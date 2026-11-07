import { buildRobotsTxt } from '#shared/utils/seo';

/**
 * @file server/routes/robots.txt.ts
 * @description Dynamic robots.txt. Deny-by-default: unless the deployment
 * explicitly sets NUXT_PUBLIC_INDEXABLE=true, the whole host is disallowed —
 * so dev/staging instances can never leak into search indexes. Replaces the
 * former static public/robots.txt (deleted; this route takes over).
 */
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const indexable = String(config.public.indexable ?? 'false').toLowerCase() === 'true';

  const host = getRequestHost(event, { xForwardedHost: true });
  const proto = getRequestProtocol(event, { xForwardedProto: true });
  const origin = host ? `${proto}://${host}` : null;

  setHeader(event, 'Content-Type', 'text/plain; charset=utf-8');
  setHeader(event, 'Cache-Control', `public, max-age=${indexable ? 3600 : 300}`);
  return buildRobotsTxt(indexable, origin);
});
