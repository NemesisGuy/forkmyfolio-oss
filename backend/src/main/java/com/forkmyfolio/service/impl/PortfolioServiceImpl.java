package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.PublicPortfolioSummary;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.service.PortfolioProfileService;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {

        private final UserService userService;
        private final PortfolioProfileService portfolioProfileService;
        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public User getPublicPortfolioUserBySlug(String slug) {
                // Fetch the user
                User user = userService.findBySlug(slug)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User with slug '" + slug + "' not found."));

                // Get the profile and perform permission checks.
                // Phase 7.4 (§403/404 policy): a PRIVATE portfolio must be
                // indistinguishable from a nonexistent one for everyone except
                // its owner — "This portfolio is private." (403) revealed that
                // the slug exists. Cross-tenant/anonymous access to a private
                // portfolio now throws ResourceNotFound (404, "resource_not_found")
                // exactly like the slug-miss branch above, so existence is never
                // disclosed. 403 is preserved ONLY for role/permission denials
                // that do not conceal a resource's existence (guest-write
                // attempts, backup re-verification).
                if (!portfolioProfileService.getProfileByUser(user).isPubliclyVisible()) {
                        try {
                                User currentUser = userService.getCurrentAuthenticatedUser();
                                if (!currentUser.getId().equals(user.getId())) {
                                        log.warn("Private portfolio for slug '{}' withheld as 404 (requested by '{}').",
                                                        slug, currentUser.getEmail());
                                        throw new ResourceNotFoundException(
                                                        "User with slug '" + slug + "' not found.");
                                }
                                log.info("Access GRANTED for slug '{}' to owner '{}'.", slug, currentUser.getEmail());
                        } catch (ResourceNotFoundException rnfe) {
                                throw rnfe;
                        } catch (Exception e) {
                                log.warn("Private portfolio for slug '{}' withheld as 404 (authentication check failed: {}).",
                                                slug, e.getMessage());
                                throw new ResourceNotFoundException(
                                                "User with slug '" + slug + "' not found.");
                        }
                }

                // Force initialization of lazy collections needed for the DTO transformation in
                // the Controller boundary
                Hibernate.initialize(user.getPortfolioProfile());
                Hibernate.initialize(user.getQualifications());
                Hibernate.initialize(user.getTestimonials());

                user.getProjects().forEach(project -> {
                        Hibernate.initialize(project.getSkills());
                        Hibernate.initialize(project.getKeyFeatures()); // Phase 7.4 (§6)
                });
                user.getExperiences().forEach(experience -> Hibernate.initialize(experience.getSkills()));
                user.getUserSkills().forEach(userSkill -> Hibernate.initialize(userSkill.getSkill()));

                log.info("Access GRANTED for slug '{}'. Returning initialized user entity.", slug);
                return user;
        }

        @Override
        @Transactional(readOnly = true)
        public List<UserSkill> getVisibleUserSkillsBySlug(String slug) {
                User user = getPublicPortfolioUserBySlug(slug);
                // Visibility policy lives in the service layer (NAG-01), not the controller.
                return user.getUserSkills().stream()
                                .filter(UserSkill::isVisible)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<Qualification> getPublicQualificationsBySlug(String slug) {
                User user = getPublicPortfolioUserBySlug(slug);
                return List.copyOf(user.getQualifications());
        }

        @Override
        @Transactional(readOnly = true)
        public List<Testimonial> getPublicTestimonialsBySlug(String slug) {
                User user = getPublicPortfolioUserBySlug(slug);
                return List.copyOf(user.getTestimonials());
        }

        @Override
        @Transactional(readOnly = true)
        public PortfolioProfile getPublicPortfolioProfileBySlug(String slug) {
                User user = getPublicPortfolioUserBySlug(slug);
                return user.getPortfolioProfile();
        }

        @Override
        @Transactional(readOnly = true)
        public List<PublicPortfolioSummary> getPublicPortfolioSummaries() {
                // Lean projection straight from the repository — no entity
                // hydration, no personal data, only public + active accounts.
                return userRepository.findPublicPortfolioSummaries();
        }
}
