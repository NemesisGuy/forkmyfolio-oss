package com.forkmyfolio.integration;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.repository.ExperienceRepository;
import com.forkmyfolio.repository.ProjectRepository;
import com.forkmyfolio.repository.QualificationRepository;
import com.forkmyfolio.repository.SkillRepository;
import com.forkmyfolio.repository.TestimonialRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.repository.UserSkillRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8.3: export preferences (mode + section toggles) end-to-end suite.
 *
 * <p>
 * Contract under test:
 * <ul>
 *   <li>Defaults: a user with NO export preferences gets FULL mode with
 *       every section enabled — byte-for-byte the pre-8.3 behaviour.</li>
 *   <li>Preferences persist via the existing {@code GET/PUT
 *       /api/v1/me/settings} mechanism (UserSetting overrides over the
 *       seeded global {@code export.*} settings) — no new entity/endpoint.</li>
 *   <li>OWNER exports honour the owner's mode + toggles; BRIEF omits
 *       long-form descriptions and caps bullet lists at three.</li>
 *   <li>PRV-1 invariant: PUBLIC exports (guests AND authenticated
 *       non-owners) IGNORE the owner's export preferences completely.</li>
 *   <li>Malformed stored values fall back safely (never fail an export).</li>
 *   <li>PDF and Markdown share the same selection engine — a disabled
 *       section and BRIEF trimming appear identically in both formats.</li>
 * </ul>
 *
 * <p>
 * Runs against the real MySQL testcontainer with real JWT logins; PDF text
 * is extracted with iText so assertions see what a human reader would see.
 */
