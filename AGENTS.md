# AGENTS.md

Instructions for AI coding agents and human contributors working in this
repository. Read this file before making any changes.

## Repository status

- This repository (`forkmyfolio-oss`) is **PUBLIC / open source** (MIT).
- The authoritative release tag is `v1.0.0`. Never rewrite history and never
  move, delete or re-point the release tag.

## Secrets policy — MANDATORY

**Never commit secrets of any kind.** This is a hard rule with no exceptions:

- No passwords, API keys, JWT signing secrets, OAuth secrets, private keys,
  personal access tokens, session cookies or database credentials — not even
  in "harmless" test values or example files.
- Do not commit `.env` files or any real-credential material. The only
  environment file allowed in the repository is `.env.example` containing
  placeholder values only.
- Do not commit private infrastructure details (internal hostnames, IPs that
  are not test fixtures, credentials baked into docs, DB dumps).
- Do not commit generated or process-specific artefacts: audit reports,
  manifests, scratch folders, build outputs (`node_modules`, `target/`,
  `dist/`), screenshots or local tooling state.
- Before proposing any change, scan your own diff for credential-shaped
  content (long base64/hex strings, `BEGIN ... PRIVATE KEY`, password
  assignments outside test fixtures).

If you suspect a secret has been leaked in a commit, do **not** open a public
issue. Report it privately via [SECURITY.md](SECURITY.md) (private
vulnerability reporting) and notify the maintainer immediately.

## Contribution rules

- External contributors: use the normal **fork → changes in your own fork →
  pull request** workflow. Push access to this repository is limited to the
  maintainer; direct pushes by anyone else are not permitted.
- Pull requests are welcome for bug fixes, documentation and tested features.
  Keep changes minimal, consistent with existing code style, and include
  tests where applicable.
- CI is self-hosted and not part of this repository; do not add third-party
  CI/billing workflows.

## Project conventions

- Backend: Spring Boot 3 (Java 21) under `backend/`.
- Frontend admin: Vue 3 SPA under `frontend-admin/`.
- Frontend public: Nuxt 3 SSR under `frontend-public/`.
- Deployment: Docker Compose (see `docker-compose.example.yml`, README and
  `frontend-admin/docs/technical/`).
