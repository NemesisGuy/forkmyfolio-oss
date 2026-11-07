package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.*;
import com.forkmyfolio.model.enums.ExportMode;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.repository.SettingRepository;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.pdf.PortfolioData;
import com.forkmyfolio.service.pdf.templates.PortfolioPdfTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Phase 0.12 (PDF library migration): generates the PDF corpus used for
 * before/after comparison of the iText 7 → OpenPDF migration.
 *
 * <p>
 * Each run writes one PDF per template (the eight selectable templates plus
 * the retired-but-archived {@code metro-dark}) to {@code target/pdf-baseline/}
 * using a fixed, rich fixture. The corpus produced by the pre-migration iText
 * build is the visual/layout baseline; the same test re-run after the
 * migration produces the comparison corpus.
 *
 * <p>
 * The fixture exercises every section a template can render: name, headline,
 * summary, full contact row (email • website • linkedin • github), two
 * experiences (one open-ended → "Present") with multi-line achievements, two
 * projects (linked live/repo URLs, tech stack, structured key features),
 * skills across two categories plus an uncategorised skill, qualifications,
 * and testimonials. Deterministic data only — no clocks, no random values.
 */
@ExtendWith(MockitoExtension.class)
class PdfBaselineCorpusTest {

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

    private void stubExportData(PortfolioData data) {
        when(exportContentService.buildExportData(any(User.class), any(ExportVisibility.class)))
                .thenReturn(data);
        when(userService.findBySlugWithAllPortfolioData(anyString())).thenReturn(Optional.of(testUser));
    }

