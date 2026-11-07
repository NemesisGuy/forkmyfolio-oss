# PDF Library Migration Assessment — iText 7 Replacement Options

**Status: RESOLVED — the replacement was implemented (OpenPDF + ZXing) and
verified against the full PDF export corpus.**
This document records the assessment behind that decision.

## 1. Why this assessment exists

The backend's PDF portfolio export uses `com.itextpdf:itext7-core` **7.2.6**,
which is dual-licensed **AGPL-3.0 / commercial**. An MIT-licensed project
bundling iText in a distributed server application carries AGPL §13
obligations for the combined work (anyone running the backend as a network
service must be able to obtain the corresponding source). This must be
consciously resolved — it cannot be silently accepted.

## 2. Current implementation inventory (verified)

| Metric | Value |
|---|---|
| Files importing iText | **12** |
| Total coupled lines | **≈ 2,400** |
| Templates | 9 (`Classic`, `Elegance`, `Modern`, `MetroDark` (disabled), `MetroLight`, `ModernMinimal`, `Executive`, `Technical`, `BusinessCard`) |
| Shared block library | `TemplateSupport` (552 lines) — used by all templates, includes `PdfCanvas` usage |
| API model | iText 7 **layout API used programmatically** — `Document`, `Div`, `Paragraph`, `Table`/`Cell`, `SolidBorder`, `UnitValue`, `TextAlignment`, `DeviceRgb`, `PdfFontFactory` (base-14 Helvetica + embedded TTF), `PdfAction` (hyperlinks), `BarcodeQRCode` |
| Not used | HTML→PDF (no `pdfHTML`), Markdown→PDF (Markdown export is a separate, iText-free service) |
| Fonts | Helvetica base-14 for most templates; `Montserrat-Bold`, `Lato-Regular/Italic` embedded from resources; Font Awesome icon fonts for glyphs |
| Existing tests | `PdfGenerationServiceTest` (661 lines) generates PDFs from real templates with stubbed data, including a cross-template assertions test; export endpoints covered by a Testcontainers integration test |

## 3. Candidate evaluation

### 3.1 OpenPDF (`com.github.librepdf:openpdf`) — **RECOMMENDED replacement**

- **License:** `MPL-2.0 OR LGPL-2.1+` (dual) — compatible with an MIT project
  used as an unmodified dependency.
- **Health (verified 2026-11):** active project (4.4k stars, 2,200 commits,
  CI on Java 21/24/25), current release line **3.0.5**; the 2.1.x+ branch
  requires **Java 21** — matches this project exactly.
- **Heritage:** the LGPL successor of iText (fork of iText 4.2.0 after iText
  moved to AGPL at 5.0) — conceptually the closest relative of the current
  code.
- **Capability coverage:** tables, borders, colors, TTF font embedding,
  hyperlinks, QR/barcode generation, page size/orientation control, optional
  HTML→PDF module (`openpdf-html`, LGPL-2.1), glyph/OpenType substitution
  support for non-Latin scripts.
- **Caveat:** OpenPDF 3.x uses the `org.openpdf` package with the **classic
  (iText 4-era) API** — `Document`/`Paragraph`/`Chunk`/`PdfPTable` — which is
  a **different layout model** from iText 7's `com.itextpdf.layout`. This is
  the core migration cost (see §4).

### 3.2 Apache PDFBox — capable but too low-level

- **License:** Apache-2.0 (maximally permissive).
- PDFBox is a PDF **primitive** library (content streams, no layout engine).
  Rebuilding 9 styled templates would mean writing a layout engine — the
  largest effort of all options. Not recommended as a direct replacement;
  relevant mainly as the engine under an HTML→PDF layer.

### 3.3 OpenHTMLtoPDF — capable but stale and architecturally mismatched

- **License:** LGPL-2.1+ — compatible.
- HTML/CSS→PDF on PDFBox 2. Would require **re-expressing all 9 templates as
  HTML/CSS** — a full redesign of the template layer, with real CSS-fidelity
  risk (the library explicitly warns against modern HTML5/CSS; floats near
  page breaks must be avoided, etc.).
