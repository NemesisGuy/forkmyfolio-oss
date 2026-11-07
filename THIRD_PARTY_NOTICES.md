# Third-Party Notices

This repository incorporates third-party software and assets. This file
summarizes the principal components and their licenses; it is not an
exhaustive attribution report.

> **Note:** machine-generated attribution lists (backend Maven dependencies
> and frontend npm dependencies) are generated at release time and shipped
> with each release. This repository is licensed under the MIT licence —
> see [LICENSE](LICENSE) and [ATTRIBUTION.md](ATTRIBUTION.md).

## Backend dependencies

| Component | License | Notes |
|---|---|---|
| Spring Boot, Spring Security, Spring Data, Spring OAuth2 client | Apache-2.0 | Application framework |
| **OpenPDF (`com.github.librepdf:openpdf`)** | **MPL-2.0 OR LGPL-2.1+** | Used for PDF portfolio export (replaced iText 7 in the Phase 0.12 migration) |
| **ZXing core (`com.google.zxing:core`)** | **Apache-2.0** | QR-code generation for PDF templates (OpenPDF 3.x ships no QR support; replaced iText 7's `BarcodeQRCode` in the Phase 0.12 migration) |
| MySQL Connector/J | GPL-2.0 with Universal FOSS Exception | Permitted for use in FOSS applications per Oracle's exception |
| JJWT (jjwt-api/impl/jackson) | Apache-2.0 | JWT handling |
| Bucket4j | Apache-2.0 | Rate limiting |
| Jackson | Apache-2.0 | JSON processing |
| Flyway (flyway-core, flyway-mysql) | Apache-2.0 | Database migrations |
| Micrometer + Prometheus registry | Apache-2.0 | Metrics |
| springdoc-openapi | Apache-2.0 | API documentation |
| Google API client | Apache-2.0 | Analytics (GA4) integration |
| OWASP Java Encoder | BSD-3-Clause | Output encoding |
| Lombok | MIT | Build-time code generation (compile-only) |
| Testcontainers (JUnit Jupiter, MySQL modules) | MIT | Test-scope only |

## Frontend dependencies (headline)

| Component | License |
|---|---|
| Vue 3, Vue Router, Pinia | MIT |
| Vite, Vitest | MIT |
| Nuxt 3 | MIT |
| Bootstrap, Bootstrap Icons | MIT |
| axios | MIT |
| jwt-decode | MIT |
| lucide-vue-next | ISC |
| Playwright | Apache-2.0 |
| ESLint ecosystem, Prettier | MIT |

## Bundled fonts (backend `src/main/resources/fonts/`)

Used for server-side PDF generation. All are bundled under licenses that
permit redistribution; their license texts accompany the fonts (see
`backend/src/main/resources/fonts/LICENSE-NOTICE.md` and `OFL-1.1.txt`).

| Font family | Files | License |
|---|---|---|
| Font Awesome Free 6 | `Font Awesome 6 Brands-Regular-400.otf`, `Free-Regular-400.otf`, `Free-Solid-900.otf` | SIL OFL 1.1 (font software); icons CC BY 4.0 |
| Lato | `Lato-Regular.ttf`, `Lato-Italic.ttf` | SIL OFL 1.1 |
| Montserrat | `Montserrat-Bold.ttf` | SIL OFL 1.1 |

## Infrastructure images (deployment)

| Image | License |
|---|---|
| MySQL 8 (official image) | GPL-2.0 with FOSS exceptions (database server; used unmodified) |
| Redis 7 (official image) | RSAL/BSD — Redis 7 source-available; used unmodified via official image |
| nginx (official image) | BSD-2-Clause |
