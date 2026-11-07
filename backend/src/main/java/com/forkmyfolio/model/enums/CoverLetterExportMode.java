package com.forkmyfolio.model.enums;

/**
 * Phase 0.19: the owner's cover letter export behaviour, persisted as the
 * {@code export.coverletter.mode} UserSetting value (seeded default OFF).
 *
 * <ul>
 *   <li>{@link #OFF} — the cover letter never appears in any export and
 *       the standalone download endpoint answers 404. Default for every
 *       fresh/existing account, so the export pipeline's byte-for-byte
 *       behaviour is unchanged until the owner opts in.</li>
 *   <li>{@link #IN_PDF} — the owner's resume PDF gains an appended cover
 *       letter page and their Markdown export gains a Cover Letter
 *       section. Applies to OWNER exports only.</li>
 *   <li>{@link #SEPARATE} — a standalone cover letter PDF is downloadable
 *       by the owner from their Data &amp; Exports page. The resume PDF and
 *       Markdown are NOT modified.</li>
 * </ul>
 *
 * <p>
 * PRV-1: cover letter content is owner-personal. PUBLIC exports always
 * behave as OFF regardless of the stored value (enforced by
 * {@code ExportContentService}), so public visitors can never receive it.
 */
public enum CoverLetterExportMode {
    OFF,
    IN_PDF,
    SEPARATE
}
