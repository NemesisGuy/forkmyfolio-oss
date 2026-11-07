package com.forkmyfolio.service;

import com.forkmyfolio.exception.PermissionDeniedException;
import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.PublicPortfolioSummary;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;

import java.util.List;

/**
 * Service for retrieving user data for public portfolios.
 */
public interface PortfolioService {

    /**
     * Retrieves a user entity for the purpose of building a public portfolio.
     * This method validates that the user exists and their portfolio is set to
     * public.
     *
     * @param slug The unique, URL-friendly identifier of the user.
     * @return The {@link User} entity if the portfolio is public.
     * @throws ResourceNotFoundException if no active user with the given slug is
     *                                   found.
     * @throws PermissionDeniedException if the user's portfolio is private.
     */
    User getPublicPortfolioUserBySlug(String slug);

    /**
     * Retrieves the publicly visible skills of a portfolio owner.
     * Visibility filtering is business policy and performed here, not in the
     * controller (NAG-01).
     *
     * @param slug The public slug of the portfolio owner.
     * @return The list of visible {@link UserSkill} entities.
     */
    List<UserSkill> getVisibleUserSkillsBySlug(String slug);

    /**
     * Retrieves the qualifications of a portfolio owner for public display.
     *
     * @param slug The public slug of the portfolio owner.
     * @return The list of {@link Qualification} entities.
     */
    List<Qualification> getPublicQualificationsBySlug(String slug);

    /**
     * Retrieves the testimonials of a portfolio owner for public display.
     *
     * @param slug The public slug of the portfolio owner.
     * @return The list of {@link Testimonial} entities.
     */
    List<Testimonial> getPublicTestimonialsBySlug(String slug);

    /**
     * Retrieves the portfolio profile of a portfolio owner, e.g. for vCard
     * generation.
     *
     * @param slug The public slug of the portfolio owner.
     * @return The {@link PortfolioProfile} of the owner.
     */
    PortfolioProfile getPublicPortfolioProfileBySlug(String slug);

    /**
     * SEO sitemap source (v3.1.2): minimal summaries (slug + updatedAt) of
     * every ACTIVE user with a publiclyVisible portfolio. Contains no
     * personal data and never includes private or inactive accounts.
     *
     * @return Lean list of public portfolio summaries, ordered by slug.
     */
    List<PublicPortfolioSummary> getPublicPortfolioSummaries();
}