# Licensing Assessment — ForkMyFolio OSS

**Status: DECIDED — MIT applied (root `LICENSE`, Copyright (c) 2026 NemesisNet).**
This document is retained as the audit record of the licensing analysis that
informed the decision. Nothing here constitutes legal advice.

## 1. The decision (RECORDED)

| Question | Decision |
|---|---|
| Which license applies to this repository? | **MIT — approved by the owner and applied** (root `LICENSE`, `Copyright (c) 2026 NemesisNet`) |

MIT was applied and the repository published; this document is now a
historical audit record, not a pre-release checklist.

## 2. Copyright ownership

- All application source in this repository originates from the owner's
  private repositories (`ForkMyFolio-backend`, `ForkMyFolio-frontend-vue`,
  and the outer working tree containing the Nuxt application). No third
  party holds copyright in the application source. **[ASSUMED — owner
  confirms there are no contracted contributors or employer claims.]**
- Recommended copyright line for MIT: `Copyright (c) 2026 <owner name/handle>`.
  The owner must supply the exact name/handle.

## 3. Dependency and asset obligations (the part that can change the decision)

### 3.1 RESOLVED — iText 7 (AGPL-3.0) was replaced by OpenPDF

**Historical finding:** the original assessment flagged `com.itextpdf:itext7-core`
(AGPL-3.0/commercial dual licence) as the single most consequential licensing
risk for an MIT-licensed distribution. **Resolved by option 2:** the PDF layer
was migrated to **OpenPDF** (MPL-2.0/LGPL-2.1), with the migration rationale
recorded in `PDF_LIBRARY_MIGRATION_ASSESSMENT.md` and the dependency notices in
`THIRD_PARTY_NOTICES.md`. No iText code or binaries ship in current releases.
The old CRITICAL/release-blocker wording below was removed when the migration
completed; see the migration assessment for the original analysis.

### 3.2 MySQL Connector/J — GPL-2.0 with Universal FOSS Exception

- `com.mysql:mysql-connector-j` is GPL-2.0 licensed but ships with Oracle's
  "Universal FOSS Exception", which permits its use in independently
  licensed open-source applications. Usable in an MIT project; must be
  listed in third-party notices.

### 3.3 Bundled fonts

All bundled fonts are from families with established open licenses. The
applicable license texts ship alongside the fonts (see
`backend/src/main/resources/fonts/LICENSE-NOTICE.md` and `OFL-1.1.txt`);
only the families actually used by the PDF templates are kept in the
repository.

| Font files | Family | License | Notes |
|---|---|---|---|
| `Font Awesome 6/7 Brands-Regular-400.otf`, `Free-Regular-400.otf`, `Free-Solid-900.otf` | Font Awesome Free | Fonts: **SIL OFL 1.1**; icons: CC BY 4.0 | Font Awesome Free licensing; attribution required for icons |
| `Lato-Regular.ttf`, `Lato-Italic.ttf` | Lato | **SIL OFL 1.1** | |
| `Montserrat-Bold.ttf` | Montserrat | **SIL OFL 1.1** | |
| `NotoColorEmoji.ttf`, `NotoEmoji-Regular.ttf`, `NotoSansSymbols-*.ttf` | Noto | **SIL OFL 1.1** | |

OFL permits bundling and redistribution; the obligations are: include the
license text, do not sell the fonts standalone, respect Reserved Font Names
where applicable. **[License families ASSUMED from the published licensing
of these font families — owner/maintainer must confirm each file's exact
license before publication.]**

### 3.4 Frontend JavaScript dependencies

- `frontend-admin/` and `frontend-public/` dependencies are overwhelmingly
  MIT/BSD/Apache-2.0 (Vue, Vite, Nuxt, Pinia, Bootstrap, Bootstrap Icons,
  axios, jwt-decode, lucide-vue-next, Vitest, Playwright, etc.).
- A complete machine-generated attribution report (e.g. `npm ls`-based
  license extraction) should be generated at publication time and attached
  to THIRD_PARTY_NOTICES.md.

### 3.5 Other backend dependencies (headline licenses)

| Dependency | License |
|---|---|
| Spring Boot / Spring Security ecosystem | Apache-2.0 |
| JJWT (JWT library) | Apache-2.0 |
| Bucket4j (rate limiting) | Apache-2.0 |
| Jackson | Apache-2.0 |
| Flyway | Apache-2.0 |
| Micrometer / Prometheus registry | Apache-2.0 |
| springdoc-openapi | Apache-2.0 |
| Google API client (GA4 integration) | Apache-2.0 |
| Testcontainers, JUnit, Mockito (test scope) | MIT / EPL / Apache-2.0 |

### Historical: commercial-use implications at decision time

- **MIT (adopted):** anyone may use, modify, and commercially exploit the code,
  including a private commercial edition. Same-snapshot divergence with
  one-way OSS→private merges is compatible with MIT as long as the private
  edition retains the copyright and license notice for OSS-derived parts.
- **iText/AGPL interaction:** resolved by the OpenPDF migration (§3.1); no
  AGPL-licensed PDF dependency remains in current releases.
- **Fonts:** OFL fonts may be bundled in commercial products; the license
  text must ship with them.

## 5. What happens next

1. ~~Owner records a license decision~~ — **DONE: MIT approved and applied
   (root `LICENSE`, Copyright (c) 2026 NemesisNet).** The iText question was
   resolved by the completed OpenPDF migration (option 2) — see
   `PDF_LIBRARY_MIGRATION_ASSESSMENT.md`.
2. ~~Add the MIT LICENSE file~~ — done. Font license texts (`OFL-1.1.txt`
   alongside `LICENSE-NOTICE.md`) and the npm attribution report shipped with
   the published repository.
3. If the decision changes, re-run this assessment's compatibility check
   against §3.