    /**
     * The fixed rich fixture shared by every template in the corpus.
     */
    private PortfolioData richFixture() {
        PortfolioProfile profile = new PortfolioProfile();
        profile.setUser(testUser);
        profile.setHeadline("Full-Stack Engineer & Systems Designer");
        profile.setSummary("Engineer with a decade of experience across backend systems, "
                + "developer tooling and technical leadership. Writes about architecture, "
                + "builds open source, and mentors growing teams.");
        profile.setPublicEmail("test@example.com");
        profile.setWebsiteUrl("https://example.com");
        profile.setLinkedinUrl("https://www.linkedin.com/in/example");
        profile.setGithubUrl("https://github.com/example");

        Experience exp1 = new Experience();
        exp1.setUser(testUser);
        exp1.setJobTitle("Senior Software Engineer");
        exp1.setCompanyName("Example Corp");
        exp1.setLocation("Remote");
        exp1.setStartDate(LocalDate.of(2021, 2, 1));
        exp1.setEndDate(null); // renders as "Present"
        exp1.setDescription("Leads the platform team responsible for the core "
                + "deployment pipeline, developer experience tooling and internal "
                + "service standards used by forty engineers.");
        exp1.setAchievements("Shipped the deployment pipeline used by every product team\n"
                + "Cut average build times from 40 minutes to under 4\n"
                + "Mentored six engineers through promotion");
        exp1.setVisible(true);
        exp1.setDisplayOrder(100);

        Experience exp2 = new Experience();
        exp2.setUser(testUser);
        exp2.setJobTitle("Software Engineer");
        exp2.setCompanyName("StartupCo");
        exp2.setStartDate(LocalDate.of(2018, 6, 1));
        exp2.setEndDate(LocalDate.of(2021, 1, 1));
        exp2.setDescription("Built the first customer-facing dashboard and the billing "
                + "integration that took the company from seed to Series A.");
        exp2.setAchievements("Delivered the billing integration three weeks early");
        exp2.setVisible(true);
        exp2.setDisplayOrder(200);

        Project proj1 = new Project();
        proj1.setUser(testUser);
        proj1.setTitle("ForkMyFolio");
        proj1.setDescription("Self-hosted portfolio platform with a Spring Boot backend, "
                + "Vue admin dashboard and Nuxt public site.");
        proj1.setLiveUrl("https://example.com/forkmyfolio");
        proj1.setSkills(new LinkedHashSet<>(List.of(skill("Java", "Backend"), skill("Vue", "Frontend"))));
        proj1.setVisible(true);
        proj1.setDisplayOrder(100);
        com.forkmyfolio.model.KeyFeature kf1 = new com.forkmyfolio.model.KeyFeature();
        kf1.setValue("Structured export parity across PDF and Markdown");
        kf1.setDisplayOrder(0);
        kf1.setProject(proj1);
        com.forkmyfolio.model.KeyFeature kf2 = new com.forkmyfolio.model.KeyFeature();
        kf2.setValue("Nine configurable PDF templates");
        kf2.setDisplayOrder(1);
        kf2.setProject(proj1);
        proj1.setKeyFeatures(new ArrayList<>(List.of(kf1, kf2)));

        Project proj2 = new Project();
        proj2.setUser(testUser);
        proj2.setTitle("Build Metrics");
        proj2.setDescription("Lightweight CI analytics dashboard.");
        proj2.setRepoUrl("https://github.com/example/build-metrics");
        proj2.setVisible(true);
        proj2.setDisplayOrder(200);

        Skill vue = skill("Vue", "Frontend");
        vue.setLevel(SkillLevel.ADVANCED);
        Skill typescript = skill("TypeScript", "Frontend");
        typescript.setLevel(SkillLevel.ADVANCED);
        Skill java = skill("Java", "Backend");
        java.setLevel(SkillLevel.EXPERT);
        Skill postgres = skill("Postgres", null); // uncategorised → "Other Skills"
        postgres.setLevel(SkillLevel.INTERMEDIATE);

        Qualification qual1 = new Qualification();
        qual1.setUser(testUser);
        qual1.setQualificationName("BSc Computer Science");
        qual1.setInstitutionName("University of Example");
        qual1.setCompletionYear(2018);
        qual1.setVisible(true);

        Qualification qual2 = new Qualification();
        qual2.setUser(testUser);
        qual2.setQualificationName("AWS Solutions Architect");
        qual2.setInstitutionName("Amazon Web Services");
        qual2.setCompletionYear(2020);
        qual2.setVisible(true);

        Testimonial t1 = new Testimonial();
        t1.setQuote("An exceptional engineer who raises the bar for every team they join.");
        t1.setAuthorName("Ada Lovelace");
        t1.setAuthorTitle("CTO, Analytical Engines");
        Testimonial t2 = new Testimonial();
        t2.setQuote("Delivered beyond expectations, every single time.");
        t2.setAuthorName("Grace Hopper");
        t2.setAuthorTitle("Rear Admiral");
        Testimonial t3 = new Testimonial();
        t3.setQuote("A rare talent for system design.");
        t3.setAuthorName("Alan Turing");

        return new PortfolioData(profile,
                new ArrayList<>(List.of(exp1, exp2)),
                new ArrayList<>(List.of(qual1, qual2)),
                new ArrayList<>(List.of(proj1, proj2)),
                new ArrayList<>(List.of(vue, typescript, java, postgres)),
                new ArrayList<>(List.of(t1, t2, t3)),
                "test@example.com", true, true, ExportMode.FULL, null);
    }

    private Skill skill(String name, String category) {
        Skill s = new Skill();
        s.setName(name);
        s.setCategory(category);
        s.setLevel(SkillLevel.INTERMEDIATE);
        return s;
    }

