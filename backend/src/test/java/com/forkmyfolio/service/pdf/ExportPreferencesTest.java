package com.forkmyfolio.service.pdf;

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
