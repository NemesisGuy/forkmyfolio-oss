# ForkMyFolio — Feature List

**Task:** D1 — feature list for the entire system (what the product does today).
**Snapshot:** Phase 0.30 feature set; documentation corrected against the
v1.2.11 code on 2026-11-26 (behaviour claims verified against source).

ForkMyFolio is a self-hosted, multi-user portfolio platform. Each user builds
a portfolio (profile, experience, projects, skills, qualifications,
testimonials) and publishes it as a public website addressed by a personal
slug. Administrators manage accounts, content and platform settings for the
whole installation.

---

## 1. Accounts & authentication

- **Local registration** — email + password with mandatory terms-of-service
  acceptance (timestamp and terms version recorded on the account).
- **Login / logout** — stateless JWT **access token** plus an **httpOnly
  refresh-token cookie** with rotation: every refresh issues a new refresh
  token and invalidates the old one.
- **Social login (OAuth2 / OIDC, opt-in)** — the stack ships the full
  provider-neutral path, disabled by default. The API stashes the authorization
  request in a short-lived signed cookie, links or creates the account on
  callback, and issues a JWT + HttpOnly refresh cookie. The bundled nginx proxy
  routes `/oauth2/**` to the backend (the SPA's `/oauth2/redirect` callback view
  is served by the frontend in same-host mode). Enable the SPA button with
  `VITE_SOCIAL_LOGIN_ENABLED=true` and `VITE_SOCIAL_PROVIDERS=keycloak`; it
  renders a vendor-neutral "Sign in with SSO" alternative only on /login and /register (never in the public-site navbar). The
  provider's client id/secret and issuer URI are configured only in `.env`
  — never in a frontend build. The SPA currently surfaces only the `keycloak`
  provider id; Google and GitHub registrations remain backend-only UI-wise.
- **Password change** — authenticated users can change their own password
  (`password_last_changed_at` is tracked).
- **Admin password reset** — an administrator can reset any user's password.
  The reset updates `password_last_changed_at`; it does **not** trigger the
  force-password-change screen (only a freshly seeded admin with a null
  timestamp is forced to change it on first login).
- **Re-authentication for sensitive operations** — before exporting or
  restoring a backup the current password is re-verified and a short-lived
  **backup token** is issued (`POST /api/v1/auth/verify-password`).
- **Password visibility toggle** in the backup password-verification modal.
- **Rate limiting** — a Bucket4j-based filter throttles public portfolio,
  analytics and download traffic, plus (since v1.2.11) the unauthenticated
  `/auth/login`, `/register` and `/guest-login` endpoints with a per-client-IP
  budget (default 120 per 5 minutes, env-tunable). `/refresh-token` is exempt
  (every SPA page load probes it; it only consumes a server-issued random
  token). Client-supplied forwarding headers cannot be used to choose a fresh
  bucket.
- **Deactivation kills sessions** — deactivating a user revokes all refresh
  tokens and rejects existing JWTs (including guest JWTs carrying that owner).

## 2. Roles & access control

| Role | Scope |
|---|---|
| `ADMIN` | Full platform administration (`/admin/*` SPA routes, `/api/v1/admin/**`). |
| `USER` | Owns one portfolio; all `/api/v1/me/**` and `/api/v1/management/**` data. |
| `GUEST` | **Read-only**, time-limited JWT derived from a guest pass (see §7). Never stored on the account — it exists only inside the token. |

- Route-level enforcement in Spring Security (`hasRole` matchers) plus
  method-level `@PreAuthorize` on controllers; ownership checks live in the
  service layer.
- Guest JWTs may **read** `/api/v1/me/**` but every mutating method requires
  `USER`/`ADMIN`, so a guest pass can never write to the owner's portfolio.

## 3. Portfolio content management

All of the following are owned by the signed-in user and managed from the
`/:slug/manage/*` dashboard (Vue 3 SPA):

- **Public profile** — headline, summary, public email, location, website /
  LinkedIn / GitHub URLs, resume URL + preview image, cover-letter template,
  avatar. Three independent visibility switches:
  - `visible` — portfolio enabled at all,
  - `is_public` — reachable by anonymous visitors at `/:slug`,
  - `search_indexable` — opt-in to appearing in sitemaps/search engines
    (default **off**; a global install-level switch always wins).
- **Experience** — job title, company (name/URL/logo), location +
  location type (`REMOTE`/`ON_SITE`/`HYBRID`), employment type
  (`FULL_TIME`, `PART_TIME`, `CONTRACT`, `FREELANCE`, `INTERNSHIP`,
  `APPRENTICESHIP`), start/end dates, description, achievements, manual
  ordering (with position-0 auto-chronological fallback), skill tagging.
- **Projects** — title, description, repo/live URLs, image, media URL,
  manual ordering, visibility, structured **key features** list (ordered
  bullets), skill tagging.
- **Skills** — the user picks from the platform catalogue (§4) and can
  override proficiency level and write a personal description per picked
  skill.
