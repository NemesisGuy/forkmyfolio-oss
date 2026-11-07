package com.forkmyfolio.service.pdf.templates;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.impl.PdfGenerationService;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The retired "Metro Dark" template — kept only so previously generated
 * artefacts remain reproducible (the corpus test instantiates it directly;
 * it is no longer selectable and "metro-dark" preferences alias to
 * "metro-light").
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): the canvas-wide background fill is drawn
 * through a page event writing to the under-content layer (registered by
 * the service before the document opens — the previous engine set a
 * document-level background colour mid-render). Section rules use
 * {@link LineSeparator} (previously 1pt background-filled divs). Fonts are
 * {@link BaseFont} programs loaded once in the constructor.
 *
 * <p>
 * Legacy behaviours deliberately preserved exactly as they were: the
 * contact line reads {@code profile.getPublicEmail()} directly (not the
 * export data's {@code contactEmail}), honours no contact toggle, shows
 * only email + website, and the template renders no footer, testimonials
 * or page numbers.
 */
public class MetroDarkTemplate implements PortfolioPdfTemplate {

    // --- Enhanced Dark Mode Palette ---
    private static final Color BG_COLOR = new Color(20, 20, 25);         // #141419 (Deep Charcoal)
    private static final Color TEXT_COLOR = new Color(235, 235, 235);   // #EBEBEB (Bright Off-white)
    private static final Color ACCENT_COLOR = new Color(0, 150, 255);   // #0096FF (Vibrant Blue)
    private static final Color SUBTLE_COLOR = new Color(160, 160, 160); // #A0A0A0 (Medium Gray)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private final BaseFont headingFont;
    private final BaseFont bodyFont;
    private final BaseFont bodyItalicFont;

    public MetroDarkTemplate() throws IOException, DocumentException {
        this.headingFont = loadFontFromResource("fonts/Montserrat-Bold.ttf");
        this.bodyFont = loadFontFromResource("fonts/Lato-Regular.ttf");
        this.bodyItalicFont = loadFontFromResource("fonts/Lato-Italic.ttf");
    }

    @Override
    public float[] getMargins() {
        // Previous engine: setMargins(40, 40, 40, 40) (overrode the corpus
        // harness's zero margins mid-render; 40 was the effective value)
        return new float[]{40, 40, 40, 40}; // {left, right, top, bottom}
    }

    /**
     * The canvas-wide background fill: painted on the under-content layer
     * of every page so body text renders above it.
     */
    @Override
    public PdfPageEventHelper getPageEvent() {
        return new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                Rectangle page = document.getPageSize();
                PdfContentByte under = writer.getDirectContentUnder();
                under.saveState();
                under.setColorFill(BG_COLOR);
                under.rectangle(page.getLeft(), page.getBottom(),
                        page.getRight() - page.getLeft(), page.getTop() - page.getBottom());
                under.fill();
                under.restoreState();
            }
        };
    }

    @Override
    public void generate(Document document, PdfGenerationService.PdfContext ctx, PortfolioData data)
            throws IOException, DocumentException {
        User user = data.profile().getUser();

        // --- Header ---
        Paragraph nameLine = new Paragraph(user.getFirstName().toUpperCase() + " " + user.getLastName().toUpperCase(),
                new Font(headingFont, 36, Font.NORMAL, TEXT_COLOR));
        nameLine.setAlignment(Element.ALIGN_CENTER);
        nameLine.setSpacingAfter(0);
        document.add(nameLine);

        Paragraph headlineLine = new Paragraph(data.profile().getHeadline(),
                new Font(bodyItalicFont, 14, Font.NORMAL, ACCENT_COLOR));
        headlineLine.setAlignment(Element.ALIGN_CENTER);
        headlineLine.setSpacingAfter(10);
        document.add(headlineLine);

        // --- Clickable and robust contact info ---
        createContactInfo(document, data.profile());

        // --- Body ---
        // Phase 8 (PRV-1): the summary honours the profile's own public
        // section toggle — public exports omit it when hidden, exactly like
        // the public portfolio pages.
        if (data.includeSummary()) {
            createSection(document, "Profile", data.profile().getSummary());
        }
        createExperienceSection(document, data.experiences());
        createProjectsSection(document, data.projects());
        createSkillsSection(document, data.skills());
        createQualificationsSection(document, data.qualifications());
    }

    /**
     * Phase 7.4 (§8 parity): the Markdown export always carried a grouped
     * Skills section; this template omitted it entirely.
     */
    private void createSkillsSection(Document doc, List<Skill> skills) throws DocumentException {
        Map<String, List<Skill>> grouped = skillsGroupedByCategory(skills);
        if (grouped.isEmpty()) return;
        createSectionHeader(doc, "Skills");
        for (Map.Entry<String, List<Skill>> entry : grouped.entrySet()) {
            String names = entry.getValue().stream().map(Skill::getName).collect(Collectors.joining(", "));
            Paragraph category = new Paragraph(entry.getKey(),
                    new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR));
            category.setSpacingAfter(0);
            doc.add(category);
            Paragraph namesLine = new Paragraph(names, new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
            namesLine.setIndentationLeft(15);
            namesLine.setSpacingAfter(5);
            namesLine.setLeading(0, 1.2f);
            doc.add(namesLine);
        }
    }

    private void createContactInfo(Document doc, PortfolioProfile profile) throws DocumentException {
        Paragraph contactParagraph = new Paragraph(10);
        contactParagraph.setAlignment(Element.ALIGN_CENTER);
        contactParagraph.setSpacingAfter(20);
        List<Chunk> contactParts = new java.util.ArrayList<>();

        if (profile.getPublicEmail() != null && !profile.getPublicEmail().isBlank()) {
            Chunk email = new Chunk(profile.getPublicEmail(), new Font(bodyFont, 10, Font.NORMAL, SUBTLE_COLOR));
            email.setAnchor("mailto:" + profile.getPublicEmail());
            email.setUnderline(1f, -2);
            contactParts.add(email);
        }

        if (profile.getWebsiteUrl() != null && !profile.getWebsiteUrl().isBlank()) {
            Chunk website = new Chunk(profile.getWebsiteUrl().replaceFirst("^(https?://)", ""),
                    new Font(bodyFont, 10, Font.NORMAL, SUBTLE_COLOR));
            website.setAnchor(profile.getWebsiteUrl());
            website.setUnderline(1f, -2);
            contactParts.add(website);
        }

        for (int i = 0; i < contactParts.size(); i++) {
            contactParagraph.add(contactParts.get(i));
            if (i < contactParts.size() - 1) {
                contactParagraph.add(new Chunk("  •  ", new Font(bodyFont, 10, Font.NORMAL, SUBTLE_COLOR)));
            }
        }

        if (!contactParts.isEmpty()) {
            doc.add(contactParagraph);
        }
    }

    private void createSection(Document doc, String title, String content) throws DocumentException {
        if (content == null || content.isBlank()) return;
        createSectionHeader(doc, title);
        Paragraph body = new Paragraph(content, new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
        body.setLeading(0, 1.3f);
        doc.add(body);
    }

    private void createSectionHeader(Document doc, String title) throws DocumentException {
        Paragraph heading = new Paragraph(title.toUpperCase(),
                new Font(headingFont, 12, Font.NORMAL, ACCENT_COLOR));
        heading.setSpacingBefore(15);
        heading.setSpacingAfter(5);
        doc.add(heading);
        // Previously a 1pt background-filled div; now a full-width rule.
        Paragraph rule = new Paragraph(0);
        rule.add(new Chunk(new LineSeparator(1f, 100, ACCENT_COLOR, Element.ALIGN_CENTER, -2)));
        rule.setSpacingAfter(10);
        doc.add(rule);
    }

    private void createExperienceSection(Document doc, List<Experience> experiences) throws DocumentException {
        if (experiences.isEmpty()) return;
        createSectionHeader(doc, "Experience");
        experiences.sort(Comparator.comparing(Experience::getStartDate).reversed());
        for (Experience exp : experiences) {
            String dateText = exp.getStartDate().format(DATE_FORMATTER) + " - "
                    + (exp.getEndDate() == null ? "Present" : exp.getEndDate().format(DATE_FORMATTER));
            Paragraph titleLine = new Paragraph(0);
            titleLine.add(new Chunk(exp.getJobTitle(), new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR)));
            titleLine.add(new Chunk(" at ", new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR)));
            titleLine.add(new Chunk(exp.getCompanyName(), new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR)));
            titleLine.add(new Chunk("  |  " + dateText, new Font(bodyFont, 9, Font.NORMAL, SUBTLE_COLOR)));
            titleLine.setLeading(0, 1.4f);
            titleLine.setSpacingAfter(0);
            doc.add(titleLine);

            Paragraph description = new Paragraph(exp.getDescription(),
                    new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
            description.setIndentationLeft(15);
            description.setSpacingBefore(5);
            description.setLeading(0, 1.2f);
            List<String> achievements = achievementLines(exp);
            description.setSpacingAfter(achievements.isEmpty() ? 15 : 2);
            doc.add(description);

            // Phase 7.4 (§7): render key achievements (previously missing from
            // this template; the Markdown export always included them).
            if (!achievements.isEmpty()) {
                Paragraph achievementsHeading = new Paragraph("Key Achievements",
                        new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR));
                achievementsHeading.setIndentationLeft(15);
                achievementsHeading.setSpacingAfter(1);
                doc.add(achievementsHeading);
                for (String achievement : achievements) {
                    Paragraph bullet = new Paragraph("– " + achievement,
                            new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
                    bullet.setIndentationLeft(28);
                    bullet.setLeading(0, 1.2f);
                    bullet.setSpacingAfter(1);
                    doc.add(bullet);
                }
                Paragraph gap = new Paragraph(" ");
                gap.setSpacingAfter(10); // gap before next entry
                doc.add(gap);
            }
        }
    }

    private void createProjectsSection(Document doc, List<Project> projects) throws DocumentException {
        if (projects.isEmpty()) return;
        createSectionHeader(doc, "Projects");
        for (Project proj : projects) {
            // Determine the primary URL for the project link
            String projectUrl = null;
            if (proj.getLiveUrl() != null && !proj.getLiveUrl().isBlank()) {
                projectUrl = proj.getLiveUrl();
            } else if (proj.getRepoUrl() != null && !proj.getRepoUrl().isBlank()) {
                projectUrl = proj.getRepoUrl();
            }

            boolean linked = projectUrl != null;
            Chunk titleChunk = new Chunk(proj.getTitle(),
                    new Font(bodyFont, 10, Font.BOLD, linked ? ACCENT_COLOR : TEXT_COLOR));
            if (linked) {
                titleChunk.setAnchor(projectUrl);
                titleChunk.setUnderline(1f, -2);
            }

            Paragraph titleLine = new Paragraph(0);
            titleLine.add(titleChunk);
            titleLine.add(new Chunk("  |  " + proj.getSkills().stream().map(Skill::getName).collect(Collectors.joining(", ")),
                    new Font(bodyFont, 9, Font.NORMAL, SUBTLE_COLOR)));
            titleLine.setLeading(0, 1.4f);
            titleLine.setSpacingAfter(0);
            doc.add(titleLine);

            Paragraph description = new Paragraph(proj.getDescription(),
                    new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
            description.setIndentationLeft(15);
            description.setSpacingBefore(5);
            description.setLeading(0, 1.2f);
            List<String> keyFeatures = keyFeatureLines(proj);
            description.setSpacingAfter(keyFeatures.isEmpty() ? 15 : 2);
            doc.add(description);

            // Phase 7.4 (§6): structured key features (none for legacy projects)
            if (!keyFeatures.isEmpty()) {
                Paragraph featuresHeading = new Paragraph("Key Features",
                        new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR));
                featuresHeading.setIndentationLeft(15);
                featuresHeading.setSpacingAfter(1);
                doc.add(featuresHeading);
                for (String feature : keyFeatures) {
                    Paragraph bullet = new Paragraph("– " + feature,
                            new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
                    bullet.setIndentationLeft(28);
                    bullet.setLeading(0, 1.2f);
                    bullet.setSpacingAfter(1);
                    doc.add(bullet);
                }
                Paragraph gap = new Paragraph(" ");
                gap.setSpacingAfter(10); // gap before next entry
                doc.add(gap);
            }
        }
    }

    private void createQualificationsSection(Document doc, List<Qualification> qualifications) throws DocumentException {
        if (qualifications.isEmpty()) return;
        createSectionHeader(doc, "Education");
        qualifications.sort(Comparator.comparing(Qualification::getCompletionYear).reversed());
        for (Qualification qual : qualifications) {
            Paragraph titleLine = new Paragraph(0);
            titleLine.add(new Chunk(qual.getQualificationName(), new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR)));
            titleLine.add(new Chunk("  |  " + qual.getCompletionYear(), new Font(bodyFont, 9, Font.NORMAL, SUBTLE_COLOR)));
            titleLine.setLeading(0, 1.4f);
            titleLine.setSpacingAfter(0);
            doc.add(titleLine);
            Paragraph institution = new Paragraph(qual.getInstitutionName(),
                    new Font(bodyItalicFont, 10, Font.ITALIC, TEXT_COLOR));
            institution.setIndentationLeft(15);
            institution.setSpacingBefore(0);
            institution.setSpacingAfter(10);
            institution.setLeading(0, 1.4f);
            doc.add(institution);
        }
    }

    private static BaseFont loadFontFromResource(String resourcePath) throws IOException, DocumentException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Font resource not found: " + resourcePath);
            }
            return BaseFont.createFont(resourcePath, BaseFont.WINANSI, BaseFont.EMBEDDED, true, is.readAllBytes(), null);
        }
    }
}
