# ForkMyFolio

A self-hosted, multi-user portfolio platform. Users manage their professional
portfolio content through an administration dashboard and publish it as a
fast, server-rendered public website — with PDF, Markdown, and vCard export,
optional web analytics, and full account backup/restore.

> **Status:** release candidate. The project is being prepared for its first
> open-source release; see [docs/](docs/) for curation notes.

## Core functionality

- **Portfolio management** — profile, skills, projects, experience,
  qualifications, testimonials, and site settings, managed per user through
  the administration dashboard.
- **Public portfolio sites** — each user's portfolio is served as a
  server-rendered (SSR) public website addressed by slug, including
  `robots.txt` and `sitemap.xml` generation and a configurable
  search-engine indexability switch.
- **Portfolio export** — download a portfolio as **PDF**, **Markdown**
  (GitHub-friendly `.md`), or **vCard**.
- **Authentication** — JWT access tokens with refresh-token rotation,
  registration, login, logout, and password verification endpoints.
- **Guest passes** — time-limited guest access to protected portfolio
  content via guest login.
- **Job application tracker** — users can record and track job
  applications.
- **Announcements** — administrator-published announcements.
- **Web analytics** — integration with **Umami** and **Google Analytics 4**
  (GA4); analytics provider credentials are stored encrypted at rest
  (AES-256-GCM) and can be managed per user.
- **Backup & restore** — users can export a full account backup and restore
  from it; administrators can manage backups centrally.
- **Contact messages** — visitors can send messages; users manage them in
  the dashboard.
- **Policies** — servable policy documents (e.g. privacy policy) per site.
- **Caching** — Redis-backed application caching.

## Architecture overview

Three applications plus infrastructure, fronted by a single reverse proxy:

```
                    ┌─────────────────────────────┐
  browser ────────► │  nginx reverse proxy (:80)  │  ← the only published port
                    └──────────┬──────────────────┘
             ┌─────────────────┼─────────────────────┐
             ▼                 ▼                     ▼
      ┌──────────────┐  ┌──────────────┐   ┌──────────────────┐
      │ frontend-    │  │ frontend-    │   │  backend (Spring │
      │ admin (Vue   │  │ public       │   │  Boot, Java 21)  │
      │ 3 SPA) /admin│  │ (Nuxt 3 SSR) │   │  REST API /api/v1│
      └──────────────┘  └──────────────┘   └───────┬──────────┘
                                                   ▼
                                     ┌────────────────────────┐
                                     │ MySQL 8 · Redis 7      │
                                     └────────────────────────┘
```

- **`backend/`** — Spring Boot REST API (`/api/v1`). Owns all data, business
  logic, authentication, PDF/Markdown/vCard generation, and analytics
  integration. Serves actuator health at `/actuator/health`.
- **`frontend-admin/`** — Vue 3 + Vite single-page application for portfolio
  administration, served by nginx. The API base URL is injected at container
  start (`entrypoint.sh` renders `config.js`), so one image works behind any
  proxy path.
- **`frontend-public/`** — Nuxt 3 SSR application rendering the public
  portfolio sites. It calls the backend server-side
  (`NUXT_API_BASE_URL=http://app:8080/api/v1`) and client-side
  (`NUXT_PUBLIC_API_BASE_URL=/api/v1`).
- **`deploy/proxy/`** — small nginx reverse-proxy image. In `same-host` mode
  it routes `/api` → backend, the admin dashboard → the Vue SPA, and
  everything else → the Nuxt public site, all on one port. Configuration is
  validated at container start; an invalid configuration aborts startup
  instead of silently serving a stale default.

## Technology stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security (JWT), Flyway migrations, OpenPDF 3.0.5 (PDF), ZXing 3.5.3 (QR) |
| Database | MySQL 8 |
| Cache | Redis 7 |
| Admin frontend | Vue 3, Vite, Pinia, Bootstrap, Vitest, Playwright |
| Public frontend | Nuxt 3 (SSR), Vitest |
| Proxy | nginx (envsubst templating, startup validation) |
| Deployment | Docker, Docker Compose |

## Requirements

- Docker Engine 24+ and Docker Compose v2 (the example stack builds all
  images from source; no local toolchain is required to run it).
