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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8 PRV-1 (SECURITY DEFECT) regression suite: public export endpoints
 * (PDF + Markdown) must apply EXACTLY the same visibility rules as the
 * public portfolio pages.
 *
 * <p>
 * Pre-fix behaviour (the defect): the export services rendered EVERY stored
 * item — including experiences, projects, qualifications, skills and
 * testimonials the owner had explicitly hidden — to any anonymous guest, and
 * the Markdown contact line leaked the ACCOUNT login email instead of the
 * profile's public contact email.
 *
 * <p>
 * Contract under test (user-approved 2026-11-04):
 * <ul>
 *   <li>GUEST (unauthenticated): only publicly visible content; public
 *       email only; profile summary honours the profile's own toggle.</li>
 *   <li>OWNER (authenticated as the portfolio owner): may export their own
 *       hidden content.</li>
 *   <li>CROSS-USER (authenticated as somebody else): treated exactly like a
 *       guest — authentication must NEVER grant another user's hidden
 *       content.</li>
 * </ul>
 *
 * <p>
 * Runs against the real MySQL testcontainer with real JWT logins, and
 * extracts text from the generated PDFs with iText's {@code PdfTextExtractor}
 * so the assertions see what a human reader would see.
 */
@Transactional
public class ExportPrivacyIntegrationTest extends AbstractMySqlIntegrationTest {

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
        private String hiddenExperienceTitle;
        private String visibleExperienceTitle;
        private String hiddenProjectTitle;
        private String visibleProjectTitle;
        private String hiddenQualificationName;
        private String visibleQualificationName;
        private String hiddenSkillName;
        private String visibleSkillName;
        private String hiddenTestimonialQuote;
        private String visibleTestimonialQuote;
        private String publicEmail;
        private String accountEmail;

        @BeforeEach
        void seedPortfolioOwnerWithMixedVisibility() throws Exception {
                String suffix = runSuffix();
                ownerEmail = "export-owner-" + suffix + "@example.com";
                ownerSlug = "export-owner-" + suffix;
                accountEmail = ownerEmail;
                publicEmail = "contact-" + suffix + "@public.example.com";

                hiddenExperienceTitle = "HiddenRole-" + suffix;
                visibleExperienceTitle = "VisibleRole-" + suffix;
                hiddenProjectTitle = "HiddenProject-" + suffix;
                visibleProjectTitle = "VisibleProject-" + suffix;
                hiddenQualificationName = "HiddenDegree-" + suffix;
                visibleQualificationName = "VisibleDegree-" + suffix;
                hiddenSkillName = "HiddenSkill-" + suffix;
                visibleSkillName = "VisibleSkill-" + suffix;
                hiddenTestimonialQuote = "Hidden quote " + suffix;
                visibleTestimonialQuote = "Visible quote " + suffix;

                User owner = seedUser(ownerEmail, ownerSlug, true);
                PortfolioProfile profile = owner.getPortfolioProfile();
                profile.setPublicEmail(publicEmail);
                userRepository.save(owner);

                // --- Experiences: one hidden, one visible -----------------------
                Experience hiddenExp = baseExperience(owner, hiddenExperienceTitle, 1);
                hiddenExp.setVisible(false);
                experienceRepository.save(hiddenExp);

                Experience visibleExp = baseExperience(owner, visibleExperienceTitle, 0);
                visibleExp.setVisible(true);
                experienceRepository.save(visibleExp);

                // --- Projects: one hidden, one visible --------------------------
                Project hiddenProject = new Project();
                hiddenProject.setUser(owner);
                hiddenProject.setTitle(hiddenProjectTitle);
                hiddenProject.setDescription("Secret internal project description " + suffix);
                hiddenProject.setVisible(false);
                hiddenProject.setDisplayOrder(1);
                projectRepository.save(hiddenProject);

                Project visibleProject = new Project();
                visibleProject.setUser(owner);
                visibleProject.setTitle(visibleProjectTitle);
                visibleProject.setDescription("Public showcase project description " + suffix);
                visibleProject.setVisible(true);
                visibleProject.setDisplayOrder(0);
                projectRepository.save(visibleProject);

                // --- Qualifications: one hidden, one visible --------------------
                Qualification hiddenQual = new Qualification();
                hiddenQual.setUser(owner);
                hiddenQual.setQualificationName(hiddenQualificationName);
                hiddenQual.setInstitutionName("Secret Institute");
                hiddenQual.setStartYear(2010);
                hiddenQual.setCompletionYear(2014);
                hiddenQual.setVisible(false);
                qualificationRepository.save(hiddenQual);

                Qualification visibleQual = new Qualification();
                visibleQual.setUser(owner);
                visibleQual.setQualificationName(visibleQualificationName);
                visibleQual.setInstitutionName("Public University");
                visibleQual.setStartYear(2015);
                visibleQual.setCompletionYear(2019);
                visibleQual.setVisible(true);
                qualificationRepository.save(visibleQual);

                // --- Skills: one hidden, one visible ----------------------------
                saveUserSkill(owner, hiddenSkillName, SkillLevel.EXPERT, false);
                saveUserSkill(owner, visibleSkillName, SkillLevel.ADVANCED, true);

                // --- Testimonials: one hidden, one visible ----------------------
                // NOTE: owner is a managed entity in this persistence context,
                // so the bidirectional relation MUST be synced on the parent
                // collection too — ExportContentService reads
                // owner.getTestimonials()/owner.getUserSkills().
                Testimonial hiddenTestimonial = new Testimonial();
                hiddenTestimonial.setUser(owner);
                hiddenTestimonial.setQuote(hiddenTestimonialQuote);
                hiddenTestimonial.setAuthorName("Private Colleague");
                hiddenTestimonial.setVisible(false);
                owner.getTestimonials().add(hiddenTestimonial);
                testimonialRepository.save(hiddenTestimonial);

                Testimonial visibleTestimonial = new Testimonial();
                visibleTestimonial.setUser(owner);
                visibleTestimonial.setQuote(visibleTestimonialQuote);
                visibleTestimonial.setAuthorName("Public Colleague");
                visibleTestimonial.setVisible(true);
                owner.getTestimonials().add(visibleTestimonial);
                testimonialRepository.save(visibleTestimonial);

                // Second user: the authenticated cross-user (attacker) probe
                User attacker = seedUser("export-attacker-" + suffix + "@example.com",
                                "export-attacker-" + suffix, true);

                ownerJwt = realLogin(ownerEmail, PASSWORD);
                attackerJwt = realLogin(attacker.getEmail(), PASSWORD);
        }

