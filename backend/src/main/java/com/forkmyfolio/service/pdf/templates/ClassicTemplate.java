package com.forkmyfolio.service.pdf.templates;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ExportMode;
import com.forkmyfolio.service.impl.PdfGenerationService;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A classic, professional, single-column resume template using serif fonts.
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): section rules are
 * {@link LineSeparator}s (previously border-top divs), the title/date rows
 * are {@link PdfPTable}s, and fonts are {@link BaseFont} programs created
 * once in the constructor. Multi-line paragraphs without an explicit leading
 * in the previous engine now carry an explicit 1.4× leading (the previous
 * engine's default; OpenPDF's default is 1.5×).
 */
public class ClassicTemplate implements PortfolioPdfTemplate {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final Color BLACK = Color.BLACK;

    private final BaseFont headingFont;
    private final BaseFont bodyFont;

    public ClassicTemplate() throws IOException, DocumentException {
        this.headingFont = BaseFont.createFont(BaseFont.TIMES_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        this.bodyFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    @Override
    public float[] getMargins() {
        // Previous engine: setMargins(50, 50, 50, 50)
        return new float[]{50, 50, 50, 50}; // {left, right, top, bottom}
    }

    @Override
    public void generate(Document document, PdfGenerationService.PdfContext ctx, PortfolioData data)
            throws IOException, DocumentException {

        // 1. Header
        User user = data.profile().getUser();
        Paragraph nameLine = new Paragraph(user.getFirstName() + " " + user.getLastName(),
                new Font(headingFont, 24, Font.NORMAL, BLACK));
        nameLine.setAlignment(Element.ALIGN_CENTER);
        nameLine.setSpacingAfter(2);
        document.add(nameLine);

        // Phase 7.4 (§9): headline was never rendered by this template while
        // every other template shows it.
        String headline = data.profile().getHeadline();
        if (headline != null && !headline.isBlank()) {
            Paragraph headlineLine = new Paragraph(headline, new Font(bodyFont, 12, Font.NORMAL, BLACK));
            headlineLine.setAlignment(Element.ALIGN_CENTER);
            headlineLine.setSpacingAfter(2);
            document.add(headlineLine);
        }

        // Phase 8.3: the concise-resume flag resolved by the shared
        // selection engine (owner preference; PUBLIC exports are always FULL).
        boolean brief = data.exportMode() == ExportMode.BRIEF;

        // Phase 7.4 (§9): String.join over null Linked/GitHub fields rendered
        // literal "null" strings — guard blanks like the other templates do.
        // Phase 8.3: the contact block honours the owner's contact toggle;
        // the email shown is the export data's contactEmail (publicEmail for
        // public exports; publicEmail-or-account-email for owner exports).
        String contactInfo = data.includeContact() ? java.util.stream.Stream.of(
                data.contactEmail(),
                data.profile().getLinkedinUrl(),
                data.profile().getGithubUrl()
        ).filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" | ")) : "";
        if (!contactInfo.isBlank()) {
            Paragraph contactLine = new Paragraph(contactInfo, new Font(bodyFont, 10, Font.NORMAL, BLACK));
            contactLine.setAlignment(Element.ALIGN_CENTER);
            contactLine.setLeading(0, 1.4f);
            document.add(contactLine);
        }

        // 2. Summary
        // Phase 8 (PRV-1): summary honours the profile's public section
        // toggle for public exports.
        if (data.includeSummary() && data.profile().getSummary() != null && !data.profile().getSummary().isBlank()) {
            createSection(document, "Summary");
            Paragraph summary = new Paragraph(data.profile().getSummary(), new Font(bodyFont, 10, Font.NORMAL, BLACK));
            summary.setLeading(0, 1.4f);
            summary.setSpacingAfter(10);
            document.add(summary);
        }

        // 3. Experience
        createSection(document, "Experience");
        data.experiences().sort(Comparator.comparing(Experience::getStartDate).reversed());
        for (Experience exp : data.experiences()) {
            String dateStr = exp.getStartDate().format(DATE_FORMATTER) + " – " +
                    (exp.getEndDate() == null ? "Present" : exp.getEndDate().format(DATE_FORMATTER));
            // Phase 8.3: BRIEF mode omits the long-form description.
            createEntry(document, exp.getJobTitle(), dateStr, exp.getCompanyName() + ", " + exp.getLocation(),
                    brief ? null : exp.getDescription());
            // Phase 7.4 (§7): render key achievements (previously missing from
            // this template; the Markdown export always included them).
            List<String> achievements = brief
                    ? achievementLines(exp).stream().limit(3).collect(Collectors.toList())
                    : achievementLines(exp);
            if (!achievements.isEmpty()) {
                Paragraph achievementsTitle = new Paragraph("Key Achievements",
                        new Font(bodyFont, 10, Font.BOLD, BLACK));
                achievementsTitle.setSpacingBefore(2);
                achievementsTitle.setIndentationLeft(15);
                document.add(achievementsTitle);
                for (String achievement : achievements) {
                    Paragraph bullet = new Paragraph("– " + achievement, new Font(bodyFont, 10, Font.NORMAL, BLACK));
                    bullet.setIndentationLeft(25);
                    bullet.setLeading(0, 1.4f);
                    bullet.setSpacingAfter(2);
                    document.add(bullet);
                }
            }
        }

        // 4. Projects
        createSection(document, "Projects");
        for (Project proj : data.projects()) {
            createEntry(document, proj.getTitle(), proj.getSkills().stream().map(Skill::getName).collect(Collectors.joining(", ")), null,
                    brief ? null : proj.getDescription());
            // Phase 7.4 (§6): structured key features (none for legacy projects)
            List<String> keyFeatures = brief
                    ? keyFeatureLines(proj).stream().limit(3).collect(Collectors.toList())
                    : keyFeatureLines(proj);
            if (!keyFeatures.isEmpty()) {
                Paragraph featuresTitle = new Paragraph("Key Features", new Font(bodyFont, 10, Font.BOLD, BLACK));
                featuresTitle.setSpacingBefore(2);
                featuresTitle.setIndentationLeft(15);
                document.add(featuresTitle);
                for (String feature : keyFeatures) {
                    Paragraph bullet = new Paragraph("– " + feature, new Font(bodyFont, 10, Font.NORMAL, BLACK));
                    bullet.setIndentationLeft(25);
                    bullet.setLeading(0, 1.4f);
                    bullet.setSpacingAfter(2);
                    document.add(bullet);
                }
            }
        }

        // 4.1 Skills (Phase 7.4 §8 parity: Markdown always had this section;
        // the template omitted it entirely)
        if (!skillsGroupedByCategory(data.skills()).isEmpty()) {
            createSection(document, "Skills");
            skillsGroupedByCategory(data.skills()).forEach((category, categorySkills) -> {
                try {
                    String names = categorySkills.stream().map(Skill::getName).collect(Collectors.joining(", "));
                    Paragraph categoryLine = new Paragraph(category, new Font(bodyFont, 10, Font.ITALIC, BLACK));
                    categoryLine.setSpacingBefore(2);
                    document.add(categoryLine);
                    Paragraph namesLine = new Paragraph(names, new Font(bodyFont, 9, Font.NORMAL, BLACK));
                    namesLine.setLeading(0, 1.4f);
                    document.add(namesLine);
                } catch (DocumentException e) {
                    throw new IllegalStateException("PDF rendering failed", e);
                }
            });
        }

        // 5. Education
        createSection(document, "Education");
        data.qualifications().sort(Comparator.comparing(Qualification::getCompletionYear).reversed());
        for (Qualification qual : data.qualifications()) {
            // Phase 8.3: BRIEF mode omits the grade line.
            createEntry(document, qual.getQualificationName(), String.valueOf(qual.getCompletionYear()),
                    qual.getInstitutionName(), brief ? null : qual.getGrade());
        }

        // 6. Testimonials (Phase 8.5 DEF-4: legacy-template parity — this
        // template remained selectable but silently omitted testimonials).
        // Same semantics as TemplateSupport.renderTestimonialsSection: the
        // selection engine has already applied visibility/selection; BRIEF
        // caps at three; empty/absent testimonials skip cleanly.
        if (data.testimonials() != null && !data.testimonials().isEmpty()) {
            List<Testimonial> testimonials = brief
                    ? data.testimonials().stream().limit(3).collect(Collectors.toList())
                    : data.testimonials();
            if (!testimonials.isEmpty()) {
                createSection(document, "Testimonials");
                for (Testimonial testimonial : testimonials) {
                    if (testimonial == null || testimonial.getQuote() == null
                            || testimonial.getQuote().isBlank()) {
                        continue;
                    }
                    Paragraph quote = new Paragraph("\u201C" + testimonial.getQuote() + "\u201D",
                            new Font(bodyFont, 10, Font.NORMAL, BLACK));
                    quote.setIndentationLeft(15);
                    quote.setLeading(0, 1.4f);
                    quote.setSpacingAfter(1);
                    document.add(quote);
                    String attribution = testimonial.getAuthorName() == null
                            ? "" : testimonial.getAuthorName();
                    if (testimonial.getAuthorTitle() != null && !testimonial.getAuthorTitle().isBlank()) {
                        attribution += (attribution.isBlank() ? "" : ", ") + testimonial.getAuthorTitle();
                    }
                    if (!attribution.isBlank()) {
                        Paragraph attributionLine = new Paragraph("— " + attribution,
                                new Font(bodyFont, 9, Font.ITALIC, BLACK));
                        attributionLine.setIndentationLeft(25);
                        attributionLine.setLeading(0, 1.4f);
                        attributionLine.setSpacingAfter(6);
                        document.add(attributionLine);
                    }
                }
            }
        }
    }

