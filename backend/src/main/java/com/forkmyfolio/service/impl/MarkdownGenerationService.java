package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.*;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.service.pdf.PortfolioData;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarkdownGenerationService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownGenerationService.class);

    //<editor-fold desc="Dependencies">
    // Phase 8 (PRV-1): content selection is centralized in the shared
    // ExportContentService; this service only renders the Markdown text.
    private final ExportContentService exportContentService;
    //</editor-fold>

    /**
     * Generates a portfolio in Markdown format for a specific user.
     *
     * @param user       The User entity for whom the portfolio is being generated.
     * @param visibility Phase 8 (PRV-1): the visibility contract for content
     *                   selection — PUBLIC mirrors the public portfolio pages'
     *                   filters (guests and non-owner requesters), OWNER includes
     *                   the owner's own non-public content. Resolved by the
     *                   controller; never derived here.
     * @return A MarkdownFile record containing the byte content and suggested filename.
     */
    @Transactional(readOnly = true)
    public MarkdownFile generatePortfolioMarkdown(User user, ExportVisibility visibility) {
        log.info("Starting Markdown generation process for user '{}' [visibility={}]", user.getSlug(), visibility);

        // Phase 8 (PRV-1): ALL content selection is delegated to the shared
        // ExportContentService — identical visibility rules to the PDF
        // pipeline, so the two formats can never drift.
        PortfolioData portfolioData = exportContentService.buildExportData(user, visibility);

        log.info("Generating Markdown for user: {}", user.getEmail());

        // 2. Build the Markdown string
        String markdownContent = buildMarkdown(portfolioData);

        // 3. Create the file record
        String filename = String.format("%s%s-Portfolio-%s.md",
                user.getFirstName(),
                user.getLastName(),
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );

        log.info("Markdown generation complete. Final content length: {} characters.", markdownContent.length());
        return new MarkdownFile(markdownContent.getBytes(StandardCharsets.UTF_8), filename);
    }

    private String buildMarkdown(PortfolioData data) {
        StringBuilder md = new StringBuilder();
        PortfolioProfile profile = data.profile();
        User user = profile.getUser();
        // Phase 8.3: the concise-resume flag resolved by the shared
        // selection engine (owner preference; PUBLIC exports are always FULL).
        boolean brief = data.exportMode() == com.forkmyfolio.model.enums.ExportMode.BRIEF;

        // --- Header ---
        md.append("# ").append(user.getFirstName()).append(" ").append(user.getLastName()).append("\n");
        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
            md.append("> ").append(profile.getHeadline()).append("\n\n");
        }
        appendContactInfo(md, data);
        md.append("\n---\n\n");

        // --- About Me ---
        // Phase 8 (PRV-1): honours the profile's own public section toggle
        // for public exports; owner exports honour the owner's export
        // summary toggle (data.includeSummary()).
        if (data.includeSummary() && profile.getSummary() != null && !profile.getSummary().isBlank()) {
            md.append("## 📖 About Me\n\n");
            md.append(profile.getSummary()).append("\n\n---\n\n");
        }

        // --- Experience ---
        if (!data.experiences().isEmpty()) {
            md.append("## 💼 Experience\n\n");
            data.experiences().forEach(exp -> appendExperience(md, exp, brief));
            md.append("---\n\n");
        }

        // --- Projects ---
        if (!data.projects().isEmpty()) {
            md.append("## 🚀 Projects\n\n");
            data.projects().forEach(proj -> appendProject(md, proj, brief));
            md.append("---\n\n");
        }

        // --- Skills ---
        if (!data.skills().isEmpty()) {
            md.append("## 🛠️ Skills & Technologies\n\n");
            appendSkills(md, data.skills());
            md.append("---\n\n");
        }

        // --- Qualifications ---
        if (!data.qualifications().isEmpty()) {
            md.append("## 🎓 Qualifications & Education\n\n");
            data.qualifications().forEach(qual -> appendQualification(md, qual, brief));
            md.append("---\n\n");
        }

        // --- Testimonials (Phase 8.3) ---
        if (!data.testimonials().isEmpty()) {
            md.append("## 💬 Testimonials\n\n");
            List<Testimonial> testimonials = brief
                    ? data.testimonials().stream().limit(3).collect(Collectors.toList())
                    : data.testimonials();
            testimonials.forEach(testimonial -> appendTestimonial(md, testimonial));
            md.append("---\n\n");
        }

        // --- Cover Letter (Phase 0.19) ---
        // Owner preference only: rendered when the cover letter mode is
        // IN_PDF and the owner wrote text. PUBLIC exports always carry mode
        // OFF (ExportContentService defaults), so visitors can never
        // receive it. No recipient context exists in the Markdown pipeline,
        // so {{company}}/{{role}} placeholders are emitted as written —
        // the owner can refine them in the Markdown file itself.
        if (data.includeCoverLetter()) {
            md.append("## ✉️ Cover Letter\n\n");
            md.append(data.coverLetterTemplate().trim()).append("\n\n---\n\n");
        }

        return md.toString();
    }

    private void appendContactInfo(StringBuilder md, PortfolioData data) {
        // Phase 8 (PRV-1): the email shown is the export data's contact email —
        // the profile's PUBLIC email (never the account login email) for
        // public downloads; owner exports may fall back to their own account
        // email via the selection engine. Phone remains intentionally
        // excluded (commented out historically).
        // Phase 8.3: the whole contact block honours the owner's contact
        // toggle (includeContact).
        if (!data.includeContact()) {
            return;
        }
        if (data.contactEmail() != null && !data.contactEmail().isBlank()) {
            md.append("📧 ").append(data.contactEmail());
        }
        PortfolioProfile profile = data.profile();
        if (profile.getWebsiteUrl() != null && !profile.getWebsiteUrl().isBlank()) {
            md.append(" | 🌐 [Website](").append(profile.getWebsiteUrl()).append(")");
        }
        if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) {
            md.append(" | 💼 [LinkedIn](").append(profile.getLinkedinUrl()).append(")");
        }
        if (profile.getGithubUrl() != null && !profile.getGithubUrl().isBlank()) {
            md.append(" | 💻 [GitHub](").append(profile.getGithubUrl()).append(")");
        }
        md.append("\n");
    }

    /**
     * Phase 8.3: in BRIEF mode the long-form description is omitted and the
     * achievement list is capped at three — a concise resume, reusing
     * existing data only (nothing invented or rewritten).
     */
    private void appendExperience(StringBuilder md, Experience exp, boolean brief) {
        md.append("### ").append(exp.getJobTitle()).append("\n");
        md.append("**").append(exp.getCompanyName()).append("**");
        if (exp.getLocation() != null && !exp.getLocation().isBlank()) {
            md.append(" | ").append(exp.getLocation());
        }
        md.append("\n\n");

        String startDate = exp.getStartDate().format(DateTimeFormatter.ofPattern("MMM yyyy"));
        String endDate = exp.getEndDate() != null ? exp.getEndDate().format(DateTimeFormatter.ofPattern("MMM yyyy")) : "Present";
        md.append("*").append(startDate).append(" - ").append(endDate).append("*\n\n");

        if (!brief && exp.getDescription() != null && !exp.getDescription().isBlank()) {
            md.append(exp.getDescription().replaceAll("\\r\\n|\\r|\\n", "\n> ")).append("\n\n");
        }

        if (exp.getAchievements() != null && !exp.getAchievements().isBlank()) {
            List<String> achievements = Arrays.stream(exp.getAchievements().split("\\r?\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.toList());
            if (brief) {
                achievements = achievements.stream().limit(3).collect(Collectors.toList());
            }
            if (!achievements.isEmpty()) {
                md.append("**Key Achievements:**\n");
                for (String achievement : achievements) {
                    md.append("- ").append(achievement).append("\n");
                }
                md.append("\n");
            }
        }

        if (exp.getSkills() != null && !exp.getSkills().isEmpty()) {
            String skills = exp.getSkills().stream().map(Skill::getName).collect(Collectors.joining(", "));
            md.append("**Skills:** ").append(skills).append("\n\n");
        }
    }

    /**
     * Phase 8.3: in BRIEF mode the long-form description is omitted and the
     * key-feature list is capped at three.
     */
    private void appendProject(StringBuilder md, Project proj, boolean brief) {
        md.append("### ").append(proj.getTitle()).append("\n\n");
        if (!brief && proj.getDescription() != null && !proj.getDescription().isBlank()) {
            md.append(proj.getDescription()).append("\n\n");
        }

        if (proj.getRepoUrl() != null && !proj.getRepoUrl().isBlank()) {
            md.append("- **Repository:** [").append(proj.getRepoUrl()).append("](").append(proj.getRepoUrl()).append(")\n");
        }
        if (proj.getLiveUrl() != null && !proj.getLiveUrl().isBlank()) {
            md.append("- **Live Demo:** [").append(proj.getLiveUrl()).append("](").append(proj.getLiveUrl()).append(")\n");
        }
        // Phase 7.4 (§6): structured key features (display order preserved);
        // only emitted when present - legacy projects with Markdown bullets
        // embedded in the description keep rendering those via the description.
        if (proj.getKeyFeatures() != null && !proj.getKeyFeatures().isEmpty()) {
            List<KeyFeature> features = brief
                    ? proj.getKeyFeatures().stream().limit(3).collect(Collectors.toList())
                    : proj.getKeyFeatures();
            md.append("- **Key Features:**\n");
            for (KeyFeature feature : features) {
                md.append("  - ").append(feature.getValue()).append("\n");
            }
        }
        md.append("\n");

        if (proj.getSkills() != null && !proj.getSkills().isEmpty()) {
            String skills = proj.getSkills().stream().map(Skill::getName).collect(Collectors.joining(" | "));
            md.append("**Technologies:** ").append(skills).append("\n\n");
        }
    }

    private void appendSkills(StringBuilder md, List<Skill> skills) {
        // Group skills by category for better organization
        Map<String, List<Skill>> groupedSkills = skills.stream()
                .filter(s -> s.getCategory() != null && !s.getCategory().isBlank())
                .collect(Collectors.groupingBy(Skill::getCategory));

        for (Map.Entry<String, List<Skill>> entry : groupedSkills.entrySet()) {
            md.append("#### ").append(entry.getKey()).append("\n");
            String skillNames = entry.getValue().stream()
                    .map(Skill::getName)
                    .collect(Collectors.joining(", "));
            md.append(skillNames).append("\n\n");
        }

        // Add skills without a category
        List<Skill> uncategorizedSkills = skills.stream()
                .filter(s -> s.getCategory() == null || s.getCategory().isBlank())
                .toList();
        if (!uncategorizedSkills.isEmpty()) {
            md.append("#### Other Skills\n");
            String skillNames = uncategorizedSkills.stream()
                    .map(Skill::getName)
                    .collect(Collectors.joining(", "));
            md.append(skillNames).append("\n\n");
        }
    }

    /**
     * Phase 8.3: in BRIEF mode only the qualification name, institution and
     * date render (field/level/grade/credential URL omitted) so the concise
     * resume stays concise.
     */
    private void appendQualification(StringBuilder md, Qualification qual, boolean brief) {
        md.append("### ").append(qual.getQualificationName()).append("\n");

        String institution = qual.getInstitutionName();
        if (StringUtils.hasText(qual.getInstitutionWebsite())) {
            institution = String.format("[%s](%s)", institution, qual.getInstitutionWebsite());
        }
        md.append("**").append(institution).append("**\n\n");

        // Date String
        String dateString;
        if (Boolean.TRUE.equals(qual.getStillStudying())) {
            dateString = qual.getStartYear() != null ? qual.getStartYear() + " - Present" : "Present";
        } else {
            dateString = qual.getCompletionYear() != null ? String.valueOf(qual.getCompletionYear()) : "N/A";
        }
        md.append("- **Date:** ").append(dateString).append("\n");

        if (!brief) {
            if (StringUtils.hasText(qual.getFieldOfStudy())) {
                md.append("- **Field:** ").append(qual.getFieldOfStudy()).append("\n");
            }
            if (qual.getLevel() != null) {
                String level = qual.getLevel().name().charAt(0) + qual.getLevel().name().substring(1).toLowerCase();
                md.append("- **Level:** ").append(level).append("\n");
            }
            if (StringUtils.hasText(qual.getGrade())) {
                md.append("- **Grade:** ").append(qual.getGrade()).append("\n");
            }
            if (StringUtils.hasText(qual.getCredentialUrl())) {
                md.append("- **Credential:** [View Certificate](").append(qual.getCredentialUrl()).append(")\n");
            }
        }
        md.append("\n");
    }

    /**
     * Phase 8.3: testimonial rendering — quote plus author attribution
     * (author title included when present).
     */
    private void appendTestimonial(StringBuilder md, Testimonial testimonial) {
        if (testimonial == null || testimonial.getQuote() == null || testimonial.getQuote().isBlank()) {
            return;
        }
        md.append("> ").append(testimonial.getQuote().replaceAll("\\r?\\n", "\n> ")).append("\n>\n");
        String attribution = testimonial.getAuthorName() == null ? "" : testimonial.getAuthorName();
        if (testimonial.getAuthorTitle() != null && !testimonial.getAuthorTitle().isBlank()) {
            attribution += ", " + testimonial.getAuthorTitle();
        }
        if (!attribution.isBlank()) {
            md.append("> — ").append(attribution).append("\n");
        }
        md.append("\n");
    }

    public record MarkdownFile(byte[] content, String suggestedFilename) {
    }
}