package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.VisitorStats;
import com.forkmyfolio.model.enums.VisitorStatType;
import com.forkmyfolio.repository.VisitorStatsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing visitor and application statistics.
 * Adheres to NAG-01 (DBR): Purely entity-based. No DTO dependencies.
 */
@Service
@RequiredArgsConstructor
public class VisitorStatsService {

    private static final Logger log = LoggerFactory.getLogger(VisitorStatsService.class);

    // Reference IDs for global stats
    private static final String TOTAL_VISITS_REF_ID = "site_total";
    private static final String PROJECTS_SECTION_REF_ID = "projects_section";
    private static final String SKILLS_SECTION_REF_ID = "skills_section";
    private static final String EXPERIENCE_SECTION_REF_ID = "experience_section";
    private static final String QUALIFICATIONS_SECTION_REF_ID = "qualifications_section";
    private static final String CONTACT_MESSAGE_SUBMISSION_REF_ID = "contact_submission";
    private static final String PDF_DOWNLOAD_REF_ID = "pdf_download";
    private static final String VCARD_DOWNLOAD_REF_ID = "vcard_download";
    private static final String TESTIMONIALS_SECTION_REF_ID = "testimonials_section";
    private static final String LOGIN_SUCCESS_REF_ID = "auth_login_success";
    private static final String LOGIN_FAILURE_REF_ID = "auth_login_failure";
    private static final String LOGOUT_SUCCESS_REF_ID = "auth_logout_success";

    private final VisitorStatsRepository visitorStatsRepository;

    @Async
    @Transactional
    public void incrementTotalVisits() {
        incrementStat(VisitorStatType.TOTAL_VISITS, TOTAL_VISITS_REF_ID);
    }

    @Async
    @Transactional
    public void incrementPortfolioView(String slug) {
        incrementStat(VisitorStatType.PORTFOLIO_VIEW, slug);
    }

    @Async
    @Transactional
    public void incrementProjectView(String projectId) {
        incrementStat(VisitorStatType.PROJECT_VIEW, projectId);
    }

    @Async
    @Transactional
    public void incrementProjectsSectionView() {
        incrementStat(VisitorStatType.PROJECTS_SECTION_VIEW, PROJECTS_SECTION_REF_ID);
    }

    @Async
    @Transactional
    public void incrementSkillsSectionView() {
        incrementStat(VisitorStatType.SKILLS_SECTION_VIEW, SKILLS_SECTION_REF_ID);
    }

    @Async
    @Transactional
    public void incrementExperienceSectionView() {
        incrementStat(VisitorStatType.EXPERIENCE_SECTION_VIEW, EXPERIENCE_SECTION_REF_ID);
    }

    @Async
    @Transactional
    public void incrementQualificationsSectionView() {
        incrementStat(VisitorStatType.QUALIFICATIONS_SECTION_VIEW, QUALIFICATIONS_SECTION_REF_ID);
    }

    @Async
    @Transactional
    public void incrementContactMessageSubmission() {
        incrementStat(VisitorStatType.CONTACT_MESSAGE_SUBMISSION, CONTACT_MESSAGE_SUBMISSION_REF_ID);
    }

    @Async
    @Transactional
    public void incrementPdfDownload() {
        incrementStat(VisitorStatType.PDF_DOWNLOAD, PDF_DOWNLOAD_REF_ID);
    }

    @Async
    @Transactional
    public void incrementVcardDownload() {
        incrementStat(VisitorStatType.VCARD_DOWNLOAD, VCARD_DOWNLOAD_REF_ID);
    }

    @Async
    @Transactional
    public void incrementTestimonialsSectionView() {
        incrementStat(VisitorStatType.TESTIMONIALS_SECTION_VIEW, TESTIMONIALS_SECTION_REF_ID);
    }

    @Async
    @Transactional
    public void incrementLoginSuccess() {
        incrementStat(VisitorStatType.LOGIN_SUCCESS, LOGIN_SUCCESS_REF_ID);
    }

    @Async
    @Transactional
    public void incrementLoginFailure() {
        incrementStat(VisitorStatType.LOGIN_FAILURE, LOGIN_FAILURE_REF_ID);
    }

    @Async
    @Transactional
    public void incrementLogoutSuccess() {
        incrementStat(VisitorStatType.LOGOUT_SUCCESS, LOGOUT_SUCCESS_REF_ID);
    }

    /**
     * Phase 7.4 (§11): the OLD implementation was read-modify-write
     * (findByTypeAndRefId -> setCount(+1) -> save), which lost increments
     * under concurrent traffic and could race duplicate rows into existence
     * for a brand-new ref. Now EVERY increment:
     * <ol>
     * <li>runs a race-safe {@code INSERT IGNORE} seed FIRST (V10's unique key
     * on (type, ref_id) makes duplicate rows impossible; a concurrent seed
     * no-ops). Seeding unconditionally and BEFORE the update serializes
     * concurrent first-visit seeds on the unique key — the previously-tried
     * update-first/seed-if-missed order DEADLOCKED under MySQL REPEATABLE
     * READ gap locks (both concurrent UPDATEs miss, then both INSERTs race
     * the gap),</li>
     * <li>then runs the atomic single-statement UPDATE (no lost update
     * possible).</li>
     * </ol>
     * Both statements share one transaction/connection — deliberately NOT a
     * nested REQUIRES_NEW call, which would need a second pooled connection
     * per increment and starve the default 10-connection Hikari pool under
     * bursts of async stat increments.
     */
    private void incrementStat(VisitorStatType type, String refId) {
        // Phase 0.20: a portfolio view must never fail because a background
        // stat write hit a transient DB problem (this method also runs on
        // non-async callers in some tests — belt and braces either way).
        try {
            visitorStatsRepository.seedRowIfAbsent(type.name(), refId);
            visitorStatsRepository.incrementCount(type, refId);
        } catch (RuntimeException e) {
            log.warn("Visitor stat increment for {}['{}'] failed — dropping (request unaffected): {}",
                    type, refId, e.getMessage());
        }
    }

    /**
     * Retrieves all visitor statistics.
     * 
     * @return A list of all VisitorStats entities.
     */
    @Transactional(readOnly = true)
    public List<VisitorStats> getAllStats() {
        log.debug("Fetching all visitor stats.");
        return visitorStatsRepository.findAll();
    }
}