- **Qualifications** — qualification + institution (name/URL/logo), field of
  study, level (11 values from `HIGH_SCHOOL` to `DOCTORATE`), start/completion
  year (ongoing study supported), grade, credential URL, ordering.
- **Testimonials** — quote, author name, author title, ordering/visibility.
- **Section visibility settings** — per-section on/off switches and theme
  (`portfolio.theme` and `portfolio.*.show` keys) that drive both the public
  site and exports.

## 4. Skills catalogue (platform-wide)

- One curated, installation-wide **skills catalogue** served by
  `GET /api/v1/skills` (authenticated users only): name, category, default
  proficiency level, description and a Font Awesome icon per skill.
- Seeded and kept current by `SkillDataLoader`, which **upserts** the JSON
  catalogue at startup by name (case-insensitive): inserts new skills,
  refreshes category/description/icon/level on existing ones, and never
  touches user-owned fields (uuid, visibility, per-user rows) or deletes
  anything.
- Users attach catalogue skills to their portfolio, to individual experiences
  and to projects (many-to-many join tables), which powers grouped skill
  display and PDF/Markdown rendering.

## 5. Public portfolio website (Nuxt 4 SSR)

Public pages served for every published slug:

- **Home** — hero/summary, contact links, resume download.
- **Projects** list + **project detail** page.
- **Skills** (grouped by category with icons), **Experience**,
  **Qualifications**, **Testimonials**.
- **Contact form** — visitors send a message that lands in the owner's inbox
  (§8).
- **SEO** — per-page metadata, `robots.txt`, generated `sitemap.xml` (built
  from the public-portfolio list, filtered by `search_indexable`), HTTP SEO
  headers middleware, and per-user `noindex` honoured from the payload.
- **Anonymous access rules** — private or hidden content returns 404 to
  visitors; owners always see their own content.

## 6. Exports & downloads

Any visitor (public mode) or the owner (full mode) can download the
portfolio:

- **PDF** — server-rendered with a selectable template. Eight templates ship
  today: `modern`, `elegance`, `classic`, `metro-light`, `executive`,
  `modern-minimal`, `technical`, `business-card`. (Legacy `metro-dark`
  preferences are transparently aliased to `metro-light`.)
- **Markdown** — GitHub-friendly `.md` export of the whole portfolio.
- **vCard** — contact card (`.vcf`) generated from the profile.
- **Cover letter** — exportable as its own PDF or merged into the portfolio
  PDF (`export.coverletter.mode`: `OFF` / `IN_PDF` / `SEPARATE`). The
  `export.coverletter.public` switch only allows the **merged** (`IN_PDF`)
  cover letter in visitor downloads; the **separate** cover-letter PDF is
  owner-only (404 to visitors) and is not counted as a visitor download.
- **Export shaping** — `export.mode` (`FULL` / `BRIEF`) and per-section
  `export.section.*` toggles control how much detail the document contains.
- **Visibility contract** — `PUBLIC` exports mirror exactly what the public
  site shows; `OWNER` exports include hidden/preview sections. Downloads are
  counted as visitor statistics.

## 7. Guest access (guest passes)

- Owners mint **time-limited guest passes** (label + expiry) from the
  dashboard; each pass has a single secret token.
- A visitor exchanges the token at `/guest-access` for a **read-only guest
  JWT** scoped to the owner's account. Guests may call `GET /api/v1/me` and
  other explicitly guest-permitted reads, but management controllers
  (`/me/projects`, `/me/experiences`, guest-pass management, etc.) carry
  `hasRole('USER')` and return 403 — so a guest does **not** get free access to
  the whole owner dashboard.
- Owners can list and revoke passes; guests can never see or manage them.

## 8. Contact & messaging

- **Public contact form** — anyone can message a portfolio owner
  (`POST /api/v1/portfolios/:slug/contact-messages`, rate-limited, counted as
  a visitor stat). Message priority defaults to `MEDIUM`.
- **Owner inbox** — list, mark read/replied/archived, set priority
  (`HIGH`/`MEDIUM`/`LOW`), delete.
- **Admin view** — administrators can read every message in the installation
  and delete any of them.

## 9. Job application tracker

- Per-user tracker for job applications with a **13-state pipeline**:
  `INTERESTED → PREPARING → DRAFT → APPLIED → ACKNOWLEDGED →
  INTERVIEW_SCHEDULED → INTERVIEW_COMPLETED → TECHNICAL_ASSESSMENT → OFFER`
  plus outcomes `REJECTED`, `WITHDRAWN`, `NO_RESPONSE`, `ARCHIVED` (two
  legacy values, `INTERVIEWING` and `ACCEPTED`, are retained for old
  records — 15 enum values in total).
- Rich application fields: job/company/location, type, work arrangement,
  application URL, resume version used, job description, discovered/deadline/
  follow-up/interview/assessment dates, recruiter contact, salary
  range/currency/period, applied-at and status-change timestamps.
- **Notes** — append-only dated notes per application.

## 10. Announcements

