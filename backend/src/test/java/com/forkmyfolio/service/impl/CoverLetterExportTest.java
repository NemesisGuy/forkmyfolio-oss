package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.CoverLetterExportMode;
import com.forkmyfolio.model.enums.ExportMode;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.repository.SettingRepository;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Phase 0.19: unit tests for the cover letter export feature.
 *
 * Pins the merge behaviour of the Markdown Cover Letter section and the
 * owner gate of the standalone PDF download:
 * <ul>
 *   <li>IN_PDF → the Markdown gains a "Cover Letter" section; PUBLIC
 *       exports never carry the section (mode OFF via the selection
 *       engine's defaults).</li>
 *   <li>OFF / SEPARATE → the Markdown never contains the section
 *       (SEPARATE is a standalone file only, never inlined).</li>
 *   <li>the standalone {@code generateCoverLetterPdf} answers
 *       {@link ResourceNotFoundException} unless mode == SEPARATE and text
 *       is present (fail-closed, indistinguishable 404).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CoverLetterExportTest {

    @Mock
    private ExportContentService exportContentService;
    @Mock
    private com.forkmyfolio.repository.SettingRepository settingRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private MarkdownGenerationService markdownGenerationService;

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setSlug("test-user");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
    }

    private PortfolioData data(CoverLetterExportMode mode, String text) {
        PortfolioProfile profile = new PortfolioProfile();
        profile.setUser(testUser);
        profile.setHeadline("Software Engineer");
        profile.setSummary("Experienced developer.");
        return new PortfolioData(profile,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "owner@example.com",
                true, true, ExportMode.FULL, null,
                text, mode);
    }

    private String markdownFor(PortfolioData data) {
        when(exportContentService.buildExportData(any(User.class), any(ExportVisibility.class)))
                .thenReturn(data);
        return new String(
                markdownGenerationService.generatePortfolioMarkdown(testUser, ExportVisibility.OWNER)
                        .content(),
                StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // Markdown Cover Letter section (IN_PDF)
    // ------------------------------------------------------------------

    @Test
    void markdown_containsCoverLetterSection_whenModeIsInPdfAndTextPresent() {
        String md = markdownFor(data(CoverLetterExportMode.IN_PDF,
                "Dear {{company}},\n\nI am applying for the {{role}} role.\n\nRegards,\nTest"));

        assertThat(md).contains("## ✉️ Cover Letter");
        assertThat(md).contains("Dear {{company}},");
        assertThat(md).contains("I am applying for the {{role}} role.");
    }

    @Test
    void markdown_omitsCoverLetterSection_whenModeIsOff() {
        String md = markdownFor(data(CoverLetterExportMode.OFF, "Some perfectly good letter text."));
        assertThat(md).doesNotContain("Cover Letter");
    }

    @Test
    void markdown_omitsCoverLetterSection_whenModeIsSeparate_standaloneOnly() {
        // SEPARATE means its own download — the resume formats stay untouched.
        String md = markdownFor(data(CoverLetterExportMode.SEPARATE, "Letter text."));
        assertThat(md).doesNotContain("Cover Letter");
    }

    @Test
    void markdown_omitsCoverLetterSection_whenModeInPdfButTextBlank() {
        String md = markdownFor(data(CoverLetterExportMode.IN_PDF, ""));
        assertThat(md).doesNotContain("Cover Letter");
    }

    // ------------------------------------------------------------------
    // Standalone cover letter PDF (SEPARATE)
    // ------------------------------------------------------------------

    @Test
    void generateCoverLetterPdf_producesFile_whenModeSeparateAndTextPresent() {
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
        when(exportContentService.buildExportData(any(User.class), eq(ExportVisibility.OWNER)))
                .thenReturn(data(CoverLetterExportMode.SEPARATE, "Dear Hiring Team,\n\nPlease consider me."));

        PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generateCoverLetterPdf(testUser,
                "Acme Corp", "Backend Engineer");

        assertThat(pdfFile).isNotNull();
        assertThat(pdfFile.content()).isNotEmpty();
        // Very low-tech PDF sanity check: a valid OpenPDF stream header.
        assertThat(new String(pdfFile.content(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdfFile.suggestedFilename()).contains("CoverLetter");
        assertThat(pdfFile.suggestedFilename()).endsWith(".pdf");
    }

    @Test
    void generateCoverLetterPdf_throwsNotFound_whenModeIsOff() {
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
        when(exportContentService.buildExportData(any(User.class), eq(ExportVisibility.OWNER)))
                .thenReturn(data(CoverLetterExportMode.OFF, "Letter text."));

        assertThatThrownBy(() -> pdfGenerationService.generateCoverLetterPdf(testUser, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateCoverLetterPdf_throwsNotFound_whenModeIsInPdf_notSeparate() {
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
        when(exportContentService.buildExportData(any(User.class), eq(ExportVisibility.OWNER)))
                .thenReturn(data(CoverLetterExportMode.IN_PDF, "Letter text."));

        // The standalone download is strictly the SEPARATE mode's surface.
        assertThatThrownBy(() -> pdfGenerationService.generateCoverLetterPdf(testUser, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateCoverLetterPdf_throwsNotFound_whenTextBlank() {
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
        when(exportContentService.buildExportData(any(User.class), eq(ExportVisibility.OWNER)))
                .thenReturn(data(CoverLetterExportMode.SEPARATE, ""));

        assertThatThrownBy(() -> pdfGenerationService.generateCoverLetterPdf(testUser, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateCoverLetterPdf_throwsNotFound_whenUserUnknown() {
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfGenerationService.generateCoverLetterPdf(testUser, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        // The selection engine must never run for an unknown owner.
        org.mockito.Mockito.verifyNoInteractions(exportContentService);
    }
}
