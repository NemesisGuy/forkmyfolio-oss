# Licensing Assessment — ForkMyFolio OSS

**Status: DECIDED — MIT applied (root `LICENSE`, Copyright (c) 2026 NemesisNet).**
This document is retained as the audit record of the licensing analysis that
informed the decision. Nothing here constitutes legal advice.

## 1. The decision (RECORDED)

| Question | Decision |
|---|---|
| Which license applies to this repository? | **MIT — approved by the owner and applied** (root `LICENSE`, `Copyright (c) 2026 NemesisNet`) |

Until the owner records approval, **no `LICENSE` file may be added** and the
repository must not be published. This is a release blocker.

## 2. Copyright ownership

- All application source in this repository originates from the owner's
  private repositories (`ForkMyFolio-backend`, `ForkMyFolio-frontend-vue`,
  and the outer working tree containing the Nuxt application). No third
  party holds copyright in the application source. **[ASSUMED — owner
  confirms there are no contracted contributors or employer claims.]**
- Recommended copyright line for MIT: `Copyright (c) 2026 <owner name/handle>`.
  The owner must supply the exact name/handle.

## 3. Dependency and asset obligations (the part that can change the decision)

### 3.1 CRITICAL — iText 7 (PDF generation) is AGPL-3.0

- `com.itextpdf:itext7-core` **7.2.6** is used by `PdfGenerationService` to
  produce the PDF portfolio export.
- iText 7 is dual-licensed: **AGPL-3.0** or a commercial iText license.
- Implications for an MIT-licensed project:
  - The MIT license text itself does not forbid using AGPL dependencies.
  - **But any distribution of the backend that includes iText code or object
    code** (including the published Docker images built from this repo)
    makes the iText portions subject to **AGPL-3.0**, whose §13 requires
    offering the corresponding source of the combined work to users of a
    network service. Recipients must be able to exercise those AGPL rights
    for the iText-covered parts regardless of the MIT notice on the rest.
  - Practical resolution paths (owner decision):
    1. **Purchase an iText commercial license** (removes AGPL obligations),
    2. **Replace iText** with a permissively licensed PDF library — evaluated
       in depth in `PDF_LIBRARY_MIGRATION_ASSESSMENT.md` (recommendation:
       staged migration to OpenPDF, MPL-2.0/LGPL-2.1; requires a rendering-
       layer rewrite, so it is a planned migration, not a dependency swap),
    3. **Accept and document** the AGPL obligation for the iText-covered
       portion (the project stays MIT for its own code; the distribution
       obligation for iText parts is disclosed in THIRD_PARTY_NOTICES.md).
- **This is the single most consequential licensing finding of the audit.**
  It must be resolved (or consciously accepted) before publication.

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

## 4. Commercial-use implications (owner briefing)

- **MIT (recommended):** anyone may use, modify, and commercially exploit
  the code, including the future private commercial edition. The commercial
  edition strategy (same-snapshot divergence with one-way OSS→private
  merges between the editions) is fully compatible with MIT — the
  private edition can keep its own proprietary additions as long as it
  honors MIT in the parts it took from the OSS repo (retain the copyright
  and license notice).
- **AGPL interaction:** even under MIT, the iText-covered portions of any
  distributed backend image carry AGPL obligations (§3.1). If the owner
  intends to sell the commercial edition *without* source obligations
  flowing from the PDF feature, option 1 (commercial iText license) or
  option 2 (replace iText) is the clean path.
- **Fonts:** OFL fonts may be bundled in commercial products; the license
  text must ship with them.

## 5. What happens next

1. ~~Owner records a license decision~~ — **DONE: MIT approved and applied
   (root `LICENSE`, Copyright (c) 2026 NemesisNet).** The iText question is
   being resolved via the OpenPDF migration (option 2) — see
   `PDF_LIBRARY_MIGRATION_ASSESSMENT.md`.
2. ~~Add the MIT LICENSE file~~ — done. Remaining: font license texts
   (`OFL-1.1.txt` added alongside `LICENSE-NOTICE.md` in the fonts
   directory) and the generated npm attribution report at publication time.
3. If the decision changes, re-run this assessment's compatibility check
   against §3.
