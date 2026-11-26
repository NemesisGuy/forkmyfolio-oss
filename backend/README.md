# ForkMyFolio Backend API

Spring Boot 3 REST API (`/api/v1`) for ForkMyFolio — a self-hosted,
multi-user portfolio platform. See the [root README](../README.md) for the
project overview, deployment, and configuration.

## Stack

- Java 21, Spring Boot 3.5 (Spring Security with JWT, Spring Data JPA)
- MySQL 8 (production), Redis 7 (public-portfolio cache only; Bucket4j
  rate-limit buckets are in-memory)
- Flyway migrations (`src/main/resources/db/migration`, `V1`–`V13` + `V4_1`)
- OpenPDF 3.0.5 + ZXing 3.5.3 for PDF portfolio export (8 selectable
  templates; a 9th legacy class, `metro-dark`, is aliased to `metro-light`)
- springdoc-openapi (Swagger UI at `/swagger-ui.html` — served by the backend,
  **not** routed by the bundled nginx proxy)

## Layout

```
src/main/java/com/forkmyfolio/
  config/       Security, AppProperties, startup initialisation
  controller/   REST controllers (admin / management / guest / public)
  service/      Business logic (+ service/pdf: export templates)
  repository/   Spring Data repositories
  model/        JPA entities and enums
  dto/          Request/response DTOs
  security/     JWT provider, filters, rate limiting, OAuth2 support
  advice/, aop/, exception/, util/
```

## Running locally

Requires JDK 21 and a reachable MySQL 8 + Redis 7 (see
`src/main/resources/application.properties` for the environment variables;
`../.env.example` documents them).

```bash
mvn spring-boot:run
```

## Testing

```bash
mvn test -Dtest='!*IntegrationTest'   # unit tests (no Docker needed)
mvn test                              # + Testcontainers integration tests
                                      # (*IntegrationTest — requires a
                                      #  working Docker daemon)
```

Integration tests start a throwaway MySQL container via Testcontainers; no
local database state is used or modified.

### Experience ordering (Phase 0.29)

Manual ordering (`displayOrder` pinned slots) and position `0` = automatic
chronological placement are governed by ONE contract:
`com.forkmyfolio.util.ExperienceOrdering`, applied at every backend read
(admin + guest experience APIs, full public portfolio payload, Markdown
export, all eight PDF templates) and mirrored on the client in
`frontend-admin/src/utils/experienceOrdering.ts` and
`frontend-public/app/utils/experienceOrdering.ts`.

Regression coverage:

- `ExperienceOrderingTest` — 11 unit tests pinning the contract (slot order,
  date placement, current-job-first, deterministic tiebreaks, idempotent
  re-sort, input never mutated).
- `ExperienceOrderingIntegrationTest` — the full reported-defect scenario list
  driven through the real HTTP API on Testcontainers MySQL: multiple position
  changes in one editing session, 100 → 4 without clobbering other positions,
  reopen/reload stability, position-0 chronology for current + historical
  entries, overlapping employment periods, manual order never overwritten by
  automatic sorting, and every surface (admin list, guest API, public
  portfolio payload, Markdown export, PDF) serving the same order.
- `SkillServiceImplTest.findOrCreateSkills_emptyResultMustBeMutable_forHibernateMerge`
  — pins the root cause of the original persistence bug: a skills-less
  experience save returned the immutable `Set.of()`, which Hibernate's merge
  `clear()`-ed → HTTP 500 → transaction rollback → the position change was
  silently discarded.
- Client mirror specs: `frontend-admin/src/utils/experienceOrdering.spec.ts`,
  `frontend-public/tests/unit/experienceOrdering.spec.ts` (keep the mirrors
  behaviour-identical to the Java contract).

## API documentation

Swagger UI: `/swagger-ui.html` · OpenAPI spec: `/api-docs` · health probe:
`/actuator/health`. These paths are served directly by the backend; the
bundled same-host proxy only routes `/api/`, so reach them via the backend
port (or your own proxy rule) rather than through the public site.
