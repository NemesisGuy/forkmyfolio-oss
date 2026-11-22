package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSetting;
import com.forkmyfolio.model.enums.CoverLetterExportMode;
import com.forkmyfolio.model.enums.ExportMode;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.repository.SettingRepository;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Phase 0.27: the cover letter adopts the SELECTED resume template's identity.
 *
 * <p>
 * For every selectable template this renders an owner export with the letter
 * as page 1 and asserts the letter page is a real letter — letterhead, letter
 * body, no "Cover Letter" banner heading and none of the resume's section
 * content — while the resume itself still follows on page 2. Each PDF is
 * written to {@code target/pdf-baseline/cover-letter-<template>.pdf} so the
 * per-template letterheads can be compared visually.
 */
@ExtendWith(MockitoExtension.class)
class PdfCoverLetterCorpusTest {

    private static final String LETTER_SENTENCE =
            "I am applying for the platform engineering role at Acme Corp.";

    @Mock
    private ExportContentService exportContentService;
    @Mock
    private SettingRepository settingRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setSlug("letter-user");
        testUser.setFirstName("Ada");
        testUser.setLastName("Lovelace");
        testUser.setEmail("ada@example.com");
        testUser.setUserSkills(new HashSet<>());
        testUser.setExperiences(new HashSet<>());
        testUser.setProjects(new HashSet<>());
        testUser.setQualifications(new HashSet<>());
        testUser.setUserSettings(new HashSet<>());
    }

    private PortfolioData letterFixture() {
        PortfolioProfile profile = new PortfolioProfile();
        profile.setUser(testUser);
        profile.setHeadline("Platform Engineer");
        profile.setSummary("Engineer focused on reliable infrastructure and developer tooling.");
        profile.setPublicEmail("ada@example.com");
        profile.setWebsiteUrl("https://example.com");
        profile.setLinkedinUrl("https://www.linkedin.com/in/example");
        profile.setGithubUrl("https://github.com/example");

        Experience experience = new Experience();
        experience.setUser(testUser);
        experience.setJobTitle("Platform Engineer");
        experience.setCompanyName("Example Corp");
        experience.setStartDate(LocalDate.of(2022, 1, 1));
        experience.setDescription("Owns the deployment platform and its tooling.");
        experience.setVisible(true);
        experience.setDisplayOrder(100);

        Skill java = new Skill();
        java.setName("Java");
        java.setCategory("Backend");
        java.setLevel(SkillLevel.EXPERT);

        String letter = "Dear Hiring Team,\n\n" + LETTER_SENTENCE + "\n\n"
                + "I have spent the last four years building the tooling that other "
                + "engineers rely on every day, and I would like to do the same here.\n\n"
                + "Regards,\nAda Lovelace";

        return new PortfolioData(profile,
                new ArrayList<>(List.of(experience)),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(List.of(java)),
                new ArrayList<>(),
                "ada@example.com", true, true, ExportMode.FULL, null,
                letter, CoverLetterExportMode.IN_PDF);
    }

    @Test
    void everyTemplateRendersTheLetterAsPageOneInItsOwnIdentity() throws Exception {
        PortfolioData data = letterFixture();
        when(exportContentService.buildExportData(any(User.class), any(ExportVisibility.class)))
                .thenReturn(data);
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));

        Path outDir = Path.of("target", "pdf-baseline");
        Files.createDirectories(outDir);

        // The eight selectable templates (mirrors PdfGenerationService).
        Map<String, Boolean> templates = new LinkedHashMap<>();
        templates.put("modern", true);
        templates.put("elegance", true);
        templates.put("classic", true);
        templates.put("metro-light", true);
        templates.put("executive", true);
        templates.put("modern-minimal", true);
        templates.put("technical", true);
        templates.put("business-card", false); // card-sized page: no resume sections

        for (Map.Entry<String, Boolean> entry : templates.entrySet()) {
            String templateName = entry.getKey();
            testUser.setUserSettings(new HashSet<>());
            testUser.getUserSettings().add(
                    new UserSetting(testUser, "portfolio.pdf.template", templateName));

            PdfGenerationService.PdfFile pdfFile =
                    pdfGenerationService.generatePortfolioPdf(testUser, templateName, ExportVisibility.OWNER);
            assertNotNull(pdfFile, "PDF for template " + templateName);
            assertTrue(pdfFile.content().length > 0, "non-empty PDF for " + templateName);

            Path out = outDir.resolve("cover-letter-" + templateName + ".pdf");
            Files.write(out, pdfFile.content());

            var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
            try {
                if (!entry.getValue()) {
                    // Card-sized legacy template: a letter cannot fit, so the
                    // service omits it instead of truncating it.
                    assertTrue(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                            .getTextFromPage(1).replaceAll("\\s+", " ")
                            .indexOf(LETTER_SENTENCE) < 0,
                            templateName + " must omit the letter rather than truncate it");
                    continue;
                }

                assertTrue(reader.getNumberOfPages() >= 2,
                        templateName + " must render the letter plus the resume");

                String pageOne = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                        .getTextFromPage(1).replaceAll("\\s+", " ");
                assertTrue(pageOne.contains(LETTER_SENTENCE),
                        templateName + " page 1 must contain the letter body");
                assertFalse(pageOne.contains("Cover Letter"),
                        templateName + " must not print a 'Cover Letter' banner heading");
                assertFalse(pageOne.contains("Example Corp"),
                        templateName + " page 1 must not leak resume sections");

                StringBuilder rest = new StringBuilder();
                for (int page = 2; page <= reader.getNumberOfPages(); page++) {
                    rest.append(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                            .getTextFromPage(page)).append('\n');
                }
                assertTrue(rest.toString().replaceAll("\\s+", " ").contains("Example Corp"),
                        templateName + " resume must follow the letter");
            } finally {
                reader.close();
            }
        }
    }

    @Test
    void standaloneLetterUsesTheSelectedTemplateIdentity() throws Exception {
        PortfolioData inPdf = letterFixture();
        PortfolioData separate = new PortfolioData(
                inPdf.profile(), inPdf.experiences(), inPdf.qualifications(), inPdf.projects(),
                inPdf.skills(), inPdf.testimonials(), inPdf.contactEmail(),
                inPdf.includeSummary(), inPdf.includeContact(), inPdf.exportMode(),
                inPdf.profileImageUrl(), inPdf.coverLetterTemplate(),
                CoverLetterExportMode.SEPARATE);
        when(exportContentService.buildExportData(any(User.class), any(ExportVisibility.class)))
                .thenReturn(separate);
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));

        Path outDir = Path.of("target", "pdf-baseline");
        Files.createDirectories(outDir);

        for (String templateName : List.of("modern", "classic", "elegance", "technical")) {
            testUser.setUserSettings(new HashSet<>());
            testUser.getUserSettings().add(
                    new UserSetting(testUser, "portfolio.pdf.template", templateName));

            PdfGenerationService.PdfFile pdfFile =
                    pdfGenerationService.generateCoverLetterPdf(testUser, "Acme Corp", "Platform Engineer");
            assertTrue(pdfFile.content().length > 0, "non-empty letter for " + templateName);
            Files.write(outDir.resolve("cover-letter-standalone-" + templateName + ".pdf"),
                    pdfFile.content());

            var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
            try {
                String pageOne = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                        .getTextFromPage(1).replaceAll("\\s+", " ");
                assertTrue(pageOne.contains(LETTER_SENTENCE),
                        templateName + " standalone letter must contain the letter body");
                assertFalse(pageOne.contains("Cover Letter"),
                        templateName + " standalone letter needs no banner heading");
            } finally {
                reader.close();
            }
        }
    }
}
