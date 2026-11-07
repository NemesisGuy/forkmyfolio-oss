package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import com.forkmyfolio.service.PortfolioProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioProfileServiceImpl implements PortfolioProfileService {

    private final PortfolioProfileRepository portfolioProfileRepository;

    @Override
    @Transactional
    public PortfolioProfile getProfileByUser(User user) {
        // FIX: Implement a "get-or-create" pattern. If a profile doesn't exist,
        // create and persist a new, default one. This ensures every user has a
        // profile record from their first interaction, preventing null pointer issues.
        return portfolioProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info(
                            "No PortfolioProfile found for user ID: {}. Creating and persisting a new default instance.",
                            user.getId());
                    PortfolioProfile newProfile = new PortfolioProfile();
                    newProfile.setUser(user);
                    return portfolioProfileRepository.save(newProfile);
                });
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = { "portfolios", "publicPortfolios" }, key = "#user.slug")
    public PortfolioProfile createOrUpdateProfile(PortfolioProfile profileUpdates, User user) {
        // This method now safely handles both creation of a new profile and updates to
        // an existing one.
        PortfolioProfile existingProfile = getProfileByUser(user);

        existingProfile.setHeadline(profileUpdates.getHeadline());
        existingProfile.setSummary(profileUpdates.getSummary());
        existingProfile.setPublicEmail(profileUpdates.getPublicEmail());
        existingProfile.setWebsiteUrl(profileUpdates.getWebsiteUrl());
        existingProfile.setLinkedinUrl(profileUpdates.getLinkedinUrl());
        existingProfile.setGithubUrl(profileUpdates.getGithubUrl());
        existingProfile.setResumeUrl(profileUpdates.getResumeUrl());
        existingProfile.setResumeImageUrl(profileUpdates.getResumeImageUrl());
        existingProfile.setLocation(profileUpdates.getLocation());
        existingProfile.setCoverLetterTemplate(profileUpdates.getCoverLetterTemplate());
        existingProfile.setVisible(profileUpdates.isVisible());
        existingProfile.setPubliclyVisible(profileUpdates.isPubliclyVisible());

        return portfolioProfileRepository.save(existingProfile);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = { "portfolios", "publicPortfolios" }, key = "#user.slug")
    public PortfolioProfile updateProfileVisibility(User user, boolean isPublic) {
        PortfolioProfile profile = getProfileByUser(user);
        profile.setPubliclyVisible(isPublic);
        log.info("Updating portfolio visibility for user {} to: {}", user.getSlug(), isPublic);
        return portfolioProfileRepository.save(profile);
    }

    /**
     * Phase 10 (Workstream 2): updates the owner's search-engine indexing
     * preference. Cache eviction mirrors updateProfileVisibility — the public
     * portfolio payload carries this preference (the SSR layer derives
     * per-user noindex directives from it), so cached payloads must not go
     * stale after a preference change.
     */
    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = { "portfolios", "publicPortfolios" }, key = "#user.slug")
    public PortfolioProfile updateProfileIndexing(User user, boolean searchIndexable) {
        PortfolioProfile profile = getProfileByUser(user);
        profile.setSearchIndexable(searchIndexable);
        log.info("Updating search-engine indexing preference for user {} to: {}", user.getSlug(), searchIndexable);
        return portfolioProfileRepository.save(profile);
    }
}