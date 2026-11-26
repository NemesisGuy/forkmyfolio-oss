package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.EmailAlreadyExistsException;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.MessagePriority;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.ContactMessageRepository;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.AuthService;
import com.forkmyfolio.service.PolicyService;
import com.forkmyfolio.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Service implementation for authentication and registration.
 * Adheres to NAG-01 (DBR): Purely entity-based. No DTO dependencies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlugService slugService;
    private final PolicyService policyService;
    private final PortfolioProfileRepository portfolioProfileRepository;
    private final ContactMessageRepository contactMessageRepository;

    private AuthServiceImpl self;

    @Autowired
    public void setSelf(@Lazy AuthServiceImpl self) {
        this.self = self;
    }

    @Override
    public User registerUser(User user, String rawPassword, String termsVersion) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("An account with this email address already exists.");
        }

        // Validate that the user is accepting the current terms version
        String currentTermsVersion = policyService.getTermsOfService().getVersion();
        if (!currentTermsVersion.equals(termsVersion)) {
            throw new IllegalArgumentException(
                    "You must accept the latest Terms of Service version: " + currentTermsVersion);
        }

        // Prepare user entity
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setProvider(AuthProvider.LOCAL);
        user.setRoles(Set.of(Role.USER));
        user.setActive(true);
        user.setEmailVerified(false);
        user.setPasswordLastChangedAt(Instant.now());
        user.setTermsAcceptedAt(Instant.now());
        user.setTermsVersion(termsVersion);

        String fullName = user.getFirstName() + " " + user.getLastName();
        String baseSlug = slugService.toSlug(fullName);

        // 10 transaction attempts: probe 0 always "wastes" one attempt on the raw
        // base slug (see below), and under the e2e suite several registrations
        // race each other on the same base slug — each concurrent loser burns one
        // attempt. 10 comfortably covers a 5-way race plus the occupied base.
        int maxRetries = 10;
        // SLUG PROBE COUNTER — deliberately SEPARATE from `attempt`.
        // The old inlined pre-check did `attempt++` inside its while-loop, so a
        // long run of occupied slugs (e.g. test-user + test-user-2..-10) pushed
        // `attempt` past `maxRetries - 1` and the catch below rethrew the
        // collision as an HTTP 500 instead of retrying. `attempt` now counts
        // ONLY transaction retries; `probe` only selects the next candidate.
        //
        // PROBE WINDOW: the pre-check must keep walking until it finds a FREE
        // candidate. The old `probe >= maxRetries + 5` break stopped the walk on
        // a KNOWN-occupied candidate once probe passed 10 — exactly the state
        // the e2e suite accumulates (test-user-2..-16+ in DEV) — so every
        // remaining retry re-inserted an occupied slug and registration failed
        // with "Duplicate entry 'test-user-16'" → HTTP 500 (run D:
        // navigation-matrix + testimonials). `probeLimit` is only a defensive
        // ceiling against a pathological database, never a normal stop condition.
        final int probeLimit = 1000;
        int probe = 0;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // Generate candidate slug: base slug first, then incrementing suffixes
                String candidate = probe == 0 ? baseSlug : baseSlug + "-" + (probe + 1);

                // Quick pre-check to skip known occupied slugs (improves efficiency).
                // Skipped on probe 0 so the base slug still gets its first shot.
                if (probe > 0) {
                    while (probe < probeLimit
                            && (!slugService.isValidSlug(candidate) || userRepository.existsBySlug(candidate))) {
                        probe++;
                        candidate = baseSlug + "-" + (probe + 1);
                    }
                    if (probe >= probeLimit) {
                        // Defensive: absurd number of occupied candidates — fall
                        // through to the error below instead of inserting a
                        // candidate we KNOW is taken.
                        break;
                    }
                }

                user.setSlug(candidate);
                // CallTransactional method via proxy to ensure each attempt is a fresh
                // transaction
                return self.executeRegistrationInTransaction(user);
            } catch (DataIntegrityViolationException e) {
                // This candidate collided — advance the probe so the retry picks
                // a fresh one instead of re-trying the same slug.
                probe++;
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("slug") && attempt < maxRetries - 1) {
                    log.warn("Slug collision for '{}'. Retrying... (Attempt {})", user.getSlug(), attempt + 1);
                } else {
                    throw e;
                }
            }
        }

        throw new RuntimeException("Registration failed due to persistent slug collisions.");
    }

    /**
     * Executes the actual database operations in a new transaction.
     * This allows us to catch DataIntegrityViolationException and retry.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User executeRegistrationInTransaction(User user) {
        User savedUser = userRepository.saveAndFlush(user);

        // Provision default assets
        createDefaultProfileForUser(savedUser);
        sendWelcomeMessage(savedUser);

        return savedUser;
    }

    private void createDefaultProfileForUser(User user) {
        PortfolioProfile profile = new PortfolioProfile();
        profile.setUser(user);
        profile.setPubliclyVisible(false);
        profile.setVisible(true);
        profile.setHeadline("Welcome to Your New Portfolio!");
        profile.setSummary(
                "This is your new portfolio summary. You can tell visitors about yourself, your skills, and your professional goals.");
        portfolioProfileRepository.save(profile);
    }

    private void sendWelcomeMessage(User user) {
        ContactMessage welcomeMessage = new ContactMessage();
        welcomeMessage.setUser(user);
        welcomeMessage.setName("The ForkMyFolio Team");
        welcomeMessage.setEmail("welcome@forkmyfolio.com");
        welcomeMessage.setMessage(
                "Welcome to ForkMyFolio! Your portfolio is set to Private by default. " +
                        "Complete your profile and toggle the 'Portfolio is Public' switch when you're ready!");
        welcomeMessage.setRead(false);
        welcomeMessage.setArchived(false);
        welcomeMessage.setReplied(false);
        welcomeMessage.setPriority(MessagePriority.HIGH);
        contactMessageRepository.save(welcomeMessage);
    }
}