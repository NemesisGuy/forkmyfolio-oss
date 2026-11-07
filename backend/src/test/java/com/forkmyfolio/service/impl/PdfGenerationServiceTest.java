package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.*;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfGenerationServiceTest {

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
                testUser.setSlug("test-user");
                testUser.setFirstName("Test");
                testUser.setLastName("User");
                testUser.setEmail("test@example.com");
                testUser.setUserSkills(new HashSet<>());
                testUser.setExperiences(new HashSet<>());
                testUser.setProjects(new HashSet<>());
                testUser.setQualifications(new HashSet<>());
                testUser.setUserSettings(new HashSet<>());
        }

        /**
         * Phase 8 (PRV-1): the generation service no longer assembles content
         * itself — the shared {@link ExportContentService} owns selection and
         * is mocked here; these tests pin RENDERING behaviour only.
         */
        private void stubExportData(PortfolioData data) {
                when(exportContentService.buildExportData(any(User.class), any(ExportVisibility.class)))
                                .thenReturn(data);
        }

        private PortfolioData dataFor(PortfolioProfile profile, List<Experience> experiences,
                        List<Project> projects, List<Skill> skills) {
                // Mutable lists: templates sort these in place.
                return new PortfolioData(profile,
                                new ArrayList<>(experiences),
                                new ArrayList<>(),
                                new ArrayList<>(projects),
                                new ArrayList<>(skills));
        }

        @Test
        void generatePortfolioPdf_shouldGeneratePdfWithValidData() {
                // Arrange
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Software Engineer");
                profile.setSummary("Experienced developer.");

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList()));

                // Act
                PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser, "modern",
                                ExportVisibility.PUBLIC);

                // Assert
                assertNotNull(pdfFile);
                assertNotNull(pdfFile.content());
                assertTrue(pdfFile.content().length > 0);
                assertTrue(pdfFile.suggestedFilename().endsWith(".pdf"));
        }

        @Test
        void generatePortfolioPdf_shouldFallbackToDefaultTemplateIfNotFound() {
                // Arrange
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Software Engineer");
                profile.setSummary("Experienced developer.");

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList()));

                // Act
                PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                "non-existent-template", ExportVisibility.PUBLIC);

                // Assert
                assertNotNull(pdfFile);
                assertTrue(pdfFile.content().length > 0);
        }

        /**
         * Phase 7.4 §7/§8 regression: the Markdown export has always rendered one
         * bullet per achievement line, but every PDF template dropped the
         * achievements field entirely. Templates must now render them.
         */
        @Test
        void generatePortfolioPdf_rendersExperienceAchievements_acrossTemplates() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Software Engineer");
                profile.setSummary("Experienced developer.");

                Experience exp = new Experience();
                exp.setUser(testUser);
                exp.setJobTitle("Engineer");
                exp.setCompanyName("Acme");
                exp.setStartDate(java.time.LocalDate.of(2021, 2, 1));
                exp.setEndDate(java.time.LocalDate.of(2023, 2, 1));
                exp.setDescription("Did engineering things.");
                exp.setAchievements("Shipped the fuse reducer\nCut build times 90%");
                exp.setVisible(true);
                exp.setDisplayOrder(100);

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));

                Project proj = new Project();
                proj.setUser(testUser);
                proj.setTitle("Feature-rich Project");
                proj.setDescription("Builds on structured key features.");
                proj.setVisible(true);
                proj.setDisplayOrder(200);
                com.forkmyfolio.model.KeyFeature kf1 = new com.forkmyfolio.model.KeyFeature();
                kf1.setValue("Structured export parity");
                kf1.setDisplayOrder(0);
                kf1.setProject(proj);
                com.forkmyfolio.model.KeyFeature kf2 = new com.forkmyfolio.model.KeyFeature();
                kf2.setValue("Ordered bullets");
                kf2.setDisplayOrder(1);
                kf2.setProject(proj);
                proj.setKeyFeatures(new ArrayList<>(List.of(kf1, kf2)));

                // Phase 7.4 (§8): seed visible user skills (level lives on the
                // UserSkill row; Modern sorts by it -> must not be null)
                Skill vue = new Skill();
                vue.setName("Vue");
                vue.setCategory("Frontend");
                vue.setLevel(SkillLevel.ADVANCED); // Modern template sorts by level — must not be null
                Skill postgres = new Skill();
                postgres.setName("Postgres"); // uncategorised -> "Other Skills" group
                postgres.setLevel(SkillLevel.INTERMEDIATE);
                UserSkill usVue = new UserSkill();
                usVue.setSkill(vue);
                usVue.setLevel(SkillLevel.ADVANCED);
                usVue.setVisible(true);
                usVue.setUuid(UUID.randomUUID()); // @EqualsAndHashCode(of = "uuid") — Set.of needs distinct instances
                UserSkill usPostgres = new UserSkill();
                usPostgres.setSkill(postgres);
                usPostgres.setLevel(SkillLevel.INTERMEDIATE);
                usPostgres.setVisible(true);
                usPostgres.setUuid(UUID.randomUUID());
                testUser.setUserSkills(new HashSet<>(Set.of(usVue, usPostgres)));

                stubExportData(dataFor(profile, List.of(exp), List.of(proj), List.of(vue, postgres)));

                for (String templateName : new String[] { "modern", "classic", "elegance", "metro-dark" }) {
                        // The service resolves the template from the user's
                        // settings (the templateName argument is ignored), so
                        // each iteration must point the setting at that template.
                        testUser.setUserSettings(new HashSet<>());
                        testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", templateName));

                        PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                        templateName, ExportVisibility.PUBLIC);
                        assertNotNull(pdfFile, "PDF for template " + templateName);

                        // Extract text via OpenPDF to verify the achievements land in the PDF content
                        StringBuilder extracted = new StringBuilder();
                        var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
                        try {
                                for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                                        extracted.append(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                        .getTextFromPage(i))
                                                        .append('\n');
                                }
                        } finally {
                                reader.close();
                        }
                        String text = extracted.toString();
                        assertTrue(text.contains("Key Achievements"),
                                        templateName + " template must render the achievements heading");
                        assertTrue(text.contains("Shipped the fuse reducer"),
                                        templateName + " template must render the first achievement line");
                        assertTrue(text.contains("Cut build times 90%"),
                                        templateName + " template must render the second achievement line");
                        // Phase 7.4 (§6): structured key features parity
                        assertTrue(text.contains("Key Features"),
                                        templateName + " template must render the key features heading");
                        assertTrue(text.contains("Structured export parity"),
                                        templateName + " template must render the first key feature");
                        assertTrue(text.contains("Ordered bullets"),
                                        templateName + " template must render the second key feature");
                        // Phase 7.4 (§8): skills parity — names in every full template
                        assertTrue(text.contains("Vue"),
                                        templateName + " template must render the skill name");
                        assertTrue(text.contains("Postgres"),
                                        templateName + " template must render the uncategorised skill name");
                        if ("classic".equals(templateName) || "metro-dark".equals(templateName)) {
                                // §8 fix: these two templates now render grouped skills
                                assertTrue(text.contains("Frontend"),
                                                templateName + " template must render the skill category group");
                                assertTrue(text.contains("Other Skills"),
                                                templateName + " template must pool uncategorised skills");
                        }
                }
        }

        /**
         * Phase 8.5 (DEF-4): the legacy templates (modern/elegance/classic)
         * remained selectable but silently omitted the Testimonials section
         * that 8.3 added to the Markdown export and the 8.2 templates. All
         * three must render it now, honouring the same semantics as
         * {@code TemplateSupport.renderTestimonialsSection}: blank quotes
         * skip, BRIEF caps at three, and empty testimonial lists render no
         * section at all.
         */
        @Test
        void generatePortfolioPdf_rendersTestimonials_acrossLegacyTemplates() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Software Engineer");

                Testimonial t1 = new Testimonial();
                t1.setQuote("Outstanding engineer and mentor.");
                t1.setAuthorName("Ada Lovelace");
                t1.setAuthorTitle("CTO, Analytical Engines");
                Testimonial t2 = new Testimonial();
                t2.setQuote("Delivered beyond expectations.");
                t2.setAuthorName("Grace Hopper");
                t2.setAuthorTitle("Rear Admiral");
                Testimonial t3 = new Testimonial();
                t3.setQuote("A rare talent for system design.");
                t3.setAuthorName("Alan Turing");
                Testimonial t4 = new Testimonial();
                t4.setQuote("Fourth testimonial must not appear in BRIEF mode.");
                t4.setAuthorName("Edsger Dijkstra");
                Testimonial t5 = new Testimonial();
                t5.setQuote("Fifth testimonial must not appear in BRIEF mode.");
                t5.setAuthorName("Donald Knuth");
                Testimonial blank = new Testimonial();
                blank.setQuote("   ");
                blank.setAuthorName("Blank Quote");

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));

                for (String templateName : new String[] { "modern", "classic", "elegance" }) {
                        for (boolean brief : new boolean[] { false, true }) {
                                testUser.setUserSettings(new HashSet<>());
                                testUser.getUserSettings()
                                                .add(new UserSetting(testUser, "portfolio.pdf.template", templateName));

                                stubExportData(new PortfolioData(profile,
                                                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                                                new ArrayList<>(),
                                                new ArrayList<>(List.of(t1, blank, t2, t3, t4, t5)),
                                                "test@example.com", true, true,
                                                brief ? com.forkmyfolio.model.enums.ExportMode.BRIEF
                                                                : com.forkmyfolio.model.enums.ExportMode.FULL,
                                                null));

                                PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(
                                                testUser, templateName, ExportVisibility.OWNER);
                                assertNotNull(pdfFile, "PDF for " + templateName + "/" + (brief ? "BRIEF" : "FULL"));

                                StringBuilder extracted = new StringBuilder();
                                var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
                                try {
                                        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                                                extracted.append(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                                .getTextFromPage(i))
                                                                .append('\n');
                                        }
                                } finally {
                                        reader.close();
                                }
                                String text = extracted.toString();
                                String mode = templateName + "/" + (brief ? "BRIEF" : "FULL");
                                assertTrue(text.contains("TESTIMONIALS"),
                                                mode + " template must render the Testimonials heading");
                                assertTrue(text.contains("Outstanding engineer and mentor."),
                                                mode + " must render the first testimonial quote");
                                assertTrue(text.contains("Ada Lovelace"),
                                                mode + " must render the first testimonial author");
                                // Blank quotes never render (even when inside the BRIEF window).
                                assertFalse(text.contains("Blank Quote"),
                                                mode + " must skip blank-quote testimonials");
                                if (brief) {
                                        // BRIEF caps at three SELECTED entries (the blank one
                                        // consumes a slot, exactly like TemplateSupport), so
                                        // only the first two valid quotes render.
                                        assertTrue(text.contains("Delivered beyond expectations."),
                                                        mode + " must render the second testimonial quote");
                                        assertFalse(text.contains("A rare talent for system design."),
                                                        mode + " must cap testimonials at three");
                                        assertFalse(text.contains("Fourth testimonial"),
                                                        mode + " must cap testimonials at three");
                                        assertFalse(text.contains("Fifth testimonial"),
                                                        mode + " must cap testimonials at three");
                                } else {
                                        assertTrue(text.contains("Delivered beyond expectations."),
                                                        mode + " must render the second testimonial quote");
                                        assertTrue(text.contains("A rare talent for system design."),
                                                        mode + " must render the third testimonial quote");
                                        assertTrue(text.contains("Fourth testimonial"),
                                                        mode + " renders every testimonial");
                                }
                        }
                }
        }

        /**
         * Phase 7.4 §9: every template must survive a MINIMAL portfolio —
         * no experiences, projects, qualifications or skills, and only an
         * email in the contact fields (Classic used to NPE here via
         * String.join over null LinkedIn/GitHub). Pins 'empty data never
         * breaks an export' for all 4 full templates + business card.
         */
        @Test
        void generatePortfolioPdf_minimalPortfolio_isEdgeSafeAcrossTemplates() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Minimal Headline");
                profile.setSummary("Minimal summary.");
                profile.setPublicEmail("minimal@example.com");
                // linkedin/github/website intentionally NULL — reproduces the
                // ClassicTemplate String.join NPE

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList()));

                for (String templateName : new String[] { "modern", "classic", "elegance", "metro-dark",
                                "business-card" }) {
                        testUser.setUserSettings(new HashSet<>());
                        testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", templateName));

                        PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                        templateName, ExportVisibility.PUBLIC);
                        assertNotNull(pdfFile, "PDF for template " + templateName);
                        assertTrue(pdfFile.content().length > 0, "non-empty PDF for " + templateName);

                        var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
                        try {
                                assertTrue(reader.getNumberOfPages() >= 1,
                                                templateName + " must render at least one page");
                                // The rendered owner name must appear on page 1 —
                                // guarantees a non-degenerate, human-visible document.
                                String firstPage = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                .getTextFromPage(1);
                                String firstPageUpper = firstPage.toUpperCase();
                                // Elegance upper-cases the name — compare case-insensitively for all
                                assertTrue(firstPageUpper.contains("TEST USER"),
                                                templateName + " must render the user name on page 1");
                                assertTrue(firstPage.contains("Minimal Headline"),
                                                templateName + " must render the headline on page 1");
                        } finally {
                                reader.close();
                        }
                }
        }

        /**
         * Phase 7.4 §9: automated rendering checks — a very long single
         * description must flow onto multiple pages without exceptions and
         * must never produce a BLANK page (iText layout runaways manifest as
         * content that silently disappears past a page break).
         * BusinessCard excluded: fixed card layout renders only short fields.
         */
        @Test
        void generatePortfolioPdf_longDescription_noBlankPagesAcrossTemplates() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Overflow Headline");
                profile.setSummary("A long flowing summary that repeats. ".repeat(40));
                profile.setPublicEmail("overflow@example.com");

                Experience exp = new Experience();
                exp.setUser(testUser);
                exp.setJobTitle("Long Writer");
                exp.setCompanyName("Overflow Corp");
                exp.setStartDate(java.time.LocalDate.of(2020, 1, 1));
                exp.setDescription("Dense engineering diary entry with plenty of words and wrapping. ".repeat(80));
                exp.setAchievements("Achievement one\nAchievement two");
                exp.setVisible(true);
                exp.setDisplayOrder(100);

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, List.of(exp), Collections.emptyList(), Collections.emptyList()));

                for (String templateName : new String[] { "modern", "classic", "elegance", "metro-dark" }) {
                        testUser.setUserSettings(new HashSet<>());
                        testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", templateName));

                        PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                        templateName, ExportVisibility.PUBLIC);
                        assertNotNull(pdfFile, "PDF for template " + templateName);

                        var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
                        try {
                                int pages = reader.getNumberOfPages();
                                assertTrue(pages >= 2,
                                                templateName + " should flow to more than one page for overflowing content");
                                for (int i = 1; i <= pages; i++) {
                                        String pageText = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                        .getTextFromPage(i);
                                        assertTrue(pageText.trim().length() > 0,
                                                        templateName + " page " + i + " must not be blank");
                                }
                                // The overflow content must actually APPEAR in the
                                // document (not silently clipped at the first break)
                                StringBuilder full = new StringBuilder();
                                for (int i = 1; i <= pages; i++) {
                                        full.append(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                        .getTextFromPage(i)).append('\n');
                                }
                                assertTrue(full.toString().contains("Long Writer"),
                                                templateName + " must render the overflowing experience title");
                        } finally {
                                reader.close();
                        }
                }
        }

        // ==================================================================
        // Phase 8 (8.2): the four NEW TemplateSupport-based templates
        // (metro-light, executive, modern-minimal, technical) + the metro-dark
        // retirement/alias contract.
        // ==================================================================

        /**
         * 8.2: the selectable registry exposes the four new template keys and
         * NO LONGER exposes the retired "metro-dark" key (the public
         * settings endpoint serves exactly this list).
         */
        @Test
        void registry_exposesNewKeys_andRetiresMetroDark() {
                List<String> names = pdfGenerationService.getAvailableTemplateNames();
                org.junit.jupiter.api.Assertions.assertTrue(names.containsAll(List.of(
                                "metro-light", "executive", "modern-minimal", "technical")),
                                "new 8.2 template keys must be selectable");
                org.junit.jupiter.api.Assertions.assertFalse(names.contains("metro-dark"),
                                "retired metro-dark key must not be selectable");
        }

        /**
         * 8.2: a saved user preference of the retired "metro-dark" key must
         * transparently alias to "metro-light" — never a hard failure and
         * never a silent fallback to "modern". Distinguished from the legacy
         * templates by the page-number block only the TemplateSupport
         * templates render.
         */
        @Test
        void legacyMetroDarkPreference_aliasesToMetroLight() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Alias Headline");
                profile.setSummary("Alias summary.");
                profile.setPublicEmail("alias@example.com");

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList()));

                testUser.setUserSettings(new HashSet<>());
                testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", "metro-dark"));

                PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                "metro-dark", ExportVisibility.PUBLIC);
                assertNotNull(pdfFile);

                String page1 = extractPageText(pdfFile, 1);
                assertTrue(page1.contains("Page 1"),
                                "metro-dark preference must render through metro-light (page-number block present)");
                assertTrue(page1.toUpperCase().contains("TEST USER"));
        }

        /**
         * 8.2: every new template must survive a MINIMAL portfolio (no
         * experiences, projects, qualifications or skills; only an email) —
         * empty optional sections skip cleanly, name + headline render on
         * page 1.
         */
        @Test
        void newTemplates_minimalPortfolio_isEdgeSafe() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Minimal Headline");
                profile.setSummary("Minimal summary.");
                profile.setPublicEmail("minimal@example.com");

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList()));

                for (String templateName : new String[] { "metro-light", "executive", "modern-minimal",
                                "technical" }) {
                        testUser.setUserSettings(new HashSet<>());
                        testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", templateName));

                        PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                        templateName, ExportVisibility.PUBLIC);
                        assertNotNull(pdfFile, "PDF for template " + templateName);
                        assertTrue(pdfFile.content().length > 0, "non-empty PDF for " + templateName);

                        var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
                        try {
                                assertTrue(reader.getNumberOfPages() >= 1,
                                                templateName + " must render at least one page");
                                String firstPageUpper = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                .getTextFromPage(1).toUpperCase();
                                assertTrue(firstPageUpper.contains("TEST USER"),
                                                templateName + " must render the user name on page 1");
                                assertTrue(firstPageUpper.contains("MINIMAL HEADLINE"),
                                                templateName + " must render the headline on page 1");
                                assertTrue(firstPageUpper.contains("MINIMAL@EXAMPLE.COM"),
                                                templateName + " must render the public contact email");
                        } finally {
                                reader.close();
                        }
                }
        }

        /**
         * 8.2: every new template must paginate overflowing content across
         * pages without blank pages, and stamp page numbers (bottom-centre)
         * on every page — including page 2+.
         */
        @Test
        void newTemplates_longDescription_paginatesWithPageNumbers() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Overflow Headline");
                profile.setSummary("A long flowing summary that repeats. ".repeat(40));
                profile.setPublicEmail("overflow@example.com");

                Experience exp = new Experience();
                exp.setUser(testUser);
                exp.setJobTitle("Long Writer");
                exp.setCompanyName("Overflow Corp");
                exp.setStartDate(java.time.LocalDate.of(2020, 1, 1));
                exp.setDescription("Dense engineering diary entry with plenty of words and wrapping. ".repeat(80));
                exp.setAchievements("Achievement one\nAchievement two");
                exp.setVisible(true);
                exp.setDisplayOrder(100);

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, List.of(exp), Collections.emptyList(), Collections.emptyList()));

                for (String templateName : new String[] { "metro-light", "executive", "modern-minimal",
                                "technical" }) {
                        testUser.setUserSettings(new HashSet<>());
                        testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", templateName));

                        PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                        templateName, ExportVisibility.PUBLIC);
                        assertNotNull(pdfFile, "PDF for template " + templateName);

                        var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
                        try {
                                int pages = reader.getNumberOfPages();
                                assertTrue(pages >= 2,
                                                templateName + " should flow to more than one page");
                                for (int i = 1; i <= pages; i++) {
                                        String pageText = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                        .getTextFromPage(i);
                                        assertTrue(pageText.trim().length() > 0,
                                                        templateName + " page " + i + " must not be blank");
                                        assertTrue(pageText.contains("Page " + i),
                                                        templateName + " page " + i + " must carry its page number");
                                }
                                StringBuilder full = new StringBuilder();
                                for (int i = 1; i <= pages; i++) {
                                        full.append(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                        .getTextFromPage(i)).append('\n');
                                }
                                assertTrue(full.toString().contains("Long Writer"),
                                                templateName + " must render the overflowing experience title");
                                assertTrue(full.toString().contains("Achievement one"),
                                                templateName + " must render achievement bullets on the overflow data");
                        } finally {
                                reader.close();
                        }
                }
        }

        /**
         * 8.2: the Technical template's skills-first emphasis — the Skills
         * section must appear BEFORE the Experience section in the rendered
         * page flow (page 1 when both sections exist).
         */
        @Test
        void technicalTemplate_rendersSkillsBeforeExperience() throws Exception {
                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(testUser);
                profile.setHeadline("Engineer");
                profile.setSummary("Summary.");
                profile.setPublicEmail("tech@example.com");

                Experience exp = new Experience();
                exp.setUser(testUser);
                exp.setJobTitle("Backend Engineer");
                exp.setCompanyName("TechCo");
                exp.setStartDate(java.time.LocalDate.of(2021, 1, 1));
                exp.setDescription("Built services.");
                exp.setVisible(true);
                exp.setDisplayOrder(100);

                Skill java = new Skill();
                java.setName("Java");
                java.setCategory("Backend");
                java.setLevel(SkillLevel.ADVANCED);

                when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
                stubExportData(dataFor(profile, List.of(exp), Collections.emptyList(), List.of(java)));

                testUser.setUserSettings(new HashSet<>());
                testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", "technical"));

                PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(testUser,
                                "technical", ExportVisibility.PUBLIC);
                String page1 = extractPageText(pdfFile, 1);

                int skillsAt = page1.indexOf("SKILLS");
                int experienceAt = page1.indexOf("EXPERIENCE");
                assertTrue(skillsAt >= 0, "technical template must render a Skills section");
                assertTrue(experienceAt >= 0, "technical template must render an Experience section");
                assertTrue(skillsAt < experienceAt,
                                "technical template must render Skills BEFORE Experience (skills-first emphasis)");
        }

        /** Extracts the text of one page from a generated PDF (test helper). */
        private String extractPageText(PdfGenerationService.PdfFile pdfFile, int pageNumber) throws Exception {
                var reader = new org.openpdf.text.pdf.PdfReader(pdfFile.content());
                try {
                        return new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                        .getTextFromPage(pageNumber);
                } finally {
                        reader.close();
                }
        }
}