    private void createSection(Document doc, String title) throws DocumentException {
        // Previously a border-top div; now a full-width thin rule.
        Paragraph rule = new Paragraph(0);
        rule.add(new org.openpdf.text.Chunk(new LineSeparator(0.5f, 100, BLACK, Element.ALIGN_CENTER, 0)));
        rule.setSpacingBefore(10);
        rule.setSpacingAfter(5);
        doc.add(rule);
        Paragraph heading = new Paragraph(title.toUpperCase(), new Font(headingFont, 12, Font.NORMAL, BLACK));
        doc.add(heading);
    }

    private void createEntry(Document doc, String title, String date, String subtitle, String description)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{70, 30});
        table.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell(new Paragraph(title, new Font(bodyFont, 10, Font.BOLD, BLACK)));
        titleCell.setBorder(org.openpdf.text.Rectangle.NO_BORDER);
        titleCell.setPadding(0);
        table.addCell(titleCell);

        PdfPCell dateCell = new PdfPCell(new Paragraph(date, new Font(bodyFont, 10, Font.NORMAL, BLACK)));
        dateCell.setBorder(org.openpdf.text.Rectangle.NO_BORDER);
        dateCell.setPadding(0);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(dateCell);

        table.setSpacingAfter(0);
        doc.add(table);

        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph subtitleLine = new Paragraph(subtitle, new Font(bodyFont, 10, Font.ITALIC, BLACK));
            subtitleLine.setSpacingBefore(0);
            subtitleLine.setSpacingAfter(5);
            subtitleLine.setLeading(0, 1.4f);
            doc.add(subtitleLine);
        }

        if (description != null && !description.isBlank()) {
            Paragraph descriptionLine = new Paragraph(description, new Font(bodyFont, 10, Font.NORMAL, BLACK));
            descriptionLine.setIndentationLeft(15);
            descriptionLine.setLeading(0, 1.4f);
            descriptionLine.setSpacingAfter(10);
            doc.add(descriptionLine);
        }
    }
}
