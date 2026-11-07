# ForkMyFolio Backend API

Spring Boot 3 REST API (`/api/v1`) for ForkMyFolio — a self-hosted,
multi-user portfolio platform. See the [root README](../README.md) for the
project overview, deployment, and configuration.

## Stack

- Java 21, Spring Boot 3.5 (Spring Security with JWT, Spring Data JPA)
- MySQL 8 (production), Redis 7 (caching, rate-limit buckets)
- Flyway migrations (`src/main/resources/db/migration`)
- OpenPDF 3.0.5 + ZXing 3.5.3 for PDF portfolio export (9 templates)
- springdoc-openapi (Swagger UI at `/swagger-ui.html`)

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

## API documentation

Swagger UI: `/swagger-ui.html` · OpenAPI spec: `/api-docs` · health probe:
`/actuator/health`. These paths are served directly by the backend; the
bundled same-host proxy only routes `/api/`, so reach them via the backend
port (or your own proxy rule) rather than through the public site.
