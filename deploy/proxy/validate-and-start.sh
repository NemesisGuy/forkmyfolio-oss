#!/bin/sh
# ForkMyFolio proxy startup (Phase 9).
#
# 1. Select the template for PROXY_MODE (same-host | detached).
# 2. Sanity-check required environment values BEFORE rendering (a meaningful
#    error beats an opaque nginx -t emerg after an empty envsubst).
# 3. Render via the official image's 20-envsubst-on-templates.sh (substitutes
#    only defined env vars; nginx runtime vars are preserved).
# 4. Validate with nginx -t — an invalid configuration ABORTS the container
#    start instead of silently serving a stale default.
# 5. Hand off to the stock entrypoint (it re-renders identically, then starts
#    nginx).
set -e

MODE="${PROXY_MODE:-same-host}"
TEMPLATE_DIR="/etc/nginx/templates-available"
RENDER_DIR="/etc/nginx/templates"

case "$MODE" in
    same-host) SRC="$TEMPLATE_DIR/default.conf.template.same-host" ;;
    detached)  SRC="$TEMPLATE_DIR/default.conf.template.detached" ;;
    *)
        echo "[proxy-entrypoint] FATAL: PROXY_MODE='$MODE' is invalid (expected same-host|detached)" >&2
        exit 1
        ;;
esac

if [ -z "$UPSTREAM_APP" ] || [ -z "$UPSTREAM_NUXT" ]; then
    echo "[proxy-entrypoint] FATAL: UPSTREAM_APP and UPSTREAM_NUXT must be non-empty" >&2
    exit 1
fi
if [ "$MODE" = "same-host" ] && [ -z "$UPSTREAM_FRONTEND" ]; then
    echo "[proxy-entrypoint] FATAL: UPSTREAM_FRONTEND must be non-empty in same-host mode" >&2
    exit 1
fi
if [ -z "$CLIENT_MAX_BODY_SIZE" ]; then
    echo "[proxy-entrypoint] FATAL: CLIENT_MAX_BODY_SIZE must be non-empty" >&2
    exit 1
fi

mkdir -p "$RENDER_DIR"
# Remove any previously rendered config/template so a PROXY_MODE switch cannot
# leave a stale server block behind, then stage only the selected template.
rm -f "$RENDER_DIR/default.conf.template" "$RENDER_DIR/default.conf"
cp "$SRC" "$RENDER_DIR/default.conf.template"

echo "[proxy-entrypoint] mode=$MODE rendering $(basename "$SRC")"
/docker-entrypoint.d/20-envsubst-on-templates.sh >/dev/null

echo "[proxy-entrypoint] validating rendered nginx config..."
nginx -t

exec /docker-entrypoint.sh "$@"
