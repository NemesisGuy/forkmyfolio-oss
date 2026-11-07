# forkmyfolio-proxy

Reusable, versioned reverse-proxy image for ForkMyFolio deployments.
Replaces host bind-mount `nginx-proxy.conf` workflows: the configuration is
versioned with the release and configured at container start via environment
variables. No secrets and no deployment-specific values are baked in.

## Configuration contract

| Variable | Default | Purpose |
|---|---|---|
| `PROXY_MODE` | `same-host` | `same-host` (SPA + API + Nuxt behind one proxy) or `detached` (SPA hosted externally; proxy serves API + Nuxt only) |
| `UPSTREAM_FRONTEND` | `frontend:80` | Vue SPA upstream (same-host mode only) |
| `UPSTREAM_NUXT` | `nuxt:3000` | Nuxt SSR upstream |
| `UPSTREAM_APP` | `app:8080` | Spring Boot API upstream |
| `CLIENT_MAX_BODY_SIZE` | `25m` | Proxy upload ceiling (backup restore, media) |

Any invalid `PROXY_MODE`, an empty required variable, or a rendered config
that fails `nginx -t` aborts the container start (fail-fast, visible in
`docker logs`).

## Modes

- **same-host** — full routing: `/api/` → app; SPA/auth/manage/assets/`config.js`/favicon → frontend; everything else → Nuxt.
- **detached** — `/api/` → app; everything else → Nuxt. No frontend upstream exists in this template, so the proxy starts cleanly without a frontend service.

## Build

```bash
docker build -t forkmyfolio-proxy deploy/proxy/
```

The proxy is built automatically by the example compose file (`build:
./deploy/proxy`); the command above is for standalone use. Architecture and
configuration details are documented in the `Dockerfile` and
`validate-and-start.sh` comments.
