package com.forkmyfolio.model.enums;

/**
 * Declares the privacy contract under which export content (PDF, Markdown)
 * is selected. Phase 8, PRV-1 (security defect): the public download
 * endpoints previously rendered EVERY stored item — including experiences,
 * projects and qualifications the owner had explicitly hidden from the
 * public portfolio — to any anonymous guest.
 *
 * <p>
 * The two visibility modes enforce the exact rules approved for Phase 8:
 * <ul>
 *   <li>{@link #PUBLIC} — mirrors the public portfolio pages one-for-one:
 *       only items whose {@code visible} flag permits public access are
 *       included, section by section (Experience, Project, Qualification,
 *       UserSkill, Testimonial), the profile summary honours the profile's
 *       own section-visibility toggle, and contact details expose exactly
 *       what the public pages expose ({@code publicEmail} — never the
 *       account login email). Used for every unauthenticated download and
 *       for authenticated users downloading SOMEBODY ELSE'S portfolio.</li>
 *   <li>{@link #OWNER} — the authenticated requester IS the portfolio
 *       owner (resolved in the controller; authentication never grants
 *       another user's hidden content). The owner may export their own
 *       hidden content, subject to their later-configured export
 *       preferences (Phase 8.3).</li>
 * </ul>
 */
public enum ExportVisibility {

    /**
     * Public/guest view: identical visibility rules to the public portfolio
     * pages. Hidden items are never included, no matter who asks.
     */
    PUBLIC,

    /**
     * Owner view: the authenticated requester owns this portfolio and may
     * include their own non-public content.
     */
    OWNER
}
