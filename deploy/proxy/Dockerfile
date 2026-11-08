# ForkMyFolio custom reverse-proxy image.
#
# Base: official nginx image, pinned to an explicit version. It ships
# /docker-entrypoint.d/20-envsubst-on-templates.sh, which renders
# /etc/nginx/templates/*.template into /etc/nginx/conf.d/ at container start,
# substituting ONLY environment variables that are defined (nginx runtime
# variables such as $host are preserved).
#
# No secrets are baked: this proxy needs only upstream service names and
# non-secret tuning values, all overridable at container start via env vars
# (see the ENV block below for the full configuration contract).
#
# Startup gate: validate-and-start.sh selects the template for PROXY_MODE,
# renders it, runs nginx -t, and aborts the container start on an invalid
# configuration instead of silently serving a stale default.
FROM nginx:1.30.0-alpine

# Phase 0.21: capacity-tuned main config (worker_connections 8192,
# worker_rlimit_nofile 65535, multi_accept) — see nginx.conf header.
COPY nginx.conf /etc/nginx/nginx.conf

COPY templates/default.conf.template.same-host /etc/nginx/templates-available/
COPY templates/default.conf.template.detached /etc/nginx/templates-available/
COPY validate-and-start.sh /validate-and-start.sh
RUN chmod +x /validate-and-start.sh

# Deliberate defaults for every variable the templates reference.
#   PROXY_MODE           selects the template (same-host | detached)
#   UPSTREAM_*           Docker-DNS service names on the compose network
#   CLIENT_MAX_BODY_SIZE raises nginx's 1m default so backup-restore uploads
#                        are not rejected with 413 through the proxy
ENV PROXY_MODE=same-host \
    UPSTREAM_FRONTEND=frontend:80 \
    UPSTREAM_NUXT=nuxt:3000 \
    UPSTREAM_APP=app:8080 \
    CLIENT_MAX_BODY_SIZE=25m

ENTRYPOINT ["/validate-and-start.sh"]
CMD ["nginx", "-g", "daemon off;"]