@Transactional
public class ExportPreferencesIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExperienceRepository experienceRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private QualificationRepository qualificationRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private UserSkillRepository userSkillRepository;
    @Autowired
    private TestimonialRepository testimonialRepository;

    private String ownerEmail;
    private String ownerSlug;
    private String ownerJwt;
    private String attackerJwt;

    // Unique markers — asserted present/absent in exports
    private String experienceTitle;
    private String experienceDescription;
    private String achievement1;
    private String achievement4;
    private String projectTitle;
    private String projectDescription;
    private String keyFeature1;
    private String keyFeature5;
    private String qualificationName;
    private String qualificationGrade;
    private String skillName;
    private String testimonialQuote;
    private String testimonialHiddenQuote;
    private String publicEmail;
    private String accountEmail;

    @BeforeEach
    void seedOwnerWithRichContent() throws Exception {
        String suffix = runSuffix();
        ownerEmail = "pref-owner-" + suffix + "@example.com";
        ownerSlug = "pref-owner-" + suffix;
        accountEmail = ownerEmail;
        publicEmail = "pref-contact-" + suffix + "@public.example.com";

        experienceTitle = "Backend Engineer-" + suffix;
        experienceDescription = "Long-form experience description " + suffix;
        achievement1 = "Achievement one " + suffix;
        achievement4 = "Achievement four " + suffix;
        projectTitle = "Pref Project-" + suffix;
        projectDescription = "Long-form project description " + suffix;
        keyFeature1 = "Feature one " + suffix;
        keyFeature5 = "Feature five " + suffix;
        qualificationName = "BSc Computer Science-" + suffix;
        qualificationGrade = "First Class Honours " + suffix;
        skillName = "PrefSkill-" + suffix;
        testimonialQuote = "Outstanding engineer " + suffix;
        testimonialHiddenQuote = "Hidden praise " + suffix;

        User owner = seedUser(ownerEmail, ownerSlug, true);
        PortfolioProfile profile = owner.getPortfolioProfile();
        profile.setPublicEmail(publicEmail);
        profile.setSummary("Summary of " + ownerSlug);
        userRepository.save(owner);

        // Experience with a long description and FOUR achievements (BRIEF
        // must cap at three — the 4th is the canary).
        Experience exp = new Experience();
        exp.setUser(owner);
        exp.setJobTitle(experienceTitle);
        exp.setCompanyName("Some Corp");
        exp.setStartDate(LocalDate.of(2020, 1, 1));
        exp.setDescription(experienceDescription);
        exp.setAchievements(String.join("\n", achievement1, "Achievement two " + suffix,
                "Achievement three " + suffix, achievement4));
        exp.setDisplayOrder(0);
        exp.setVisible(true);
        experienceRepository.save(exp);

        // Project with a long description and FIVE key features.
        Project project = new Project();
        project.setUser(owner);
        project.setTitle(projectTitle);
        project.setDescription(projectDescription);
        project.setVisible(true);
        project.setDisplayOrder(0);
        Project savedProject = projectRepository.save(project);
        // KeyFeature rows are managed through the project relation.
        String[] featureNames = {keyFeature1, "Feature two " + suffix,
                "Feature three " + suffix, "Feature four " + suffix, keyFeature5};
        for (int i = 1; i <= 5; i++) {
            com.forkmyfolio.model.KeyFeature f = new com.forkmyfolio.model.KeyFeature();
            f.setProject(savedProject);
            f.setValue(featureNames[i - 1]);
            f.setDisplayOrder(i - 1);
            savedProject.getKeyFeatures().add(f);
        }
        projectRepository.save(savedProject);

        Qualification qual = new Qualification();
        qual.setUser(owner);
        qual.setQualificationName(qualificationName);
        qual.setInstitutionName("Public University");
        qual.setStartYear(2015);
        qual.setCompletionYear(2019);
        qual.setGrade(qualificationGrade);
        qual.setVisible(true);
        qualificationRepository.save(qual);

        Skill skill = new Skill();
        skill.setName(skillName);
        skill.setCategory("Testing");
        skillRepository.save(skill);
        UserSkill userSkill = new UserSkill();
        userSkill.setUser(owner);
        userSkill.setSkill(skill);
        userSkill.setLevel(SkillLevel.ADVANCED);
        userSkill.setVisible(true);
        owner.getUserSkills().add(userSkill);
        userSkillRepository.save(userSkill);

        Testimonial visibleTestimonial = new Testimonial();
        visibleTestimonial.setUser(owner);
        visibleTestimonial.setQuote(testimonialQuote);
        visibleTestimonial.setAuthorName("Public Colleague");
        visibleTestimonial.setAuthorTitle("CTO, Example Corp");
        visibleTestimonial.setVisible(true);
        owner.getTestimonials().add(visibleTestimonial);
        testimonialRepository.save(visibleTestimonial);

        Testimonial hiddenTestimonial = new Testimonial();
        hiddenTestimonial.setUser(owner);
        hiddenTestimonial.setQuote(testimonialHiddenQuote);
        hiddenTestimonial.setAuthorName("Private Colleague");
        hiddenTestimonial.setVisible(false);
        owner.getTestimonials().add(hiddenTestimonial);
        testimonialRepository.save(hiddenTestimonial);

        User attacker = seedUser("pref-attacker-" + suffix + "@example.com",
                "pref-attacker-" + suffix, true);

        ownerJwt = realLogin(ownerEmail, PASSWORD);
        attackerJwt = realLogin(attacker.getEmail(), PASSWORD);
    }

    // ------------------------------------------------------------------
    // Defaults & persistence
    // ------------------------------------------------------------------

    @Test
    void exportSettings_areExposedWithSafeDefaults() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/settings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();

        assertThat(settingValue(body, "export.mode")).isEqualTo("FULL");
        for (String section : List.of("summary", "experience", "projects", "skills",
                "qualifications", "testimonials", "contact")) {
            assertThat(settingValue(body, "export.section." + section))
                    .as("section %s default", section)
                    .isEqualTo("true");
        }
    }

    @Test
    void exportSettings_persistAcrossRetrieveAndUpdateCycles() throws Exception {
        updateExportSettings(Map.of(
                "export.mode", "BRIEF",
                "export.section.projects", "false"));

        MvcResult result = mockMvc.perform(get("/api/v1/me/settings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(settingValue(body, "export.mode")).isEqualTo("BRIEF");
        assertThat(settingValue(body, "export.section.projects")).isEqualTo("false");
        // Untouched sections keep their effective values
        assertThat(settingValue(body, "export.section.experience")).isEqualTo("true");
    }

    @Test
    void ownerWithNoExportPreferences_getsDefaultFullExport() throws Exception {
        String md = downloadMarkdown(ownerJwt);
        assertThat(md).contains(experienceTitle);
        assertThat(md).contains(experienceDescription);
        assertThat(md).contains(achievement4); // all achievements, not capped
        assertThat(md).contains(projectDescription);
        assertThat(md).contains(keyFeature5);
        assertThat(md).contains(qualificationGrade);
        assertThat(md).contains(testimonialQuote);
        assertThat(md).contains(publicEmail);

        String pdfText = downloadPdfText(ownerJwt);
        assertThat(pdfText).contains(experienceDescription);
        assertThat(pdfText).contains(keyFeature5);
    }

    // ------------------------------------------------------------------
    // BRIEF mode
    // ------------------------------------------------------------------

    @Test
    void briefMarkdown_omitsLongFormAndCapsBullets() throws Exception {
        updateExportSettings(Map.of("export.mode", "BRIEF"));

        String md = downloadMarkdown(ownerJwt);

        // Concise: long-form descriptions omitted, bullets capped at three
        assertThat(md).contains(experienceTitle);
        assertThat(md).doesNotContain(experienceDescription);
        assertThat(md).contains(achievement1);
        assertThat(md).doesNotContain(achievement4); // 4th achievement capped
        assertThat(md).contains(projectTitle);
        assertThat(md).doesNotContain(projectDescription);
        assertThat(md).contains(keyFeature1);
        assertThat(md).doesNotContain(keyFeature5); // 5th feature capped
        assertThat(md).contains(qualificationName);
        assertThat(md).doesNotContain(qualificationGrade); // grade omitted
        // Summary text is user-authored — never truncated or rewritten
        assertThat(md).contains("Summary of " + ownerSlug);
        assertThat(md).contains(testimonialQuote); // testimonials still render (≤3)
    }

    @Test
    void briefPdf_omitsLongFormAndCapsBullets() throws Exception {
        updateExportSettings(Map.of("export.mode", "BRIEF"));

        String pdfText = downloadPdfText(ownerJwt);

        assertThat(pdfText).contains(experienceTitle);
        assertThat(pdfText).doesNotContain(experienceDescription);
        assertThat(pdfText).doesNotContain(achievement4);
        assertThat(pdfText).doesNotContain(projectDescription);
        assertThat(pdfText).doesNotContain(keyFeature5);
    }

    @Test
    void briefMode_isConsistentBetweenPdfAndMarkdown() throws Exception {
        updateExportSettings(Map.of("export.mode", "BRIEF"));

        String md = downloadMarkdown(ownerJwt);
        String pdfText = downloadPdfText(ownerJwt);

        // Same selection engine: both formats drop the same content
        assertThat(md).doesNotContain(experienceDescription);
        assertThat(pdfText).doesNotContain(experienceDescription);
        assertThat(md).doesNotContain(projectDescription);
        assertThat(pdfText).doesNotContain(projectDescription);
        assertThat(md).doesNotContain(achievement4);
        assertThat(pdfText).doesNotContain(achievement4);
    }

    // ------------------------------------------------------------------
    // Section toggles
    // ------------------------------------------------------------------

    @Test
    void ownerSectionToggles_removeSectionsFromMarkdownAndPdf() throws Exception {
        updateExportSettings(Map.of(
                "export.section.projects", "false",
                "export.section.testimonials", "false",
                "export.section.contact", "false"));

        String md = downloadMarkdown(ownerJwt);
        assertThat(md).doesNotContain(projectTitle);
        assertThat(md).doesNotContain(testimonialQuote);
        assertThat(md).doesNotContain(publicEmail); // contact block removed
        assertThat(md).doesNotContain(accountEmail); // and no fallback leak
        // Enabled sections unaffected
        assertThat(md).contains(experienceTitle);
        assertThat(md).contains("Summary of " + ownerSlug);

        String pdfText = downloadPdfText(ownerJwt);
        assertThat(pdfText).doesNotContain(projectTitle);
        assertThat(pdfText).doesNotContain(testimonialQuote);
        assertThat(pdfText).doesNotContain(publicEmail);
        assertThat(pdfText).contains(experienceTitle);
    }

    @Test
    void testimonialRendering_visibleQuoteInOwnerAndPublicExports_hiddenQuoteNeverPublic() throws Exception {
        String ownerMd = downloadMarkdown(ownerJwt);
        assertThat(ownerMd).contains(testimonialQuote);
        assertThat(ownerMd).contains("Public Colleague");
        assertThat(ownerMd).contains(testimonialHiddenQuote); // owner sees own hidden

        String guestMd = downloadMarkdown(null);
        assertThat(guestMd).contains(testimonialQuote);
        assertThat(guestMd).doesNotContain(testimonialHiddenQuote); // PRV-1
    }

    // ------------------------------------------------------------------
    // PRV-1 invariant: PUBLIC exports ignore owner preferences
    // ------------------------------------------------------------------

    @Test
    void publicExport_ignoresOwnerPreferences() throws Exception {
        // Owner configures the most restrictive possible own export
        updateExportSettings(Map.of(
                "export.mode", "BRIEF",
                "export.section.experience", "false",
                "export.section.projects", "false",
                "export.section.contact", "false"));

        // GUEST: unaffected — public exports mirror the public pages
        String guestMd = downloadMarkdown(null);
        assertThat(guestMd).contains(experienceTitle);
        assertThat(guestMd).contains(experienceDescription); // FULL form, not brief
        assertThat(guestMd).contains(projectTitle);
        assertThat(guestMd).contains(publicEmail); // contact still public

        String guestPdfText = downloadPdfText(null);
        assertThat(guestPdfText).contains(experienceTitle);
        assertThat(guestPdfText).contains(experienceDescription);
        assertThat(guestPdfText).contains(projectTitle);
        assertThat(guestPdfText).doesNotContain(accountEmail);
    }

    @Test
    void crossUserExport_ignoresOwnerPreferences() throws Exception {
        updateExportSettings(Map.of(
                "export.mode", "BRIEF",
                "export.section.experience", "false"));

        String md = downloadMarkdown(attackerJwt);
        assertThat(md).contains(experienceTitle);
        assertThat(md).contains(experienceDescription);
        assertThat(md).contains(publicEmail);
        assertThat(md).doesNotContain(accountEmail);
    }

    // ------------------------------------------------------------------
    // Malformed / partial settings fall back safely
    // ------------------------------------------------------------------

    @Test
    void malformedExportSettings_fallBackSafely() throws Exception {
        updateExportSettings(Map.of(
                "export.mode", "garbage-mode",
                "export.section.experience", "maybe",
                "export.section.projects", "unsupported-value"));

        // Unknown mode → FULL; unknown toggle values → enabled (a partially
        // configured user can never silently lose export content)
        String md = downloadMarkdown(ownerJwt);
        assertThat(md).contains(experienceDescription); // FULL form restored
        assertThat(md).contains(projectDescription);
        assertThat(md).contains(keyFeature5);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** PUTs export.* setting values by NAME (resolves uuids from GET first). */
    private void updateExportSettings(Map<String, String> nameToValue) throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/v1/me/settings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();
        String listBody = listResult.getResponse().getContentAsString();

        List<Map<String, Object>> payload = new java.util.ArrayList<>();
        for (Map.Entry<String, String> entry : nameToValue.entrySet()) {
            String uuid = settingUuid(listBody, entry.getKey());
            payload.add(Map.of("uuid", uuid, "value", entry.getValue()));
        }
        mockMvc.perform(put("/api/v1/me/settings")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    /** Reads a single setting's effective value from a GET /me/settings body. */
    @SuppressWarnings("unchecked")
    private String settingValue(String body, String name) {
        List<Map<String, Object>> matches = JsonPath.read(body,
                "$.data[?(@.name=='" + name + "')]");
        assertThat(matches).as("setting %s present", name).hasSize(1);
        return (String) matches.get(0).get("value");
    }

    /** Reads a single setting's uuid from a GET /me/settings body. */
    @SuppressWarnings("unchecked")
    private String settingUuid(String body, String name) {
        List<Map<String, Object>> matches = JsonPath.read(body,
                "$.data[?(@.name=='" + name + "')]");
        assertThat(matches).as("setting %s present", name).hasSize(1);
        return (String) matches.get(0).get("uuid");
    }

    private String downloadMarkdown(String jwt) throws Exception {
        var request = get("/api/v1/portfolios/{slug}/markdown", ownerSlug);
        if (jwt != null) {
            request = request.header("Authorization", "Bearer " + jwt);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String downloadPdfText(String jwt) throws Exception {
        var request = get("/api/v1/portfolios/{slug}/pdf", ownerSlug)
                .param("template", "classic");
        if (jwt != null) {
            request = request.header("Authorization", "Bearer " + jwt);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        StringBuilder extracted = new StringBuilder();
        var reader = new org.openpdf.text.pdf.PdfReader(pdfBytes);
        try {
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                extracted.append(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                        .getTextFromPage(i))
                        .append('\n');
            }
        } finally {
            reader.close();
        }
        return extracted.toString();
    }
}
