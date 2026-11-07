package com.forkmyfolio.service.pdf;

import com.forkmyfolio.model.*;
import com.forkmyfolio.model.enums.CoverLetterExportMode;
import com.forkmyfolio.model.enums.ExportMode;

import java.util.List;

/**
 * A data transfer object to hold all the necessary information for building a PDF.
 * This simplifies method signatures for PDF templates.
 *
 * <p>
 * Phase 8 (PRV-1): the record carries privacy-safe derived fields so that
 * templates render exactly what the caller's visibility contract permits:
 * <ul>
 *   <li>{@code contactEmail} — the email to show on the document (public
 *       email for public exports; the owner may see their login email in
 *       owner-view exports via Phase 8.3 preferences). Always non-null;
 *       may be blank.</li>
 *   <li>{@code includeSummary} — whether the About/Summary section should
 *       be rendered (public exports mirror the profile's own section
 *       toggle; owner exports honour the owner's export preferences).</li>
 *   <li>{@code profileImageUrl} — image URL for templates that render a
 *       photo (public-safe surface, same value the public pages expose).</li>
 * </ul>
 *
 * <p>
 * Phase 8.3 (export preferences): the record additionally carries the
 * owner-driven selections resolved by {@code ExportContentService}:
 * <ul>
 *   <li>{@code testimonials} — visibility-filtered testimonials (rendered
 *       by the Markdown export and the current-generation PDF templates).</li>
 *   <li>{@code includeContact} — whether the contact block renders at all
 *       (owner export preference; always {@code true} for PUBLIC exports).</li>
 *   <li>{@code exportMode} — FULL (default) or BRIEF (concise resume
 *       formatting). Owner preference; PUBLIC exports are always FULL.</li>
 * </ul>
 * The legacy constructors are retained for source compatibility with
 * existing tests/templates; they delegate with FULL/all-enabled defaults.
 *
 * <p>
 * Phase 0.19 (cover letter export): the record additionally carries the
 * owner's cover letter raw text ({@code coverLetterTemplate}) and the
 * resolved behaviour ({@code coverLetterMode}). {@link #includeCoverLetter()}
 * is the single gate every renderer (resume-PDF appended page + Markdown
 * section) checks, so the IN_PDF path can never drift between formats.
 */
public record PortfolioData(
        PortfolioProfile profile,
        List<Experience> experiences,
        List<Qualification> qualifications,
        List<Project> projects,
        List<Skill> skills,
        List<Testimonial> testimonials,
        String contactEmail,
        boolean includeSummary,
        boolean includeContact,
        ExportMode exportMode,
        String profileImageUrl,
        String coverLetterTemplate,
        CoverLetterExportMode coverLetterMode
) {

    /**
     * Phase 0.19: the one gate for rendering a cover letter as part of an
     * IN_PDF export — true only when the mode is IN_PDF <em>and</em> the
     * owner actually wrote text. Standalone (SEPARATE) downloads use their
     * own gate so the two features stay independent.
     */
    public boolean includeCoverLetter() {
        return coverLetterMode == CoverLetterExportMode.IN_PDF
                && coverLetterTemplate != null && !coverLetterTemplate.isBlank();
    }

    /**
     * Legacy ten-field constructor (pre-0.19 shape). Derives the 0.19
     * fields with safe defaults: no cover letter text, mode OFF.
     */
    public PortfolioData(PortfolioProfile profile,
            List<Experience> experiences,
            List<Qualification> qualifications,
            List<Project> projects,
            List<Skill> skills,
            List<Testimonial> testimonials,
            String contactEmail,
            boolean includeSummary,
            boolean includeContact,
            ExportMode exportMode,
            String profileImageUrl) {
        this(profile, experiences, qualifications, projects, skills, testimonials,
                contactEmail, includeSummary, includeContact, exportMode, profileImageUrl,
                "", CoverLetterExportMode.OFF);
    }

    /**
     * Legacy eight-field constructor (pre-8.3 shape). Derives the 8.3
     * fields with safe defaults: no testimonials, contact block enabled,
     * FULL export mode.
     */
    public PortfolioData(PortfolioProfile profile,
            List<Experience> experiences,
            List<Qualification> qualifications,
            List<Project> projects,
            List<Skill> skills,
            String contactEmail,
            boolean includeSummary,
            String profileImageUrl) {
        this(profile, experiences, qualifications, projects, skills, List.of(),
                contactEmail, includeSummary, true, ExportMode.FULL, profileImageUrl);
    }

    /**
     * Legacy five-field constructor used by tests and templates. Derives the
     * privacy fields exactly the way the public portfolio pages would:
     * publicEmail-or-blank and the profile's own summary toggle.
     */
    public PortfolioData(PortfolioProfile profile,
            List<Experience> experiences,
            List<Qualification> qualifications,
            List<Project> projects,
            List<Skill> skills) {
        this(profile,
                experiences,
                qualifications,
                projects,
                skills,
                profile != null && profile.getPublicEmail() != null ? profile.getPublicEmail() : "",
                profile == null || profile.isVisible(),
                profile != null && profile.getUser() != null ? profile.getUser().getProfileImageUrl() : null);
    }
}
