package com.forkmyfolio.service;

import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;

/**
 * Service interface for managing a user's portfolio profile.
 */
public interface PortfolioProfileService {

    /**
     * Retrieves the portfolio profile for a given user. If a profile does not exist,
     * a new, transient default instance is created and returned.
     *
     * @param user The user whose profile to retrieve.
     * @return The existing or a new default PortfolioProfile.
     */
    PortfolioProfile getProfileByUser(User user);

    PortfolioProfile createOrUpdateProfile(PortfolioProfile profileUpdates, User user);

    /**
     * Updates the public visibility of a user's portfolio.
     *
     * @param user     The user whose profile to update.
     * @param isPublic The new visibility status.
     * @return The updated PortfolioProfile.
     */
    PortfolioProfile updateProfileVisibility(User user, boolean isPublic);

    /**
     * Phase 10 (Workstream 2): updates the owner's search-engine indexing
     * preference. Independent of visibility — a public portfolio can opt out
     * of search indexing while remaining reachable via its direct URL. Always
     * subordinate to the deployment-wide indexable switch.
     *
     * @param user            The user whose preference to update.
     * @param searchIndexable Whether search engines may index the portfolio.
     * @return The updated PortfolioProfile.
     */
    PortfolioProfile updateProfileIndexing(User user, boolean searchIndexable);
}