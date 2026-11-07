package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSetting;
import com.forkmyfolio.model.enums.ExportMode;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.repository.ExperienceRepository;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import com.forkmyfolio.repository.ProjectRepository;
import com.forkmyfolio.repository.QualificationRepository;
import com.forkmyfolio.repository.UserSettingRepository;
import com.forkmyfolio.service.pdf.ExportPreferences;
import com.forkmyfolio.service.pdf.PortfolioData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Single source of truth for WHAT content goes into an export (PDF or
 * Markdown). Both generation services delegate every content-selection
 * decision here so their visibility behaviour can never drift — Phase 8,
 * PRV-1 (security defect).
 *
 * <p>
 * PRV-1 root cause: the export services previously pulled items straight
 * from the repositories WITHOUT the visibility filters the public portfolio
 * pages apply (see {@code PortfolioMapper}: per-section
 * {@code filter(X::isVisible)}). A guest could therefore download hidden
 * experiences/projects/qualifications from a public portfolio, and the
 * contact line leaked the ACCOUNT email instead of the public contact
 * email. This service re-implements the public pages' rule set exactly and
 * adds the owner-view escape hatch approved for Phase 8:
 *
 * <ul>
 *   <li>{@link ExportVisibility#PUBLIC} — guest downloads and
 *       authenticated users downloading someone else's portfolio. Only
 *       publicly visible items; summary honours the profile's own section
 *       toggle; contact email is the profile's {@code publicEmail} (never
 *       the account login email). The owner's personal {@code export.*}
 *       preferences are IGNORED here — public exports must behave exactly
 *       like the public portfolio pages no matter how the owner configures
 *       their own downloads.</li>
 *   <li>{@link ExportVisibility#OWNER} — resolved in the controller by
 *       comparing the current authenticated principal with the portfolio
 *       owner; authentication NEVER grants another user's hidden content.
 *       Includes the owner's non-public content, shaped by the owner's
 *       Phase 8.3 export preferences (mode + section toggles).</li>
 * </ul>
 *
 * <p>
 * Phase 8.3 (export preferences): for OWNER exports the owner's
 * {@code export.*} UserSetting overrides are resolved into an
 * {@link ExportPreferences} value (safe fallbacks — missing, partial or
 * malformed settings can never fail an export) and applied as content
 * SELECTION: a disabled section simply yields an empty list, which every
 * renderer already skips, so PDF and Markdown selection can never drift.
 * The BRIEF mode flag is carried on {@link PortfolioData} and applied at
 * render time (long-form descriptions omitted, bullet lists capped).
 *
 * <p>
 * Adheres to NAG-01 (DBR): operates on entities and maps; the returned
 * {@link PortfolioData} carries derived, privacy-safe contact/summary/
 * image fields in addition to the raw section lists so renderers never
 * re-derive privacy decisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportContentService {

    private final PortfolioProfileRepository portfolioProfileRepository;
    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final QualificationRepository qualificationRepository;
    private final UserSettingRepository userSettingRepository;

    /**
     * Builds the export content for the given portfolio owner under the
     * given visibility contract. MUST be called inside a transaction (the
     * generation services already are): lazy collections (user skills,
     * testimonials) are initialized here.
     *
     * @param owner      the portfolio owner whose content is exported.
     * @param visibility PUBLIC (privacy-filtered, prefs ignored) or OWNER
     *                   (full own data, shaped by the owner's export prefs).
     * @return the assembled, privacy-respecting export data set.
     */
    @Transactional
    public PortfolioData buildExportData(User owner, ExportVisibility visibility) {
        PortfolioProfile profile = portfolioProfileRepository.findByUser(owner)
                .orElseThrow(() -> new IllegalStateException(
                        "Portfolio profile not found for user: " + owner.getEmail()));

        boolean publicView = visibility == ExportVisibility.PUBLIC;

        // Phase 8.3: the owner's export preferences apply ONLY to their own
        // (OWNER) exports. PUBLIC exports ignore them entirely.
        ExportPreferences prefs = publicView
                ? ExportPreferences.defaults()
                : resolveOwnerPreferences(owner);

        List<Experience> experiences = selectSection(
                experienceRepository.findByUserOrderByDisplayOrderAsc(owner),
                prefs.experience(),
                publicView, "experience", Experience::isVisible);

        List<Project> projects = selectSection(
                projectRepository.findByUserOrderByDisplayOrderAsc(owner),
                prefs.projects(),
                publicView, "project", Project::isVisible);

        List<Qualification> qualifications = selectSection(
                qualificationRepository.findByUserOrderByCompletionYearDescStartYearDesc(owner),
                prefs.qualifications(),
                publicView, "qualification", Qualification::isVisible);

        List<Skill> skills = owner.getUserSkills().stream()
                .filter(userSkill -> !publicView || userSkill.isVisible())
                .map(userSkill -> {
                    Skill globalSkill = userSkill.getSkill();
                    // Transient Skill carrying user-specific proficiency data,
                    // exactly as the previous inline assembly did.
                    Skill exportSkill = new Skill();
                    exportSkill.setName(globalSkill.getName());
                    exportSkill.setCategory(globalSkill.getCategory());
                    exportSkill.setIcon(globalSkill.getIcon());
                    exportSkill.setLevel(userSkill.getLevel());
                    exportSkill.setDescription(userSkill.getDescription());
                    exportSkill.setVisible(userSkill.isVisible());
                    return exportSkill;
                })
                .collect(Collectors.toList());
        if (!prefs.skills()) {
            skills = List.of();
        }

        // Testimonials are selected with the same visibility contract;
        // Phase 8.3 adds their rendering (Markdown + current-generation PDF
        // templates) gated on the owner's testimonial section toggle.
        List<Testimonial> testimonials = (publicView
                ? owner.getTestimonials().stream().filter(Testimonial::isVisible)
                : owner.getTestimonials().stream())
                .filter(testimonial -> prefs.testimonials())
                .collect(Collectors.toList());

        // Profile summary: public view mirrors the public pages, which hide
        // the summary section when the profile's own visibility toggle is
        // off; owner view honours the owner's export summary toggle.
        boolean includeSummary = publicView ? profile.isVisible() : prefs.summary();

        // Contact email: the public pages NEVER expose the account login
        // email (only profile.publicEmail is rendered). Owner view falls
        // back to the account email to preserve the owner's current
        // Markdown-export behaviour for their own files. The owner's
        // contact toggle removes the whole contact block when disabled.
        String contactEmail;
        if (!prefs.contact()) {
            contactEmail = "";
        } else {
            contactEmail = profile.getPublicEmail() != null && !profile.getPublicEmail().isBlank()
                    ? profile.getPublicEmail()
                    : (publicView ? "" : owner.getEmail());
        }

        if (log.isDebugEnabled()) {
            log.debug("Export data assembled [user='{}', visibility={}, mode={}, experiences={}, projects={}, qualifications={}, skills={}, testimonials={}, summary={}, contact={}]",
                    owner.getSlug(), visibility, prefs.mode(), experiences.size(), projects.size(),
                    qualifications.size(), skills.size(), testimonials.size(),
                    includeSummary, prefs.contact());
        }

        return new PortfolioData(
                profile,
                experiences,
                qualifications,
                projects,
                skills,
                testimonials,
                contactEmail,
                includeSummary,
                prefs.contact(),
                prefs.mode(),
                owner.getProfileImageUrl(),
                // Phase 0.19: the cover letter is owner-personal data. PUBLIC
                // exports resolve to defaults() whose coverLetterMode is OFF,
                // so visitors can never receive it (PRV-1); OWNER exports
                // honour the owner's own export.coverletter.mode setting.
                profile.getCoverLetterTemplate(),
                prefs.coverLetterMode());
    }

    /**
     * Resolves the owner's {@code export.*} UserSetting overrides into an
     * {@link ExportPreferences}. Read via the repository (not the lazy
     * {@code user.getUserSettings()} collection) so the owner may be a
     * freshly fetched, settings-uninitialized entity. Missing, partial or
     * malformed values fall back to the safe defaults inside
     * {@link ExportPreferences#fromUserSettings(Map)}.
     */
    private ExportPreferences resolveOwnerPreferences(User owner) {
        Map<String, String> overrides = userSettingRepository.findByUser(owner).stream()
                .filter(setting -> setting.getName() != null
                        && (ExportPreferences.MODE_SETTING.equals(setting.getName())
                                || setting.getName().startsWith(ExportPreferences.SECTION_PREFIX)
                                || ExportPreferences.COVER_LETTER_MODE_SETTING.equals(setting.getName())))
                .collect(Collectors.toMap(UserSetting::getName, UserSetting::getValue));
        return ExportPreferences.fromUserSettings(overrides);
    }

    /**
     * Applies one section's selection rules: the owner's toggle (empty list
     * when disabled) and, for PUBLIC exports, the per-item visibility
     * filter the public portfolio pages apply (PRV-1).
     */
    private <T> List<T> selectSection(List<T> items, boolean sectionEnabled,
            boolean publicView, String sectionName, java.util.function.Function<T, Boolean> visibilityOf) {
        Stream<T> stream = items.stream();
        if (!sectionEnabled) {
            log.debug("Export section '{}' disabled by owner preference", sectionName);
            return List.of();
        }
        if (publicView) {
            List<T> visible = stream.filter(item -> Boolean.TRUE.equals(visibilityOf.apply(item)))
                    .collect(Collectors.toList());
            int hidden = items.size() - visible.size();
            if (hidden > 0) {
                log.info("PRV-1 public export filtering: {} hidden {} item(s) excluded", hidden, sectionName);
            }
            return visible;
        }
        return stream.collect(Collectors.toList());
    }
}