- **Maintenance concern:** last stable release **1.0.10 (September 2021)**;
  no release in years. Not recommended as the primary path.

### 3.4 iText commercial license — no-code-change option

Removes AGPL obligations for the combined work. Cost is recurring and
per-negotiation; incompatible with "fully OSS" goals but compatible with the
private commercial edition strategy.

## 4. Why replacement was NOT implemented in this phase

The migration is **not a dependency swap** — it is a rewrite of the rendering
layer:

1. iText 7's layout model (`Div`/`Paragraph` fluent setters, `SolidBorder`,
   `UnitValue`, `Table`/`Cell` with per-cell styling) has **no 1:1 mapping**
   in OpenPDF's classic API (`Paragraph`/`Chunk`/`PdfPTable`/`PdfPCell`).
   Every one of the 9 templates and the 552-line shared block library must be
   re-authored, not mechanically translated.
2. **Visual regression risk is high and verification is not fully
   automatable**: the existing 661-line test suite verifies generation
   succeeds and asserts content, but pixel-level layout fidelity across 9
   templates (colors, spacing, borders, icon glyphs, QR placement) requires
   human visual comparison against baseline renders.
3. The PDF export is a flagship user-facing feature; shipping a rushed port
   that renders subtly wrong output would be worse than a documented,
   planned migration.

Per the phase instruction ("if replacement … requires significant redesign,
stop and document"), this assessment is that documentation.

## 5. Recommended migration plan (for the owner's decision)

**Recommended target: OpenPDF 3.x (`org.openpdf`), staged migration.**

| Step | Work | Verification |
|---|---|---|
| 1 | Freeze baseline: generate a PDF corpus from all 9 templates with fixed fixture data (the existing test's stub data); store rendered page images as the visual baseline | Existing tests + baseline artifacts |
| 2 | Port `TemplateSupport` (the shared block library) to `org.openpdf` — this carries most of the layout logic | Unit tests + side-by-side renders |
| 3 | Port templates one at a time (simplest → most complex: Executive/Technical/ModernMinimal → BusinessCard → MetroLight → Classic → MetroDark → Modern → Elegance), each behind the existing `PortfolioPdfTemplate` interface — **the API contract (`PdfFile` record, controller endpoints) does not change** | Per-template visual diff vs baseline |
| 4 | Swap the pom dependency; assert zero `com.itextpdf` imports remain (add an ArchUnit rule forbidding the package) | `grep` + ArchUnit |
| 5 | Full regression: backend unit tests, export integration test, fresh-clone compose run, manual download of each template's PDF | CI + E2E |

**Effort estimate:** several focused working sessions (the block-library port
is the long pole); not a quick patch, hence not executed under this phase's
"straightforward only" mandate.

**Interim risk note (until the migration lands):** while iText remains, any
*pre-built distribution* of the backend (e.g. published Docker images) and
the act of running the server as a network service implicate AGPL §13 for the
iText-covered portions. Publishing the **source repository** with a Maven
dependency on iText is materially lower-risk (iText's own jars are
distributed by iText under its own license), but the distinction must be an
explicit, recorded owner decision — not a silent default.

## 6. Decision required from the owner

| Option | Consequence |
|---|---|
| A. Approve staged OpenPDF migration (recommended) | MIT-clean publication after migration; PDF feature visually re-verified |
| B. Purchase iText commercial license | No code change; recurring cost; immediate publication possible |
| C. Accept AGPL obligation explicitly for iText portions | Publication possible now with documented AGPL disclosure in THIRD_PARTY_NOTICES |
| D. Drop/defer PDF export from the OSS edition | Markdown/vCard exports unaffected |

**Decision: option A was approved and implemented** — the OpenPDF + ZXing
migration landed and was verified; publication is not blocked by this item.
