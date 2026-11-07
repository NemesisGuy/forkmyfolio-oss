package com.forkmyfolio.service.pdf.templates;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.KeyFeature;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ExportMode;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 8 (8.2): shared, THEME-PARAMETERISED block library for PDF
 * templates. New templates compose their layouts from these blocks and
 * inject a {@link Theme} (fonts + colours + heading style) to keep a
 * distinct visual identity — the RENDERING LOGIC itself exists exactly
 * once here.
 *
 * <p>
 * Design rules encoded once, for every template that uses this library:
 * <ul>
 *   <li>All light-background, print-safe rendering — no canvas-wide
 *       background fills (the defect that retired Metro Dark).</li>
 *   <li>PRV-1: the summary block honours {@link PortfolioData#includeSummary()};
 *       contact lines render {@code publicEmail} (from PortfolioData) —
 *       never the account email.</li>
 *   <li>Phase 7.4 parity: achievements and structured key features render
 *       as bullets; skills group by category (uncategorised → "Other
 *       Skills").</li>
 *   <li>Defensive rendering: empty optional sections skip cleanly; null
 *       dates/years/URLs never produce literal "null" text or NPEs.</li>
 * </ul>
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): ported from the previous layout engine.
 * Rule lines are drawn with {@link LineSeparator} (the previous engine's
 * 1pt background-filled divs and border-top divs have no direct equivalent);
 * page numbers are rendered through a {@link PdfPageEventHelper} registered
 * by the service before the document opens; fonts are carried as
 * {@link BaseFont} programs and sized/styled per use.
 *
 * <p>
 * Adheres to NAG-01 (DBR): pure rendering utility, no entity mutation.
 */
public final class TemplateSupport {

    /** Private utility class — never instantiated. */
    private TemplateSupport() {
    }

    /**
     * Theme parameters a template injects. Fonts come from the template
     * (standard font programs or classpath resources); colours are
     * template-owned.
     */
    public record Theme(
            BaseFont nameFont,
            BaseFont headlineFont,
            BaseFont sectionFont,
            BaseFont itemTitleFont,
            BaseFont itemSubtitleFont,
            BaseFont bodyFont,
            BaseFont dateFont,
            Color textColor,
            Color subtleColor,
            Color accentColor,
            boolean upperCaseHeadings,
            float bodyLeading) {
    }

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final DateTimeFormatter YEAR_ONLY = DateTimeFormatter.ofPattern("yyyy");

    // ------------------------------------------------------------------
    // Shared content helpers (single source of truth; the template
    // interface defaults delegate here so legacy templates and new
    // templates can never drift).
    // ------------------------------------------------------------------

    /**
     * Groups skills into category sections in encounter order; skills
     * without a category pool under "Other Skills" (mirrors the Markdown
     * export).
     */
    public static Map<String, List<Skill>> groupSkillsByCategory(List<Skill> skills) {
        Map<String, List<Skill>> grouped = new LinkedHashMap<>();
        if (skills == null) {
            return grouped;
        }
        for (Skill skill : skills) {
            if (skill == null || skill.getName() == null || skill.getName().isBlank()) {
                continue;
            }
            String category = (skill.getCategory() == null || skill.getCategory().isBlank())
                    ? "Other Skills"
                    : skill.getCategory();
            grouped.computeIfAbsent(category, key -> new ArrayList<>()).add(skill);
        }
        return grouped;
    }

    /**
     * A project's structured key-feature values in stable display order
     * (empty for legacy projects whose bullets live in the description).
     */
    public static List<String> keyFeatureValues(Project project) {
        if (project == null || project.getKeyFeatures() == null) {
            return List.of();
        }
        return project.getKeyFeatures().stream()
                .filter(feature -> feature != null && feature.getValue() != null
                        && !feature.getValue().isBlank())
                .map(KeyFeature::getValue)
                .toList();
    }

    /**
     * An experience's achievements split into trimmed, non-blank lines
     * (one bullet per line, exactly like the Markdown export).
     */
    public static List<String> achievementValues(Experience experience) {
        if (experience == null || experience.getAchievements() == null) {
            return List.of();
        }
        return Arrays.stream(experience.getAchievements().split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    /**
     * Phase 8.3: an experience's achievements for the given export mode —
     * BRIEF caps the bullet list at the first three entries so a concise
     * resume stays concise (FULL renders every achievement).
     */
    public static List<String> achievementValues(Experience experience, ExportMode mode) {
        List<String> all = achievementValues(experience);
        return mode == ExportMode.BRIEF ? all.stream().limit(3).collect(Collectors.toList()) : all;
    }

    /**
     * A project's structured key-feature values for the given export mode —
     * BRIEF caps the list at the first three entries (FULL renders all).
     */
    public static List<String> keyFeatureValues(Project project, ExportMode mode) {
        List<String> all = keyFeatureValues(project);
        return mode == ExportMode.BRIEF ? all.stream().limit(3).collect(Collectors.toList()) : all;
    }

    // ------------------------------------------------------------------
    // Font composition helpers (OpenPDF fonts are sized/styled per use)
    // ------------------------------------------------------------------

    /** A sized, styled, coloured font over the theme's base font program. */
    public static Font font(BaseFont base, float size, int style, Color color) {
        return new Font(base, size, style, color);
    }

    /** A plain (unstyled) font over the theme's base font program. */
    public static Font font(BaseFont base, float size, Color color) {
        return new Font(base, size, Font.NORMAL, color);
    }

    /** A bold font over the theme's base font program. */
    public static Font boldFont(BaseFont base, float size, Color color) {
        return new Font(base, size, Font.BOLD, color);
    }

    // ------------------------------------------------------------------
    // Blocks
    // ------------------------------------------------------------------

    /**
     * Header block: name, headline, one-line contact row (publicEmail •
     * website • linkedin • github — blanks skipped, URLs clickable and
     * printed as visible text so exports stay useful on paper).
     */
    public static void renderHeader(Document doc, Theme theme, PortfolioData data)
            throws DocumentException {
        User user = data.profile().getUser();
        String name = (user != null ? user.getFirstName() + " " + user.getLastName() : "").trim();
        String displayName = theme.upperCaseHeadings() ? name.toUpperCase() : name;

        // Phase 0.12: leading must be multiplied (1.4×), never fixed-zero —
        // a fixed leading of 0 collapses the line height in OpenPDF and the
        // next element overlaps the text.
        Paragraph nameLine = new Paragraph(new Chunk(displayName,
                font(theme.nameFont(), 26, theme.textColor())));
        nameLine.setAlignment(Element.ALIGN_CENTER);
        nameLine.setLeading(0, 1.4f);
        nameLine.setSpacingAfter(2);
        doc.add(nameLine);

        String headline = data.profile().getHeadline();
        Paragraph headlineLine;
        if (headline != null && !headline.isBlank()) {
            headlineLine = new Paragraph(new Chunk(headline,
                    font(theme.headlineFont(), 13, theme.accentColor())));
        } else {
            headlineLine = new Paragraph(new Chunk(" ", font(theme.bodyFont(), 13, theme.accentColor())));
        }
        headlineLine.setAlignment(Element.ALIGN_CENTER);
        headlineLine.setLeading(0, 1.4f);
        headlineLine.setSpacingAfter(8);
        doc.add(headlineLine);

        Paragraph contact = buildContactLine(theme, data);
        if (contact != null) {
            doc.add(contact);
        }
    }

    /**
     * Optional profile image block (best effort): renders the profile
     * image centred at the given width; silently skips on null/blank or
     * unloadable URLs so a broken image host can never abort an export.
     */
    public static void renderProfileImage(Document doc, PortfolioData data, float widthPt)
            throws DocumentException {
        String imageUrl = data.profileImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            Image image = Image.getInstance(URI.create(imageUrl).toURL());
            image.scaleAbsolute(widthPt, widthPt);
            Paragraph holder = new Paragraph(0);
            holder.add(image);
            holder.setAlignment(Element.ALIGN_CENTER);
            holder.setSpacingAfter(12);
            doc.add(holder);
        } catch (Exception e) {
            // Best effort by design: an unreachable image host must never
            // fail the whole export.
        }
    }

    /** Section heading block: label + thin accent rule underneath. */
    public static void renderSectionHeader(Document doc, Theme theme, String title)
            throws DocumentException {
        String label = theme.upperCaseHeadings() ? title.toUpperCase() : title;
        Paragraph heading = new Paragraph(new Chunk(label,
                font(theme.sectionFont(), 12, theme.accentColor())));
        heading.setLeading(0, 1.4f);
        heading.setSpacingBefore(14);
        heading.setSpacingAfter(4);
        doc.add(heading);
        doc.add(ruleLine(theme.accentColor(), 1f, 8));
    }

    /**
     * Summary/About block. PRV-1: honours the profile's own public section
     * toggle via {@link PortfolioData#includeSummary()} — public exports
     * omit the summary exactly when the public portfolio pages do.
     */
    public static void renderSummary(Document doc, Theme theme, PortfolioData data)
            throws DocumentException {
        if (!data.includeSummary()) {
            return;
        }
        String summary = data.profile().getSummary();
        if (summary == null || summary.isBlank()) {
            return;
        }
        renderSectionHeader(doc, theme, "Summary");
        Paragraph summaryLine = new Paragraph(summary, font(theme.bodyFont(), 10, theme.textColor()));
        summaryLine.setLeading(0, theme.bodyLeading());
        summaryLine.setSpacingAfter(6);
        doc.add(summaryLine);
    }

    /**
     * Experience block: reverse-chronological entries + achievement bullets.
     * Phase 8.3: in BRIEF mode the long-form description is omitted and the
     * achievement list is capped at three, producing a concise resume.
     */
    public static void renderExperienceSection(Document doc, Theme theme, PortfolioData data)
            throws DocumentException {
        if (data.experiences() == null || data.experiences().isEmpty()) {
            return;
        }
        boolean brief = data.exportMode() == ExportMode.BRIEF;
        renderSectionHeader(doc, theme, "Experience");
        List<Experience> experiences = new ArrayList<>(data.experiences());
        experiences.sort(Comparator.comparing(
                Experience::getStartDate,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        for (Experience exp : experiences) {
            Paragraph titleLine = new Paragraph();
            titleLine.setLeading(0, 1.4f);
            titleLine.add(new Chunk(nullToBlank(exp.getJobTitle()),
                    font(theme.itemTitleFont(), 11, theme.textColor())));
            titleLine.add(new Chunk("  |  " + formatRange(
                            exp.getStartDate() == null ? null : MONTH_YEAR.format(exp.getStartDate()),
                            exp.getEndDate() == null ? "Present" : MONTH_YEAR.format(exp.getEndDate())),
                    font(theme.dateFont(), 9, theme.subtleColor())));
            doc.add(titleLine);

            String company = nullToBlank(exp.getCompanyName());
            if (exp.getLocation() != null && !exp.getLocation().isBlank()) {
                company = company.isBlank() ? exp.getLocation() : company + ", " + exp.getLocation();
            }
            if (!company.isBlank()) {
                Paragraph companyLine = new Paragraph(company,
                        font(theme.itemSubtitleFont(), 10, theme.subtleColor()));
                companyLine.setIndentationLeft(12);
                companyLine.setSpacingBefore(1);
                companyLine.setSpacingAfter(2);
                doc.add(companyLine);
            }

            // BRIEF mode: the long-form description is intentionally omitted
            // (never rewritten or truncated mid-sentence).
            if (!brief && exp.getDescription() != null && !exp.getDescription().isBlank()) {
                Paragraph description = new Paragraph(exp.getDescription(),
                        font(theme.bodyFont(), 10, theme.textColor()));
                description.setIndentationLeft(12);
                description.setSpacingBefore(3);
                description.setLeading(0, theme.bodyLeading());
                description.setSpacingAfter(achievementValues(exp).isEmpty() ? 10 : 2);
                doc.add(description);
            }

            List<String> achievements = achievementValues(exp, data.exportMode());
            if (!achievements.isEmpty()) {
                Paragraph achievementsHeading = new Paragraph("Key Achievements",
                        boldFont(theme.bodyFont(), 10, theme.subtleColor()));
                achievementsHeading.setIndentationLeft(12);
                achievementsHeading.setSpacingBefore(2);
                achievementsHeading.setSpacingAfter(1);
                doc.add(achievementsHeading);
                for (String achievement : achievements) {
                    Paragraph bullet = new Paragraph("–  " + achievement,
                            font(theme.bodyFont(), 10, theme.textColor()));
                    bullet.setIndentationLeft(24);
                    bullet.setLeading(0, theme.bodyLeading());
                    bullet.setSpacingAfter(1);
                    doc.add(bullet);
                }
                Paragraph gap = new Paragraph(" ");
                gap.setSpacingAfter(8); // gap before next entry
                doc.add(gap);
            }
        }
    }

    /**
     * Projects block: title (linked when URL present), tech stack, description, key features.
     * Phase 8.3: in BRIEF mode the long-form description is omitted and the
     * key-feature list is capped at three.
     */
    public static void renderProjectsSection(Document doc, Theme theme, PortfolioData data)
            throws DocumentException {
        if (data.projects() == null || data.projects().isEmpty()) {
            return;
        }
        boolean brief = data.exportMode() == ExportMode.BRIEF;
        renderSectionHeader(doc, theme, "Projects");
        for (Project proj : data.projects()) {
            String projectUrl = primaryUrl(proj);
            boolean linked = projectUrl != null;
            Chunk titleChunk = new Chunk(nullToBlank(proj.getTitle()),
                    font(theme.itemTitleFont(), 11, linked ? theme.accentColor() : theme.textColor()));
            if (linked) {
                titleChunk.setUnderline(1f, -2);
                titleChunk.setAnchor(projectUrl);
            }
            Paragraph titleLine = new Paragraph(titleChunk);
            titleLine.setLeading(0, 1.4f);
            String techStack = proj.getSkills() == null ? "" : proj.getSkills().stream()
                    .filter(skill -> skill != null && skill.getName() != null)
                    .map(Skill::getName)
                    .collect(Collectors.joining(", "));
            if (!techStack.isBlank()) {
                titleLine.add(new Chunk("  |  " + techStack,
                        font(theme.itemSubtitleFont(), 9, theme.subtleColor())));
            }
            doc.add(titleLine);

            // BRIEF mode: the long-form description is intentionally omitted.
            if (!brief && proj.getDescription() != null && !proj.getDescription().isBlank()) {
                Paragraph description = new Paragraph(proj.getDescription(),
                        font(theme.bodyFont(), 10, theme.textColor()));
                description.setIndentationLeft(12);
                description.setSpacingBefore(3);
                description.setLeading(0, theme.bodyLeading());
                description.setSpacingAfter(keyFeatureValues(proj).isEmpty() ? 10 : 2);
                doc.add(description);
            }

            List<String> keyFeatures = keyFeatureValues(proj, data.exportMode());
            if (!keyFeatures.isEmpty()) {
                Paragraph featuresHeading = new Paragraph("Key Features",
                        boldFont(theme.bodyFont(), 10, theme.subtleColor()));
                featuresHeading.setIndentationLeft(12);
                featuresHeading.setSpacingBefore(2);
                featuresHeading.setSpacingAfter(1);
                doc.add(featuresHeading);
                for (String feature : keyFeatures) {
                    Paragraph bullet = new Paragraph("–  " + feature,
                            font(theme.bodyFont(), 10, theme.textColor()));
                    bullet.setIndentationLeft(24);
                    bullet.setLeading(0, theme.bodyLeading());
                    bullet.setSpacingAfter(1);
                    doc.add(bullet);
                }
                Paragraph gap = new Paragraph(" ");
                gap.setSpacingAfter(8); // gap before next entry
                doc.add(gap);
            }
        }
    }

    /**
     * Skills block: grouped by category (encounter order, uncategorised →
     * "Other Skills"), category label + comma-joined names.
     */
    public static void renderSkillsSection(Document doc, Theme theme, PortfolioData data)
            throws DocumentException {
        Map<String, List<Skill>> grouped = groupSkillsByCategory(data.skills());
        if (grouped.isEmpty()) {
            return;
        }
        renderSectionHeader(doc, theme, "Skills");
        grouped.forEach((category, categorySkills) -> {
            try {
                String names = categorySkills.stream().map(Skill::getName).collect(Collectors.joining(", "));
                Paragraph categoryLine = new Paragraph(category, boldFont(theme.bodyFont(), 10, theme.accentColor()));
                categoryLine.setSpacingAfter(0);
                doc.add(categoryLine);
                Paragraph namesLine = new Paragraph(names, font(theme.bodyFont(), 10, theme.textColor()));
                namesLine.setIndentationLeft(12);
                namesLine.setSpacingAfter(5);
                namesLine.setLeading(0, theme.bodyLeading());
                doc.add(namesLine);
            } catch (DocumentException e) {
                throw new IllegalStateException("PDF rendering failed", e);
            }
        });
    }

    /** Qualifications block: reverse-chronological by completion year (nulls last). */
    public static void renderQualificationsSection(Document doc, Theme theme, PortfolioData data)
            throws DocumentException {
        if (data.qualifications() == null || data.qualifications().isEmpty()) {
            return;
        }
        renderSectionHeader(doc, theme, "Education");
        List<Qualification> qualifications = new ArrayList<>(data.qualifications());
        qualifications.sort(Comparator.comparing(
                Qualification::getCompletionYear,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        for (Qualification qual : qualifications) {
            Paragraph titleLine = new Paragraph();
            titleLine.setLeading(0, 1.4f);
            titleLine.add(new Chunk(nullToBlank(qual.getQualificationName()),
                    font(theme.itemTitleFont(), 11, theme.textColor())));
            titleLine.add(new Chunk(qual.getCompletionYear() == null ? "" : "  |  " + qual.getCompletionYear(),
                    font(theme.dateFont(), 9, theme.subtleColor())));
            doc.add(titleLine);
            if (qual.getInstitutionName() != null && !qual.getInstitutionName().isBlank()) {
                Paragraph institutionLine = new Paragraph(qual.getInstitutionName(),
                        font(theme.itemSubtitleFont(), 10, theme.subtleColor()));
                institutionLine.setIndentationLeft(12);
                institutionLine.setSpacingBefore(1);
                institutionLine.setSpacingAfter(8);
                doc.add(institutionLine);
            }
        }
    }

    /**
     * Phase 8.3: testimonials block — quote, author and (optional) author
     * title. In BRIEF mode only the first three testimonials render so the
     * concise resume stays concise. Empty/absent testimonials skip cleanly.
     */
    public static void renderTestimonialsSection(Document doc, Theme theme, PortfolioData data)
            throws DocumentException {
        if (data.testimonials() == null || data.testimonials().isEmpty()) {
            return;
        }
        boolean brief = data.exportMode() == ExportMode.BRIEF;
        List<Testimonial> testimonials = brief
                ? data.testimonials().stream().limit(3).collect(Collectors.toList())
                : data.testimonials();
        if (testimonials.isEmpty()) {
            return;
        }
        renderSectionHeader(doc, theme, "Testimonials");
        for (Testimonial testimonial : testimonials) {
            if (testimonial == null || testimonial.getQuote() == null
                    || testimonial.getQuote().isBlank()) {
                continue;
            }
            Paragraph quoteLine = new Paragraph("\u201C" + testimonial.getQuote() + "\u201D",
                    font(theme.bodyFont(), 10, theme.textColor()));
            quoteLine.setIndentationLeft(12);
            quoteLine.setLeading(0, theme.bodyLeading());
            quoteLine.setSpacingAfter(1);
            doc.add(quoteLine);
            String attribution = nullToBlank(testimonial.getAuthorName());
            if (testimonial.getAuthorTitle() != null && !testimonial.getAuthorTitle().isBlank()) {
                attribution += ", " + testimonial.getAuthorTitle();
            }
            if (!attribution.isBlank()) {
                Paragraph attributionLine = new Paragraph("— " + attribution,
                        font(theme.itemSubtitleFont(), 9, theme.subtleColor()));
                attributionLine.setIndentationLeft(12);
                attributionLine.setSpacingAfter(6);
                doc.add(attributionLine);
            }
        }
    }

    /** Footer block: generator credit line. */
    public static void renderFooter(Document doc, Theme theme) throws DocumentException {
        Paragraph rule = new Paragraph(0);
        rule.add(new Chunk(new LineSeparator(0.5f, 100, theme.subtleColor(), Element.ALIGN_CENTER, 0)));
        rule.setSpacingBefore(16);
        doc.add(rule);
        // Forkability (Phase 8.5 amendment §1): no deployment-specific URL is
        // hardcoded here — a self-hosted instance must not emit another
        // organisation's domain in generated PDFs.
        Paragraph credit = new Paragraph("Generated with ForkMyFolio",
                font(theme.bodyFont(), 8, theme.subtleColor()));
        credit.setAlignment(Element.ALIGN_CENTER);
        credit.setSpacingBefore(5);
        doc.add(credit);
    }

    /**
     * Phase 0.12 (OpenPDF migration): the page-number renderer as a page
     * event. Adds a "Page N" number to every page (bottom-centre, inside
     * the bottom margin). Templates must reserve bottom margin ≥ 30pt.
     * Registered by the service before the document opens — the previous
     * engine allowed mid-render registration, OpenPDF does not.
     *
     * <p>
     * The x/y geometry replicates the previous rendering exactly:
     * left-aligned text at x = width/2 − 20, y = 18.
     */
    public static PdfPageEventHelper pageNumberEvent(BaseFont bodyFont, Color subtleColor) {
        return new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                int pageNumber = writer.getPageNumber();
                PdfContentByte canvas = writer.getDirectContent();
                canvas.saveState();
                canvas.beginText();
                canvas.setFontAndSize(bodyFont, 8);
                canvas.setRGBColorFill(subtleColor.getRed(), subtleColor.getGreen(), subtleColor.getBlue());
                float x = document.getPageSize().getWidth() / 2 - 20;
                float y = 18;
                canvas.moveText(x, y);
                canvas.showText("Page " + pageNumber);
                canvas.endText();
                canvas.restoreState();
            }
        };
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /** A full-width horizontal rule wrapped for document flow. */
    private static Paragraph ruleLine(Color color, float lineWidth, float spacingAfter) {
        Paragraph rule = new Paragraph(0);
        rule.add(new Chunk(new LineSeparator(lineWidth, 100, color, Element.ALIGN_CENTER, -2)));
        rule.setSpacingAfter(spacingAfter);
        return rule;
    }

    private static Paragraph buildContactLine(Theme theme, PortfolioData data) {
        // Phase 8.3: the contact block honours the owner's export contact
        // toggle (includeContact) and the email shown is the export data's
        // contactEmail — publicEmail for public exports, publicEmail-or-
        // account-email for owner exports (parity with the Markdown export;
        // the previous code read profile.publicEmail directly, which dropped
        // the owner fallback in PDFs).
        if (!data.includeContact()) {
            return null;
        }
        PortfolioProfile profile = data.profile();
        List<Chunk> parts = new ArrayList<>();
        if (data.contactEmail() != null && !data.contactEmail().isBlank()) {
            parts.add(linkedText(theme, data.contactEmail(), "mailto:" + data.contactEmail()));
        }
        if (profile.getWebsiteUrl() != null && !profile.getWebsiteUrl().isBlank()) {
            parts.add(linkedText(theme, profile.getWebsiteUrl(), profile.getWebsiteUrl()));
        }
        if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) {
            parts.add(linkedText(theme, profile.getLinkedinUrl(), profile.getLinkedinUrl()));
        }
        if (profile.getGithubUrl() != null && !profile.getGithubUrl().isBlank()) {
            parts.add(linkedText(theme, profile.getGithubUrl(), profile.getGithubUrl()));
        }
        if (parts.isEmpty()) {
            return null;
        }
        Paragraph contact = new Paragraph(10);
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingAfter(6);
        for (int i = 0; i < parts.size(); i++) {
            contact.add(parts.get(i));
            if (i < parts.size() - 1) {
                contact.add(new Chunk("  •  ", font(theme.bodyFont(), 10, theme.subtleColor())));
            }
        }
        return contact;
    }

    private static Chunk linkedText(Theme theme, String label, String reference) {
        Chunk chunk = new Chunk(label, font(theme.bodyFont(), 10, theme.accentColor()));
        chunk.setAnchor(reference);
        return chunk;
    }

    private static String primaryUrl(Project proj) {
        if (proj.getLiveUrl() != null && !proj.getLiveUrl().isBlank()) {
            return proj.getLiveUrl();
        }
        if (proj.getRepoUrl() != null && !proj.getRepoUrl().isBlank()) {
            return proj.getRepoUrl();
        }
        return null;
    }

    private static String formatRange(String start, String end) {
        if (start == null || start.isBlank()) {
            return nullToBlank(end);
        }
        return start + " - " + nullToBlank(end);
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