        private Experience baseExperience(User owner, String jobTitle, int displayOrder) {
                Experience exp = new Experience();
                exp.setUser(owner);
                exp.setJobTitle(jobTitle);
                exp.setCompanyName("Some Corp");
                exp.setStartDate(LocalDate.of(2020, 1, 1));
                exp.setDescription("Experience description for " + jobTitle);
                exp.setDisplayOrder(displayOrder);
                return exp;
        }

        private void saveUserSkill(User owner, String skillName, SkillLevel level, boolean visible) {
                Skill skill = new Skill();
                skill.setName(skillName);
                skill.setCategory("Testing");
                skillRepository.save(skill);
                UserSkill userSkill = new UserSkill();
                userSkill.setUser(owner);
                userSkill.setSkill(skill);
                userSkill.setLevel(level);
                userSkill.setVisible(visible);
                // Sync the bidirectional relation on the managed parent —
                // ExportContentService reads owner.getUserSkills().
                owner.getUserSkills().add(userSkill);
                userSkillRepository.save(userSkill);
        }

        // ------------------------------------------------------------------
        // Markdown exports
        // ------------------------------------------------------------------

        @Test
        void guestMarkdown_excludesAllHiddenContent_andNeverLeaksAccountEmail() throws Exception {
                String md = downloadMarkdown(null);

                // Visible content IS present
                assertThat(md).contains(visibleExperienceTitle);
                assertThat(md).contains(visibleProjectTitle);
                assertThat(md).contains(visibleQualificationName);
                assertThat(md).contains(visibleSkillName);

                // Hidden content is NEVER present (the PRV-1 defect).
                // NOTE: testimonials are selected under the same contract but
                // not yet RENDERED by any export template (Phase 8.3 adds
                // rendering) — content assertions on quotes would be vacuous.
                assertThat(md).doesNotContain(hiddenExperienceTitle);
                assertThat(md).doesNotContain(hiddenProjectTitle);
                assertThat(md).doesNotContain(hiddenQualificationName);
                assertThat(md).doesNotContain(hiddenSkillName);
                assertThat(md).doesNotContain(hiddenTestimonialQuote);

                // Contact leak: the ACCOUNT login email must never appear in a
                // public export — only the profile's public contact email.
                assertThat(md).doesNotContain(accountEmail);
                assertThat(md).contains(publicEmail);
        }

