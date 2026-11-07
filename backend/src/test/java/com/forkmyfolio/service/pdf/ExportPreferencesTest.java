package com.forkmyfolio.service.pdf;

import com.forkmyfolio.model.enums.CoverLetterExportMode;
import com.forkmyfolio.model.enums.ExportMode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8.3: unit tests for the {@link ExportPreferences} safe parser.
 * The export pipeline must NEVER fail because of missing, partial or
 * malformed {@code export.*} UserSetting values.
 */
public class ExportPreferencesTest {

    @Test
    void nullAndEmptyMaps_yieldDefaults() {
        assertThat(ExportPreferences.fromUserSettings(null)).isEqualTo(ExportPreferences.defaults());
        assertThat(ExportPreferences.fromUserSettings(Map.of())).isEqualTo(ExportPreferences.defaults());
    }

    @Test
    void defaults_areFullModeWithEverySectionEnabled() {
        ExportPreferences defaults = ExportPreferences.defaults();
        assertThat(defaults.mode()).isEqualTo(ExportMode.FULL);
        assertThat(defaults.summary()).isTrue();
        assertThat(defaults.experience()).isTrue();
        assertThat(defaults.projects()).isTrue();
        assertThat(defaults.skills()).isTrue();
        assertThat(defaults.qualifications()).isTrue();
        assertThat(defaults.testimonials()).isTrue();
        assertThat(defaults.contact()).isTrue();
        // Phase 0.19: fresh/existing accounts never export a cover letter
        assertThat(defaults.coverLetterMode()).isEqualTo(CoverLetterExportMode.OFF);
    }

    // ------------------------------------------------------------------
    // Phase 0.19: cover letter mode parsing (FAIL-CLOSED)
    // ------------------------------------------------------------------

    @Test
    void coverLetterMode_valuesParse() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("export.coverletter.mode", "IN_PDF");
        assertThat(ExportPreferences.fromUserSettings(overrides).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.IN_PDF);

        overrides.put("export.coverletter.mode", "SEPARATE");
        assertThat(ExportPreferences.fromUserSettings(overrides).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.SEPARATE);

        overrides.put("export.coverletter.mode", "OFF");
        assertThat(ExportPreferences.fromUserSettings(overrides).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.OFF);
    }

    @Test
    void coverLetterMode_parsingIsCaseInsensitiveAndSpacerTolerant() {
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.coverletter.mode", "separate")).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.SEPARATE);
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.coverletter.mode", "in pdf")).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.IN_PDF);
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.coverletter.mode", "IN-PDF")).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.IN_PDF);
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.coverletter.mode", " off ")).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.OFF);
    }

    @Test
    void coverLetterMode_missingOrMalformed_failsClosedToOff() {
        // Missing value
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.mode", "FULL")).coverLetterMode())
                .isEqualTo(CoverLetterExportMode.OFF);
        // Unlike the section toggles (fail-open, "never lose content"),
        // a malformed cover letter value must never silently EXPOSE one.
        for (String malformed : new String[]{"", "maybe", "ENABLED", "TRUE", "download", "garbage"}) {
            assertThat(ExportPreferences.fromUserSettings(Map.of("export.coverletter.mode", malformed)).coverLetterMode())
                    .as("value '%s' must resolve OFF", malformed)
                    .isEqualTo(CoverLetterExportMode.OFF);
        }
    }

    @Test
    void coverLetterMode_doesNotAffectSectionTogglesOrMode() {
        Map<String, String> overrides = Map.of(
                "export.coverletter.mode", "SEPARATE",
                "export.section.skills", "false");
        ExportPreferences prefs = ExportPreferences.fromUserSettings(overrides);
        assertThat(prefs.coverLetterMode()).isEqualTo(CoverLetterExportMode.SEPARATE);
        assertThat(prefs.mode()).isEqualTo(ExportMode.FULL);
        assertThat(prefs.skills()).isFalse();
        assertThat(prefs.contact()).isTrue();
    }

    @Test
    void briefMode_andSectionToggles_parse() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("export.mode", "BRIEF");
        overrides.put("export.section.projects", "false");
        overrides.put("export.section.contact", "false");

        ExportPreferences prefs = ExportPreferences.fromUserSettings(overrides);

        assertThat(prefs.mode()).isEqualTo(ExportMode.BRIEF);
        assertThat(prefs.projects()).isFalse();
        assertThat(prefs.contact()).isFalse();
        // untouched sections stay enabled
        assertThat(prefs.experience()).isTrue();
        assertThat(prefs.summary()).isTrue();
    }

    @Test
    void partialSettings_keepDefaultsForMissingNames() {
        Map<String, String> overrides = Map.of("export.section.skills", "false");

        ExportPreferences prefs = ExportPreferences.fromUserSettings(overrides);

        assertThat(prefs.mode()).isEqualTo(ExportMode.FULL); // mode not set
        assertThat(prefs.skills()).isFalse();
        assertThat(prefs.projects()).isTrue(); // not set → default enabled
    }

    @Test
    void malformedMode_fallsBackToFull() {
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.mode", "garbage")).mode())
                .isEqualTo(ExportMode.FULL);
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.mode", "")).mode())
                .isEqualTo(ExportMode.FULL);
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.mode", "compact")).mode())
                .isEqualTo(ExportMode.FULL);
    }

    @Test
    void malformedToggleValues_areTreatedAsEnabled() {
        // Only the exact value "false" (any case) disables — a partially
        // configured or malformed user can never silently lose content.
        for (String malformed : new String[]{"", "maybe", "TRUE", "0", "off", "null"}) {
            assertThat(ExportPreferences.fromUserSettings(Map.of("export.section.experience", malformed)).experience())
                    .as("value '%s' must enable the section", malformed)
                    .isTrue();
        }
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.section.experience", "FALSE")).experience())
                .isFalse();
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.section.experience", " false ")).experience())
                .isFalse();
    }

    @Test
    void modeParsing_isCaseInsensitiveAndTrimmed() {
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.mode", "brief")).mode())
                .isEqualTo(ExportMode.BRIEF);
        assertThat(ExportPreferences.fromUserSettings(Map.of("export.mode", " full ")).mode())
                .isEqualTo(ExportMode.FULL);
    }

    @Test
    void nonExportSettings_areIgnored() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("portfolio.pdf.template", "executive");
        overrides.put("portfolio.skills.show", "false");
        overrides.put("unrelated.setting", "false");

        ExportPreferences prefs = ExportPreferences.fromUserSettings(overrides);

        assertThat(prefs).isEqualTo(ExportPreferences.defaults());
    }
}