- To develop without Docker:
  - Backend: JDK 21, Maven 3.9+ (a Docker-based build also works).
  - Frontends: Node.js 20 or 22, npm 10.

## Installation & Docker deployment

```bash
git clone <repository-url> forkmyfolio
cd forkmyfolio
cp .env.example .env
# edit .env — every value is a placeholder that MUST be replaced
docker compose -f docker-compose.example.yml up -d --build
```

This builds four images from source (backend, admin frontend, public
frontend, proxy) and starts MySQL and Redis from official images. The proxy
publishes **port 8089** on the host by default — override it by setting
`FMF_HOST_PORT` in `.env` if that port is already in use on your machine.

First startup takes a few minutes: the backend applies all database
migrations (Flyway) and initializes initial data before it reports healthy.

## Configuration

All configuration is environment-driven. Copy `.env.example` to `.env` and
set every value — the example compose file fails fast if a required variable
is missing.

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | MySQL **root** password (used only by the database container) |
| `DB_APP_PASSWORD` | MySQL password for the **application** account (kept separate from root) |
| `DB_USERNAME` | Application database username |
| `JWT_SECRET_KEY` | JWT signing secret — 96-byte Base64 (128 chars). Generate: `openssl rand -base64 96` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Initial administrator account; see below |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated origins allowed by the browser-facing CORS policy |
| `APP_COOKIE_SECURE` | Set `true` behind HTTPS, `false` for plain-HTTP local validation |
| `APP_COOKIE_SAMESITE`, `APP_SECURITY_COOKIE_DOMAIN` | Cookie hardening for detached/cross-site deployments |
| `ANALYTICS_ENCRYPTION_KEY` | AES-256-GCM key, Base64, exactly 32 bytes (`openssl rand -base64 32`). Encrypts stored analytics provider credentials. **Rotating it invalidates stored analytics credentials.** |
| `NUXT_PUBLIC_INDEXABLE` | `true` allows search-engine crawling of public sites; `false` for private deployments |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | **Optional** OAuth2 social login. Unset by default (the feature is inert). When enabling, also override `app.oauth2.authorized-redirect-uris` (env: `APP_OAUTH2_AUTHORIZEDREDIRECTURIS`, comma-separated) with your real post-login redirect, and register your public `/oauth2/callback/<provider>` URL with the provider — the built-in defaults point at localhost. |

The application database user is deliberately separate from the MySQL root
account: the application never needs root privileges.

## Initial admin setup

The administrator account is provisioned from the `ADMIN_EMAIL` and
`ADMIN_PASSWORD` environment variables. The backend **force-syncs the admin
password from `ADMIN_PASSWORD` on every startup** — the `.env` value is the
single source of truth. To change the admin password, change it in `.env`
and restart the backend; changing it only in the UI will be reverted on the
next restart.

Log in at the admin dashboard (served by the proxy on the same host) with
those credentials.

## Security considerations

- **Secrets live only in `.env`** (or your secrets manager). `.env` is
  git-ignored; never commit it. `.env.example` contains placeholders only.
- The JWT secret must be a freshly generated 96-byte Base64 value per
  deployment.
- Set `APP_COOKIE_SECURE=true` when serving over HTTPS, and configure
  `APP_SECURITY_COOKIE_DOMAIN` for cross-site (detached) deployments.
- Refresh tokens are stored server-side and rotated; logout revokes them.
- Analytics provider credentials (Umami/GA4) are encrypted at rest with
  AES-256-GCM using `ANALYTICS_ENCRYPTION_KEY`.
- The backend fails fast on startup if required secrets are missing rather
  than falling back to insecure defaults.
- The example stack is intended for evaluation and self-hosting behind your
  own TLS-terminating proxy for production use. Put real HTTPS in front of
  the proxy port before exposing it to the internet.
- **Actuator is internal-only** in the bundled deployment: the reverse proxy
  routes only `/api/` to the backend, so `/actuator/*` is reachable from
  inside the Docker network (health checks) but not from the internet. If
  you expose the backend port directly or customise the proxy, protect
  `/actuator/prometheus` (metrics) at your outer proxy.
