package com.forkmyfolio.service.pdf.templates;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ExportMode;
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
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A sophisticated, single-column "Elegance" template, now in a high-contrast light mode.
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): section rules are {@link LineSeparator}s
 * (previously border-bottom paragraphs), the contact row and item headers
 * are {@link PdfPTable}s, skill pills use {@link Chunk#setBackground(Color)},
 * and fonts are {@link BaseFont} programs loaded once in the constructor.
 * Text that inherited the document-level default font/size in the previous
 * engine now carries explicit fonts (12pt default size preserved).
 */
public class EleganceTemplate implements PortfolioPdfTemplate {

    // --- Template-Specific Colors (Light Mode) ---
    private static final Color TEXT_COLOR = new Color(34, 40, 49); // #222831 (Dark Charcoal)
    private static final Color ACCENT_COLOR = new Color(0, 173, 181); // #00ADB5 (Teal)
    private static final Color SUBTLE_COLOR = new Color(108, 117, 125); // #6c757d (Medium Gray)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
    /** The document-level default size inherited by unstyled text previously. */
    private static final float DEFAULT_SIZE = 12;

    // --- Template-Specific Fonts ---
    private final BaseFont headingFont;
    private final BaseFont bodyFont;
    private final BaseFont bodyItalicFont;

    public EleganceTemplate() throws IOException, DocumentException {
        this.headingFont = loadFontFromResource("fonts/Montserrat-Bold.ttf");
        this.bodyFont = loadFontFromResource("fonts/Lato-Regular.ttf");
        this.bodyItalicFont = loadFontFromResource("fonts/Lato-Italic.ttf");
    }

    @Override
    public float[] getMargins() {
        // Previous engine: setMargins(40, 40, 40, 40)
        return new float[]{40, 40, 40, 40}; // {left, right, top, bottom}
    }

    @Override
    public void generate(Document document, PdfGenerationService.PdfContext ctx, PortfolioData data)
            throws IOException, DocumentException {
        // This template uses its own fonts and colors, so we set them up here.

        // Phase 8.3: the concise-resume flag resolved by the shared
        // selection engine (owner preference; PUBLIC exports are always FULL).
        boolean brief = data.exportMode() == ExportMode.BRIEF;

        // 1. Header
        createHeader(document, data, ctx.solidIconFont, ctx.brandsIconFont);

        // 2. Summary
        // Phase 8 (PRV-1): summary honours the profile's public section
        // toggle for public exports.
        if (data.includeSummary()) {
            createSection(document, "Summary", data.profile().getSummary());
        }

        // 3. Skills
        createSkillsSection(document, data.skills());

        // 4. Experience
        createExperienceSection(document, data.experiences(), brief);

        // 5. Projects
        createProjectsSection(document, data.projects(), brief);

        // 6. Qualifications
        createQualificationsSection(document, data.qualifications(), brief);

        // 7. Testimonials (Phase 8.5 DEF-4: legacy-template parity — this
        // template remained selectable but silently omitted testimonials).
        createTestimonialsSection(document, data, brief);
    }

    private void createHeader(Document doc, PortfolioData data, BaseFont solidIconFont, BaseFont brandsIconFont)
            throws DocumentException {
        PortfolioProfile profile = data.profile();
        User user = profile.getUser();
        // Name
        Paragraph nameLine = new Paragraph(user.getFirstName().toUpperCase() + " " + user.getLastName().toUpperCase(),
                new Font(headingFont, 32, Font.NORMAL, TEXT_COLOR));
        nameLine.setSpacingAfter(0);
        doc.add(nameLine);
        // Headline
        Paragraph headlineLine = new Paragraph(profile.getHeadline(), new Font(bodyFont, 14, Font.NORMAL, ACCENT_COLOR));
        headlineLine.setSpacingBefore(0);
        headlineLine.setSpacingAfter(10);
        doc.add(headlineLine);

        // --- DYNAMIC CONTACT SECTION ---
        // Phase 8.3: the contact block honours the owner's export contact
        // toggle; the email shown is the export data's contactEmail
        // (publicEmail for public exports, publicEmail-or-account-email for
        // owner exports — parity with the Markdown export).
        List<Paragraph> contactItems = new ArrayList<>();

        if (data.includeContact()) {
            // Email
            if (data.contactEmail() != null && !data.contactEmail().isBlank()) {
                contactItems.add(createContactItem("\uf0e0", "Email", "mailto:" + data.contactEmail(), solidIconFont));
            }
            // Website
            if (profile.getWebsiteUrl() != null && !profile.getWebsiteUrl().isBlank()) {
                contactItems.add(createContactItem("\uf0c1", "Website", profile.getWebsiteUrl(), solidIconFont));
            }
            // LinkedIn
            if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) {
                contactItems.add(createContactItem("\uf08c", "LinkedIn", profile.getLinkedinUrl(), brandsIconFont));
            }
            // GitHub
            if (profile.getGithubUrl() != null && !profile.getGithubUrl().isBlank()) {
                contactItems.add(createContactItem("\uf09b", "GitHub", profile.getGithubUrl(), brandsIconFont));
            }
        }

        if (!contactItems.isEmpty()) {
            // Create a table with a dynamic number of columns for perfect spacing
            PdfPTable contactTable = new PdfPTable(contactItems.size());
            contactTable.setWidthPercentage(100);
            for (Paragraph item : contactItems) {
                PdfPCell cell = new PdfPCell();
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(2);
                cell.addElement(item);
                contactTable.addCell(cell);
            }
            doc.add(contactTable);
        }

        // Separator (previously a border-bottom paragraph)
        Paragraph separator = new Paragraph(0);
        separator.add(new Chunk(new LineSeparator(1f, 100, ACCENT_COLOR, Element.ALIGN_CENTER, 0)));
        separator.setSpacingBefore(15);
        doc.add(separator);
    }

    private Paragraph createContactItem(String iconChar, String text, String link, BaseFont iconFont) {
        Paragraph p = new Paragraph(10);
        p.setAlignment(Element.ALIGN_CENTER);
        if (iconFont != null && iconFont.charExists(iconChar.codePointAt(0))) {
            p.add(new Chunk(iconChar + " ", new Font(iconFont, DEFAULT_SIZE, Font.NORMAL, ACCENT_COLOR)));
        }
        // Make the text a styled, clickable link
        Chunk linkChunk = new Chunk(text, new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, ACCENT_COLOR));
        linkChunk.setAnchor(link);
        linkChunk.setUnderline(0.7f, -2); // subtle underline
        p.add(linkChunk);
        return p;
    }

    private void createSection(Document doc, String title, String content) throws DocumentException {
        if (content == null || content.isBlank()) return;
        Paragraph heading = new Paragraph(title.toUpperCase(), new Font(headingFont, 14, Font.NORMAL, ACCENT_COLOR));
        heading.setSpacingBefore(20);
        heading.setSpacingAfter(5);
        doc.add(heading);
        Paragraph body = new Paragraph(content, new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, TEXT_COLOR));
        body.setLeading(0, 1.2f);
        doc.add(body);
    }

    /**
     * Phase 8.5 (DEF-4): testimonials block for this legacy template — same
     * semantics as {@link TemplateSupport#renderTestimonialsSection}: the
     * selection engine has already applied visibility/selection; BRIEF caps
     * at three; empty/absent testimonials skip cleanly.
     */
    private void createTestimonialsSection(Document doc, PortfolioData data, boolean brief) throws DocumentException {
        if (data.testimonials() == null || data.testimonials().isEmpty()) {
            return;
        }
        List<Testimonial> testimonials = brief
                ? data.testimonials().stream().limit(3).collect(Collectors.toList())
                : data.testimonials();
        if (testimonials.isEmpty()) {
            return;
        }
        Paragraph heading = new Paragraph("TESTIMONIALS", new Font(headingFont, 14, Font.NORMAL, ACCENT_COLOR));
        heading.setSpacingBefore(20);
        heading.setSpacingAfter(5);
        doc.add(heading);
        for (Testimonial testimonial : testimonials) {
            if (testimonial == null || testimonial.getQuote() == null
                    || testimonial.getQuote().isBlank()) {
                continue;
            }
            Paragraph quote = new Paragraph("\u201C" + testimonial.getQuote() + "\u201D",
                    new Font(bodyItalicFont, 10, Font.NORMAL, TEXT_COLOR));
            quote.setLeading(0, 1.2f);
            quote.setIndentationLeft(12);
            quote.setSpacingAfter(1);
            doc.add(quote);
            String attribution = testimonial.getAuthorName() == null
                    ? "" : testimonial.getAuthorName();
            if (testimonial.getAuthorTitle() != null && !testimonial.getAuthorTitle().isBlank()) {
                attribution += (attribution.isBlank() ? "" : ", ") + testimonial.getAuthorTitle();
            }
            if (!attribution.isBlank()) {
                Paragraph attributionLine = new Paragraph("— " + attribution,
                        new Font(bodyFont, 9, Font.NORMAL, ACCENT_COLOR));
                attributionLine.setIndentationLeft(12);
                attributionLine.setSpacingAfter(6);
                doc.add(attributionLine);
            }
        }
    }

    private void createSkillsSection(Document doc, List<Skill> skills) throws DocumentException {
        if (skills.isEmpty()) return;
        Paragraph heading = new Paragraph("SKILLS", new Font(headingFont, 14, Font.NORMAL, ACCENT_COLOR));
        heading.setSpacingBefore(20);
        heading.setSpacingAfter(10);
        doc.add(heading);

        Paragraph skillParagraph = new Paragraph(10);
        skills.sort(Comparator.comparing(Skill::getLevel).reversed().thenComparing(Skill::getName));

        for (Skill skill : skills) {
            Color pillColor = switch (skill.getLevel()) {
                case EXPERT -> PdfGenerationService.SKILL_EXPERT_COLOR;
                case INTERMEDIATE -> PdfGenerationService.SKILL_INTERMEDIATE_COLOR;
                case ADVANCED -> PdfGenerationService.SKILL_ADVANCED_COLOR;
                case BEGINNER -> PdfGenerationService.SKILL_BEGINNER_COLOR;
            };
            // White text on the colored pills still provides good contrast.
            Chunk skillPill = new Chunk(" " + skill.getName() + " ",
                    new Font(bodyFont, 9, Font.NORMAL, Color.WHITE));
            skillPill.setBackground(pillColor);
            skillParagraph.add(skillPill);
            skillParagraph.add(new Chunk("  ", new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, TEXT_COLOR)));
        }
        doc.add(skillParagraph);
    }

    /**
     * Phase 8.3: in BRIEF mode the long-form description is omitted and the
     * achievement list is capped at three.
     */
    private void createExperienceSection(Document doc, List<Experience> experiences, boolean brief)
            throws DocumentException {
        if (experiences.isEmpty()) return;
        Paragraph heading = new Paragraph("EXPERIENCE", new Font(headingFont, 14, Font.NORMAL, ACCENT_COLOR));
        heading.setSpacingBefore(20);
        heading.setSpacingAfter(5);
        doc.add(heading);

        experiences.sort(Comparator.comparing(Experience::getStartDate).reversed());
        for (Experience exp : experiences) {
            String dateText = exp.getStartDate().format(DATE_FORMATTER) + " - " +
                    (exp.getEndDate() == null ? "Present" : exp.getEndDate().format(DATE_FORMATTER));

            PdfPTable itemHeader = new PdfPTable(new float[]{3, 1});
            itemHeader.setWidthPercentage(100);
            PdfPCell titleCell = new PdfPCell(new Paragraph(exp.getJobTitle(),
                    new Font(bodyFont, 12, Font.BOLD, TEXT_COLOR)));
            titleCell.setBorder(Rectangle.NO_BORDER);
            itemHeader.addCell(titleCell);
            PdfPCell dateCell = new PdfPCell(new Paragraph(dateText,
                    new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, TEXT_COLOR)));
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            itemHeader.addCell(dateCell);
            itemHeader.setSpacingAfter(0);
            doc.add(itemHeader);

            Paragraph companyLine = new Paragraph(
                    exp.getCompanyName() + (exp.getLocation() != null ? ", " + exp.getLocation() : ""),
                    new Font(bodyItalicFont, 10, Font.NORMAL, SUBTLE_COLOR));
            companyLine.setSpacingBefore(0);
            companyLine.setLeading(0, 1.4f);
            doc.add(companyLine);

            List<String> achievements = brief
                    ? achievementLines(exp).stream().limit(3).collect(Collectors.toList())
                    : achievementLines(exp);
            if (!brief && exp.getDescription() != null) {
                Paragraph description = new Paragraph(exp.getDescription(),
                        new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, TEXT_COLOR));
                description.setSpacingBefore(5);
                description.setLeading(0, 1.2f);
                // trailing gap only needed when there is no achievements block below
                description.setSpacingAfter(achievements.isEmpty() ? 15 : 2);
                doc.add(description);
            }

            // Phase 7.4 (§7): render key achievements (previously missing from
            // this template; the Markdown export always included them).
            if (!achievements.isEmpty()) {
                Paragraph achievementsHeading = new Paragraph("Key Achievements",
                        new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR));
                achievementsHeading.setIndentationLeft(10);
                achievementsHeading.setSpacingAfter(1);
                doc.add(achievementsHeading);
                for (String achievement : achievements) {
                    Paragraph bullet = new Paragraph("– " + achievement,
                            new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
                    bullet.setIndentationLeft(24);
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

    /**
     * Phase 8.3: in BRIEF mode the long-form description is omitted and the
     * key-feature list is capped at three.
     */
    private void createProjectsSection(Document doc, List<Project> projects, boolean brief)
            throws DocumentException {
        if (projects.isEmpty()) return;
        Paragraph heading = new Paragraph("PROJECTS", new Font(headingFont, 14, Font.NORMAL, ACCENT_COLOR));
        heading.setSpacingBefore(20);
        heading.setSpacingAfter(5);
        doc.add(heading);

        for (Project proj : projects) {
            String projectUrl = proj.getLiveUrl() != null ? proj.getLiveUrl() : proj.getRepoUrl();
            boolean linked = projectUrl != null && !projectUrl.isBlank();
            Chunk titleChunk = new Chunk(proj.getTitle(),
                    new Font(bodyFont, 12, Font.BOLD, linked ? ACCENT_COLOR : TEXT_COLOR));
            if (linked) {
                titleChunk.setAnchor(projectUrl);
                titleChunk.setUnderline(1f, -2);
            }
            doc.add(new Paragraph(titleChunk));

            if (proj.getSkills() != null && !proj.getSkills().isEmpty()) {
                String techStackString = proj.getSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.joining(" • "));
                Paragraph techStack = new Paragraph(techStackString,
                        new Font(bodyItalicFont, 10, Font.NORMAL, SUBTLE_COLOR));
                techStack.setSpacingBefore(0);
                techStack.setLeading(0, 1.4f);
                doc.add(techStack);
            }
            List<String> keyFeatures = brief
                    ? keyFeatureLines(proj).stream().limit(3).collect(Collectors.toList())
                    : keyFeatureLines(proj);
            if (!brief && proj.getDescription() != null) {
                Paragraph description = new Paragraph(proj.getDescription(),
                        new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, TEXT_COLOR));
                description.setSpacingBefore(5);
                description.setLeading(0, 1.2f);
                // trailing gap only needed when there is no key-features block below
                description.setSpacingAfter(keyFeatures.isEmpty() ? 15 : 2);
                doc.add(description);
            }

            // Phase 7.4 (§6): structured key features (none for legacy projects)
            if (!keyFeatures.isEmpty()) {
                Paragraph featuresHeading = new Paragraph("Key Features",
                        new Font(bodyFont, 10, Font.BOLD, TEXT_COLOR));
                featuresHeading.setIndentationLeft(10);
                featuresHeading.setSpacingAfter(1);
                doc.add(featuresHeading);
                for (String feature : keyFeatures) {
                    Paragraph bullet = new Paragraph("– " + feature,
                            new Font(bodyFont, 10, Font.NORMAL, TEXT_COLOR));
                    bullet.setIndentationLeft(24);
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

    /** Phase 8.3: in BRIEF mode the grade line is omitted. */
    private void createQualificationsSection(Document doc, List<Qualification> qualifications, boolean brief)
            throws DocumentException {
        if (qualifications.isEmpty()) return;
        Paragraph heading = new Paragraph("QUALIFICATIONS", new Font(headingFont, 14, Font.NORMAL, ACCENT_COLOR));
        heading.setSpacingBefore(20);
        heading.setSpacingAfter(5);
        doc.add(heading);

        qualifications.sort(Comparator.comparing(Qualification::getCompletionYear).reversed());
        for (Qualification qual : qualifications) {
            PdfPTable itemHeader = new PdfPTable(new float[]{3, 1});
            itemHeader.setWidthPercentage(100);
            PdfPCell nameCell = new PdfPCell(new Paragraph(qual.getQualificationName(),
                    new Font(bodyFont, 12, Font.BOLD, TEXT_COLOR)));
            nameCell.setBorder(Rectangle.NO_BORDER);
            itemHeader.addCell(nameCell);
            PdfPCell yearCell = new PdfPCell(new Paragraph(String.valueOf(qual.getCompletionYear()),
                    new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, TEXT_COLOR)));
            yearCell.setBorder(Rectangle.NO_BORDER);
            yearCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            itemHeader.addCell(yearCell);
            itemHeader.setSpacingAfter(0);
            doc.add(itemHeader);

            Paragraph institution = new Paragraph(qual.getInstitutionName(),
                    new Font(bodyItalicFont, 10, Font.NORMAL, SUBTLE_COLOR));
            institution.setSpacingBefore(0);
            institution.setLeading(0, 1.4f);
            doc.add(institution);

            if (!brief && qual.getGrade() != null && !qual.getGrade().isBlank()) {
                Paragraph grade = new Paragraph(qual.getGrade(),
                        new Font(bodyFont, DEFAULT_SIZE, Font.NORMAL, TEXT_COLOR));
                grade.setSpacingBefore(5);
                grade.setSpacingAfter(15);
                grade.setLeading(0, 1.2f);
                doc.add(grade);
            } else {
                Paragraph gap = new Paragraph(" ");
                gap.setSpacingAfter(15); // Add space even if no grade
                doc.add(gap);
            }
        }
    }

    private static BaseFont loadFontFromResource(String resourcePath) throws IOException, DocumentException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Font resource not found on classpath: " + resourcePath);
            }
            return BaseFont.createFont(resourcePath, BaseFont.WINANSI, BaseFont.EMBEDDED, true, is.readAllBytes(), null);
        }
    }
}