- Administrators compose announcements targeted at `ADMIN`, `USER`, or all
  users (no target role), and broadcast them platform-wide.
- Signed-in users see announcements for their role and mark them read
  (read receipts are stored per user); admins can list and delete
  announcements.

## 11. Settings & theming

- **Global settings** (administrator-managed key/value rows) define platform
  defaults: theme, section visibility, export mode, cover-letter behaviour,
  default PDF template, etc.
- **Per-user overrides** — users customise their own effective settings from
  `/:slug/manage/settings/*` (display, PDF, export pages); effective settings
  = global default + user override.
- **Public settings API** — the public site and exporters read only the
  boolean flags they need (never credentials).
- **PDF template catalogue** exposed at `GET /api/v1/settings/pdf-templates`
  so both frontends offer the live list of templates.

## 12. Web analytics

- **Provider integration** for **Umami** and **Google Analytics 4**: global
  configuration (admin) plus a per-user mode — `INHERIT` (use the platform
  config), `OWN` (personal config) or `DISABLED`.
- API keys are stored **encrypted at rest** (AES-256-GCM, only last 4 digits
  kept in clear for identification); Umami website ID / base URL and the GA4
  measurement ID are public-by-design tracking values.
- **Public tracking config** endpoint serves script URL + public website id
  to portfolio pages (unknown/private slugs are concealed as 404).
- **Connection test** for the effective configuration, and an **analytics
  dashboard** for the owner: summary, pageviews/sessions over time, and
  ranked metrics (pages, referrers, countries, browsers, devices, events).
- GA4 *reporting* is deliberately deferred (graceful 503) — tracking works,
  reporting is Umami-only today.

## 13. Visitor statistics

- Atomic, race-safe counters (`INSERT … ON DUPLICATE KEY UPDATE`) keyed by
  stat type + reference id for: total visits, portfolio views, project views,
  per-section views (projects/skills/experience/qualifications/testimonials),
  contact-form submissions, PDF / Markdown / vCard downloads, and
  login/logout success & failure.
- Owners see analytics for their own portfolio; administrators see
  installation-wide statistics on the admin stats page.

## 14. Backup & restore

- **User backup** — download a versioned JSON file containing all of the
  user's portfolio data (requires password re-verification → backup token).
- **User restore** — upload a user backup to restore that portfolio.
  Destructive: replaces the user's existing data after validation.
- **System backup (admin)** — download one versioned JSON of *all* data.
- **System restore (admin)** — restore the entire installation from a system
  backup (wipes existing data first), or restore a **single user** from a
  user-format backup.
- **Wipe (admin)** — delete all users, portfolios and skills (extreme
  caution; separate confirmed endpoint).
- Backups are validated before restore (schema/version checks).

## 15. Administration console

`/admin/*` in the Vue SPA (`requiresAdmin` guard):

- **Dashboard** — platform overview.
- **Users** — paginated list, create, edit, reset password, deactivate
  (soft delete).
- **Messages** — all contact messages across users; delete any.
- **Settings** — global application settings (bulk update).
- **PDF settings** — platform default PDF template.
- **Statistics** — installation-wide visitor statistics.
- **System backup** — create/restore backups, wipe data (§14).
- **Announcements** — compose/broadcast/list/delete (§10).
- **Analytics** — global analytics provider configuration (§12).

## 16. Policies & compliance

- Publicly served **Terms of Service** and **Privacy Policy**
  (`GET /api/v1/policies/terms`, `/api/v1/policies/privacy`) shown during
  registration and linkable from the site.
- Terms acceptance is recorded per user (timestamp + version).
- `SECURITY.md` / `CONTRIBUTING.md` / `LICENSE` + `THIRD_PARTY_NOTICES.md`
  ship with the repository.

## 17. Platform & infrastructure

- **Backend** — Spring Boot (Java 21) REST API under `/api/v1` and an actuator
  health endpoint (internal-only through the bundled proxy). OpenAPI/Swagger
  endpoints are `permitAll` at the backend but are **not routed by the bundled
  nginx proxy** — reach them only on a deliberately exposed backend port.
- **Databases** — MySQL 8 with **Flyway** migrations (`V1`–`V13` plus `V4_1`,
  additive and idempotent where possible); **Redis 7** for the `publicPortfolios`
  cache, which is evicted on every content mutation so public reads are
  always fresh. (`docker-compose.example.yml` sets
  `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, overriding the image's Flyway-only
  `none` default.)
- **Two frontends and a proxy** — Vue 3 admin SPA and Nuxt 4 SSR public site,
  both behind a single nginx reverse proxy (same-host or detached mode,
  configuration validated at container start).
- **Docker Compose** deployment for the whole stack; `.env.example` documents
  every required variable.
- **CORS** configurable per environment; **load-tests** folder holds stress
  and visibility-regression scripts.

---

*Companion document: [`DIAGRAMS.md`](DIAGRAMS.md) — feature map, key user
flows, auth/export flows and the full entity-relationship diagram.*