    /**
     * Generates one corpus PDF per template. The retired "metro-dark"
     * template is instantiated directly (it is no longer in the selectable
     * registry) so its archived rendering remains reproducible.
     */
    @Test
    void generateCorpus_forEveryTemplate() throws Exception {
        PortfolioData data = richFixture();
        stubExportData(data);

        Path outDir = Path.of("target", "pdf-baseline");
        Files.createDirectories(outDir);

        // The eight selectable registry keys (mirrors the service map) plus
        // the archived metro-dark rendering.
        Map<String, PortfolioPdfTemplate> corpus = new LinkedHashMap<>();
        corpus.put("modern", new com.forkmyfolio.service.pdf.templates.ModernTemplate());
        corpus.put("elegance", new com.forkmyfolio.service.pdf.templates.EleganceTemplate());
        corpus.put("classic", new com.forkmyfolio.service.pdf.templates.ClassicTemplate());
        corpus.put("metro-light", new com.forkmyfolio.service.pdf.templates.MetroLightTemplate());
        corpus.put("executive", create(com.forkmyfolio.service.pdf.templates.ExecutiveTemplate::new));
        corpus.put("modern-minimal", create(com.forkmyfolio.service.pdf.templates.ModernMinimalTemplate::new));
        corpus.put("technical", create(com.forkmyfolio.service.pdf.templates.TechnicalTemplate::new));
        corpus.put("business-card", new com.forkmyfolio.service.pdf.templates.BusinessCardTemplate());
        corpus.put("metro-dark", create(com.forkmyfolio.service.pdf.templates.MetroDarkTemplate::new));

        for (Map.Entry<String, PortfolioPdfTemplate> entry : corpus.entrySet()) {
            String templateName = entry.getKey();
            testUser.setUserSettings(new HashSet<>());
            testUser.getUserSettings().add(new UserSetting(testUser, "portfolio.pdf.template", templateName));

            PdfGenerationService.PdfFile pdfFile = templateName.equals("metro-dark")
                    ? generateDirect(entry.getValue(), data)
                    : pdfGenerationService.generatePortfolioPdf(testUser, templateName, ExportVisibility.PUBLIC);

            assertNotNull(pdfFile, "PDF for template " + templateName);
            assertTrue(pdfFile.content().length > 0, "non-empty PDF for " + templateName);

            Path out = outDir.resolve(templateName + ".pdf");
            Files.write(out, pdfFile.content());

            int pages = pageCount(pdfFile.content());
            assertTrue(pages >= 1, templateName + " must render at least one page");
            System.out.printf("[pdf-corpus] %-14s %6d bytes  %d page(s)%n",
                    templateName, pdfFile.content().length, pages);
        }
    }

    @FunctionalInterface
    private interface TemplateFactory {
        PortfolioPdfTemplate create() throws IOException, org.openpdf.text.DocumentException;
    }

    private static PortfolioPdfTemplate create(TemplateFactory factory) {
        try {
            return factory.create();
        } catch (IOException | org.openpdf.text.DocumentException e) {
            throw new IllegalStateException("Failed to initialise PDF template", e);
        }
    }

    /**
     * Renders a template directly (bypassing the service registry) — used
     * for the retired metro-dark template. Phase 0.12: replicates the
     * service's OpenPDF flow — the template's page size, margins and page
     * event are applied at document-open time, then the open document is
     * handed to the template.
     */
    private PdfGenerationService.PdfFile generateDirect(PortfolioPdfTemplate template, PortfolioData data)
            throws IOException, org.openpdf.text.DocumentException {
        try (var baos = new java.io.ByteArrayOutputStream()) {
            float[] margins = template.getMargins();
            var document = new org.openpdf.text.Document(template.getPageSize(),
                    margins[0], margins[1], margins[2], margins[3]);
            var writer = org.openpdf.text.pdf.PdfWriter.getInstance(document, baos);
            var pageEvent = template.getPageEvent();
            if (pageEvent != null) {
                writer.setPageEvent(pageEvent);
            }
            document.open();
            try {
                template.generate(document, new PdfGenerationService.PdfContext(), data);
            } finally {
                document.close();
            }
            return new PdfGenerationService.PdfFile(baos.toByteArray(), "metro-dark.pdf");
        }
    }

    /** Page count via the current PDF library's reader. */
    private static int pageCount(byte[] content) throws IOException {
        var reader = new org.openpdf.text.pdf.PdfReader(content);
        try {
            return reader.getNumberOfPages();
        } finally {
            reader.close();
        }
    }
}
