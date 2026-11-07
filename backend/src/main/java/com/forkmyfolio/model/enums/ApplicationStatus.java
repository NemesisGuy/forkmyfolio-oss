package com.forkmyfolio.model.enums;

/**
 * Represents the stages of a job application lifecycle.
 *
 * <p>
 * Phase 8.4: extended to the full 13-state pipeline requested by the user
 * (discovery → preparation → submission → interviews/assessment → outcome →
 * archive). The two pre-8.4 values {@link #INTERVIEWING} and {@link #ACCEPTED}
 * are retained as LEGACY values so existing database rows (and any client
 * that still sends them) remain valid — the V11 migration keeps them in the
 * column's ENUM set. New tracking should use the finer-grained replacements
 * ({@code INTERVIEW_SCHEDULED}/{@code INTERVIEW_COMPLETED} and the explicit
 * offer-acceptance workflow via notes once an {@code OFFER} is recorded).
 */
public enum ApplicationStatus {

    /**
     * Phase 8.4: a role the user has spotted and is interested in — nothing
     * prepared yet.
     */
    INTERESTED,

    /**
     * Phase 8.4: the user is actively preparing (tailoring CV, researching).
     */
    PREPARING,

    /**
     * Initial draft of an application, not yet submitted.
     */
    DRAFT,

    /**
     * Application has been formally submitted to the company.
     */
    APPLIED,

    /**
     * Phase 8.4: the company confirmed receipt of the application.
     */
    ACKNOWLEDGED,

    /**
     * Phase 8.4: an interview has been scheduled.
     */
    INTERVIEW_SCHEDULED,

    /**
     * Phase 8.4: a scheduled interview has taken place.
     */
    INTERVIEW_COMPLETED,

    /**
     * Phase 8.4: a technical assessment / take-home task is underway or done.
     */
    TECHNICAL_ASSESSMENT,

    /**
     * A job offer has been extended to the user.
     */
    OFFER,

    /**
     * Application was rejected by the company.
     */
    REJECTED,

    /**
     * User has withdrawn the application.
     */
    WITHDRAWN,

    /**
     * Phase 8.4: no response after a reasonable waiting period.
     */
    NO_RESPONSE,

    /**
     * Phase 8.4: parked without deleting the history; revisit via the
     * archived filter.
     */
    ARCHIVED,

    /**
     * LEGACY (pre-8.4): generic "in the interview process". Kept so existing
     * records remain valid; new tracking should prefer
     * {@link #INTERVIEW_SCHEDULED} / {@link #INTERVIEW_COMPLETED}.
     */
    INTERVIEWING,

    /**
     * LEGACY (pre-8.4): offer accepted. Kept so existing records remain
     * valid.
     */
    ACCEPTED
}