- **Token storage:** the admin SPA keeps the short-lived access token in
  web storage (localStorage; sessionStorage for guest sessions) and relies
  on the HttpOnly refresh-token cookie for session continuity. The refresh
  token is rotated server-side and revoked on logout. This is the standard
  SPA tradeoff: an XSS vulnerability could read the access token, which is
  why its lifetime is short (1 hour by default) while the revocable refresh
  token lives in an HttpOnly cookie.
- **Database migrations** are managed by Flyway. Migration checksum
  validation is deliberately disabled (`validate-on-migrate=false`) to avoid
  false checksum mismatches caused by line-ending differences between
  development platforms and the container build; treat edits to
  already-applied migration files as forbidden, and prefer new migrations.

## Backup and restore

- **User backups:** authenticated users can download a full account backup
  and restore from a backup file through the dashboard.
- **Admin backups:** administrators can manage account backups centrally.
- **Database volume:** the example compose persists MySQL data in the
  `forkmyfolio_mysql_data` named volume. Include this volume in your own
  host-level backup routine (e.g. scheduled `mysqldump` of the container).

## Development setup

```bash
# backend (from backend/)
mvn spring-boot:run          # requires configured MySQL/Redis, see application.properties

# admin frontend (from frontend-admin/)
npm ci
npm run dev                  # Vite dev server
npm run test:unit            # Vitest unit tests
npm run build                # production build to dist/

# public frontend (from frontend-public/)
npm ci
npm run dev                  # Nuxt dev server
npm test                     # Vitest
npm run build                # SSR build to .output/
```

The backend test suite includes unit tests and Docker/Testcontainers-based
MySQL integration tests (`*IntegrationTest`); the integration tests require
a working Docker daemon.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security issues: see
[SECURITY.md](SECURITY.md) — please do not open public issues for
vulnerabilities.

## Known limitations

- The example compose targets HTTP on a single port; production deployments
  should terminate TLS in front of it.
- User/administration guides are not yet written; the root README and the
  documents in `docs/` are the current documentation set.
- Bundled fonts (Font Awesome 6, Lato, Montserrat) ship in the backend for
  PDF generation; their licenses are documented in
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and
  `backend/src/main/resources/fonts/LICENSE-NOTICE.md`.

## Troubleshooting

- **Port already in use** — the stack publishes one port (default 8089).
  Set `FMF_HOST_PORT` in `.env` to a free port and restart.
- **Proxy container exits immediately** — the proxy validates its
  configuration at startup and aborts on invalid settings. Check
  `docker logs <project>-proxy-1`: usually a missing `UPSTREAM_*` variable
  or an invalid `PROXY_MODE` (must be `same-host` or `detached`).
- **Backend unhealthy on first start** — first startup applies all Flyway
  migrations and initialises initial data; this can take a couple of
  minutes. `docker compose logs app` shows migration progress. The
  healthcheck allows a 60 s start period with 5 retries.
- **Backend fails fast at startup with a missing-secret error** — required
  variables (`JWT_SECRET_KEY`, `ADMIN_PASSWORD`, `DB_APP_PASSWORD`,
  `DB_PASSWORD`, `ANALYTICS_ENCRYPTION_KEY`, `DB_USERNAME`) must be set in
  `.env`; the application refuses to start with insecure defaults.
- **`413 Request Entity Too Large` on backup restore** — the proxy's
  default upload ceiling is 25 MB (`CLIENT_MAX_BODY_SIZE`); raise it in the
  proxy service environment for larger backups.
- **Cannot log in after changing the admin password in the UI** — the admin
  password is force-synced from `ADMIN_PASSWORD` on every backend startup;
  change it in `.env` and restart instead.

## Upgrading

1. Pull the new version of the repository (or your fork).
2. Review the release notes for new `.env` variables; add them to your
   `.env` (required variables fail fast if missing).
3. Rebuild and restart: `docker compose -f docker-compose.example.yml up -d --build`.
4. Database schema changes apply automatically on backend startup via
   Flyway; the MySQL data volume (`forkmyfolio_mysql_data`) is preserved.
   Back up that volume (or run an in-app account backup) before upgrading.

## License & attribution

ForkMyFolio is released under the [MIT licence](LICENSE) —
Copyright (c) 2026 NemesisNet. See [ATTRIBUTION.md](ATTRIBUTION.md) for
project attribution (including what the MIT licence actually requires
versus the author's courtesy request) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for third-party component
licences.
