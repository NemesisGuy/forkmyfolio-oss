package com.forkmyfolio.service.pdf;

import com.forkmyfolio.model.enums.CoverLetterExportMode;
import com.forkmyfolio.model.enums.ExportMode;

import java.util.Map;
import java.util.Set;

/**
 * Phase 8.3: an owner's resolved export preferences (mode + section
 * toggles), parsed from their {@code export.*} UserSetting overrides.
 *
 * <p>
 * Stored as UserSetting values over the globally seeded {@code export.*}
 * settings. Parsing is deliberately forgiving — the export pipeline must
 * NEVER fail because of missing, partial or malformed preferences:
 * <ul>
 *   <li>missing name ⇒ default (mode FULL, section enabled);</li>
 *   <li>value {@code false} (case-insensitive) ⇒ section disabled;</li>
 *   <li>any other value ({@code true}, {@code 1}, garbage) ⇒ enabled;</li>
 *   <li>unparseable mode ⇒ {@link ExportMode#FULL}.</li>
 *   <li>Phase 0.19: the cover letter mode is parsed FAIL-CLOSED — missing,
 *       blank or unparseable values ⇒ {@link CoverLetterExportMode#OFF}.
 *       Unlike the section toggles (where a malformed value must never
 *       silently LOSE content), a malformed cover letter value must never
 *       silently EXPOSE one.</li>
 * </ul>
 *
 * <p>
 * PRIVACY INVARIANT (PRV-1): these preferences are the OWNER's personal
 * rendering choices. They are applied by {@code ExportContentService} only
 * for {@code ExportVisibility.OWNER} exports; PUBLIC exports ignore them
 * completely and always mirror the public portfolio pages.
 *
 * <p>
 * Adheres to NAG-01 (DBR): immutable value object; no entity access.
 */
public record ExportPreferences(
        ExportMode mode,
        boolean summary,
        boolean experience,
        boolean projects,
        boolean skills,
        boolean qualifications,
        boolean testimonials,
        boolean contact,
        CoverLetterExportMode coverLetterMode) {

    /** UserSetting name carrying the export mode ({@code FULL}|{@code BRIEF}). */
    public static final String MODE_SETTING = "export.mode";

    /** UserSetting name prefix for the per-section toggles. */
    public static final String SECTION_PREFIX = "export.section.";

    /**
     * Phase 0.19: UserSetting name carrying the cover letter export
     * behaviour ({@code OFF}|{@code IN_PDF}|{@code SEPARATE}).
     */
    public static final String COVER_LETTER_MODE_SETTING = "export.coverletter.mode";

    /** Section keys, in canonical render order. */
    public static final Set<String> SECTION_KEYS = Set.of(
            "summary", "experience", "projects", "skills",
            "qualifications", "testimonials", "contact");

    /** The safe default: full portfolio, every section enabled, cover letter OFF. */
    public static ExportPreferences defaults() {
        return new ExportPreferences(ExportMode.FULL, true, true, true, true, true, true, true,
                CoverLetterExportMode.OFF);
    }

    /**
     * Resolves preferences from a name→value map of the owner's UserSetting
     * overrides. Missing/malformed entries fall back to the defaults; an
     * entirely empty map yields {@link #defaults()}.
     */
    public static ExportPreferences fromUserSettings(Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return defaults();
        }
        ExportMode mode = parseMode(overrides.get(MODE_SETTING));
        return new ExportPreferences(
                mode,
                parseToggle(overrides.get(SECTION_PREFIX + "summary")),
                parseToggle(overrides.get(SECTION_PREFIX + "experience")),
                parseToggle(overrides.get(SECTION_PREFIX + "projects")),
                parseToggle(overrides.get(SECTION_PREFIX + "skills")),
                parseToggle(overrides.get(SECTION_PREFIX + "qualifications")),
                parseToggle(overrides.get(SECTION_PREFIX + "testimonials")),
                parseToggle(overrides.get(SECTION_PREFIX + "contact")),
                parseCoverLetterMode(overrides.get(COVER_LETTER_MODE_SETTING)));
    }

    private static ExportMode parseMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ExportMode.FULL;
        }
        try {
            return ExportMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException malformed) {
            // Malformed stored value: fall back to the safe default rather
            // than failing the export.
            return ExportMode.FULL;
        }
    }

    /**
     * {@code false} (any case) disables; everything else — including null,
     * blank and unexpected values — enables, so a partially configured user
     * can never silently lose export content.
     */
    private static boolean parseToggle(String raw) {
        return !"false".equalsIgnoreCase(raw == null ? "" : raw.trim());
    }

    /**
     * Phase 0.19: cover letter mode parsing is FAIL-CLOSED — missing,
     * blank, malformed or unknown values all resolve to
     * {@link CoverLetterExportMode#OFF}. The stored value is case- and
     * spacer-insensitive ({@code "separate"}, {@code "in pdf"},
     * {@code "in-pdf"} and {@code "IN_PDF"} all resolve to IN_PDF).
     */
    private static CoverLetterExportMode parseCoverLetterMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return CoverLetterExportMode.OFF;
        }
        String normalized = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return CoverLetterExportMode.valueOf(normalized);
        } catch (IllegalArgumentException malformed) {
            // Fail-closed: anything unrecognised stays OFF — a malformed
            // value must never expose content (the opposite tradeoff from
            // the section toggles, which fail open to never lose content).
            return CoverLetterExportMode.OFF;
        }
    }
}