        @Test
        void ownerMarkdown_includesOwnHiddenContent() throws Exception {
                String md = downloadMarkdown(ownerJwt);

                // The owner may export their own non-public content
                assertThat(md).contains(hiddenExperienceTitle);
                assertThat(md).contains(hiddenProjectTitle);
                assertThat(md).contains(hiddenQualificationName);
                assertThat(md).contains(hiddenSkillName);

                // ...and of course their public content
                assertThat(md).contains(visibleExperienceTitle);
                assertThat(md).contains(visibleProjectTitle);
        }

        @Test
        void crossUserMarkdown_authenticatedNonOwner_isTreatedExactlyLikeGuest() throws Exception {
                String md = downloadMarkdown(attackerJwt);

                // Authentication must NEVER grant another user's hidden content
                assertThat(md).doesNotContain(hiddenExperienceTitle);
                assertThat(md).doesNotContain(hiddenProjectTitle);
                assertThat(md).doesNotContain(hiddenQualificationName);
                assertThat(md).doesNotContain(hiddenSkillName);
                assertThat(md).doesNotContain(hiddenTestimonialQuote);

                // ...and never the owner's account email either
                assertThat(md).doesNotContain(accountEmail);
                assertThat(md).contains(publicEmail);

                // Public content remains readable
                assertThat(md).contains(visibleExperienceTitle);
                assertThat(md).contains(visibleProjectTitle);
        }

        // ------------------------------------------------------------------
        // PDF exports (text extracted with iText — what a reader sees)
        // NOTE: the Classic template can wrap long skill names across lines,
        // so PDF presence/absence assertions use the unique name PREFIX
        // ("VisibleSkill-"/"HiddenSkill-") instead of the full suffixed name.
        // ------------------------------------------------------------------

        @Test
        void guestPdf_excludesAllHiddenContent() throws Exception {
                String pdfText = downloadPdfText(null);

                assertThat(pdfText).contains(visibleExperienceTitle);
                assertThat(pdfText).contains(visibleProjectTitle);
                assertThat(pdfText).contains(visibleQualificationName);
                assertThat(pdfText).contains("VisibleSkill-");

                assertThat(pdfText).doesNotContain(hiddenExperienceTitle);
                assertThat(pdfText).doesNotContain(hiddenProjectTitle);
                assertThat(pdfText).doesNotContain(hiddenQualificationName);
                assertThat(pdfText).doesNotContain("HiddenSkill-");
        }

        @Test
        void ownerPdf_includesOwnHiddenContent() throws Exception {
                String pdfText = downloadPdfText(ownerJwt);

                assertThat(pdfText).contains(hiddenExperienceTitle);
                assertThat(pdfText).contains(hiddenProjectTitle);
                assertThat(pdfText).contains(hiddenQualificationName);
                assertThat(pdfText).contains("HiddenSkill-");
                assertThat(pdfText).contains(visibleExperienceTitle);
        }

        @Test
        void crossUserPdf_authenticatedNonOwner_isTreatedExactlyLikeGuest() throws Exception {
                String pdfText = downloadPdfText(attackerJwt);

                assertThat(pdfText).doesNotContain(hiddenExperienceTitle);
                assertThat(pdfText).doesNotContain(hiddenProjectTitle);
                assertThat(pdfText).doesNotContain(hiddenQualificationName);
                assertThat(pdfText).doesNotContain("HiddenSkill-");
                assertThat(pdfText).contains(visibleExperienceTitle);
        }

        // ------------------------------------------------------------------
        // Profile summary section toggle
        // ------------------------------------------------------------------

        @Test
        void profileSummaryHidden_guestAndCrossUserExportsOmitIt_ownerExportIncludesIt() throws Exception {
                // Hide the profile's summary section (same toggle the public
                // portfolio pages honour)
                User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
                owner.getPortfolioProfile().setVisible(false);
                userRepository.save(owner);

                String guestMd = downloadMarkdown(null);
                assertThat(guestMd).doesNotContain("Summary of " + ownerSlug);
                assertThat(guestMd).contains("Headline of " + ownerSlug); // headline unaffected

                String crossUserMd = downloadMarkdown(attackerJwt);
                assertThat(crossUserMd).doesNotContain("Summary of " + ownerSlug);

                String ownerMd = downloadMarkdown(ownerJwt);
                assertThat(ownerMd).contains("Summary of " + ownerSlug);

                String guestPdfText = downloadPdfText(null);
                assertThat(guestPdfText).doesNotContain("Summary of " + ownerSlug);

                String ownerPdfText = downloadPdfText(ownerJwt);
                assertThat(ownerPdfText).contains("Summary of " + ownerSlug);
        }

        // ------------------------------------------------------------------
        // Helpers
        // ------------------------------------------------------------------

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
