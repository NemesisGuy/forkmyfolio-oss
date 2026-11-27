package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.DuplicateResourceException;
import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.enums.MessagePriority;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.enums.VisitorStatType;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.repository.AnnouncementReadRepository;
import com.forkmyfolio.repository.AnnouncementRepository;
import com.forkmyfolio.repository.ContactMessageRepository;
import com.forkmyfolio.repository.GuestPassRepository;
import com.forkmyfolio.repository.JobApplicationRepository;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import com.forkmyfolio.repository.ProjectRepository;
import com.forkmyfolio.repository.UserAnalyticsConfigRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.repository.VisitorStatsRepository;
import com.forkmyfolio.service.RefreshTokenService;
import com.forkmyfolio.service.SlugService;
import com.forkmyfolio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String PUBLIC_PORTFOLIO_CACHE = "publicPortfolios";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlugService slugService;
    private final ContactMessageRepository contactMessageRepository;
    private final PortfolioProfileRepository portfolioProfileRepository;
    // v1.2.11 SECURITY: deactivation must revoke live refresh sessions.
    private final RefreshTokenService refreshTokenService;

    // Hard-delete cleanup of rows that the User entity's cascade/orphanRemoval
    // does NOT cover (their FKs have no ON DELETE CASCADE, or they are keyed by a
    // string reference such as the portfolio slug).
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final GuestPassRepository guestPassRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserAnalyticsConfigRepository userAnalyticsConfigRepository;
    private final ProjectRepository projectRepository;
    private final VisitorStatsRepository visitorStatsRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public User registerUser(String email, String password, String firstName, String lastName, String profileImageUrl,
            Set<Role> roles, Boolean active) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already in use: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProfileImageUrl(profileImageUrl);
        user.setSlug(slugService.generateUniqueSlug(firstName + " " + lastName));
        user.setProvider(AuthProvider.LOCAL); // Set default provider for local registration

        if (roles == null || roles.isEmpty()) {
            user.setRoles(Set.of(Role.USER));
        } else {
            user.setRoles(roles);
        }

        user.setActive(active != null ? active : true);

        log.info("Registering new user with email: {}", email);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Anonymous requests carry the literal "anonymousUser" principal and
        // must resolve to empty — never to a user record.
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {
            return Optional.empty();
        }
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCurrentUser(User candidate) {
        // Phase 8 (PRV-1): identity is compared on the ACCOUNT email. An
        // anonymous or unrecognized principal can never match, so guests and
        // cross-user requests can never be treated as the owner.
        return findCurrentAuthenticatedUser()
                .map(current -> current.getEmail().equals(candidate.getEmail()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUserWithAllPortfolioData() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional
    public User updateUserProfile(String firstName, String lastName, String profileImageUrl) {
        User currentUser = getCurrentAuthenticatedUser();
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setProfileImageUrl(profileImageUrl);
        log.info("Updating profile for user: {}", currentUser.getEmail());
        User saved = userRepository.save(currentUser);
        // The old @CacheEvict(key = "#currentUser.slug") referenced a parameter
        // that no longer exists after the security-context refactor. Spring
        // resolved it to null and threw EL1007E ("slug cannot be found on
        // null") BEFORE the body ran, turning every account edit into HTTP 500.
        // Evict programmatically with the loaded user's real slug instead.
        evictPublicPortfolioCache(saved.getSlug());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsersWithPortfolioData() {
        return userRepository.findAllForBackup();
    }

    @Override
    @Transactional
    @CacheEvict(value = "publicPortfolios", key = "#slug")
    public User updateUserByAdmin(UUID uuid, String firstName, String lastName, String slug, Set<Role> roles,
            Boolean active, String email) {
        User user = getUserByUuid(uuid);

        // --- Slug Validation ---
        if (!slugService.isSlugAvailable(slug, uuid)) {
            throw new DuplicateResourceException("Slug '" + slug + "' is either reserved or taken by another user.");
        }

        // --- Email (optional) ---
        // The admin edit form has always submitted this field; older backends
        // silently discarded it. Null/blank is a genuine no-op so legacy admin
        // clients that never send it keep working. A collision with a DIFFERENT
        // account is rejected before the unique constraint can throw a 500.
        if (StringUtils.hasText(email)) {
            String newEmail = email.trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                Optional<User> collision = userRepository.findByEmail(newEmail);
                if (collision.isPresent() && !collision.get().getUuid().equals(user.getUuid())) {
                    throw new DuplicateResourceException("Email is already in use: " + newEmail);
                }
                user.setEmail(newEmail);
                log.info("Admin changed login email for user UUID: {}", uuid);
            }
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setSlug(slug);
        // Hibernate's merge clears/rebuilds the roles collection. An immutable
        // Set.of(...) (any programmatic caller, and the same class of bug fixed
        // for SSO in v1.3.3) throws UnsupportedOperationException on that path —
        // always hand it a mutable copy.
        user.setRoles(roles == null ? null : new HashSet<>(roles));
        user.setActive(active);
        // v1.2.11 SECURITY: an admin PUT that deactivates an account is just
        // as dangerous as the dedicated deactivation endpoint — revoke every
        // refresh token so the user cannot simply refresh their way back in.
        if (Boolean.FALSE.equals(active)) {
            refreshTokenService.deleteByUserId(user.getId());
        }
        log.info("Admin updated user with UUID: {}", uuid);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID uuid) {
        User user = getUserByUuid(uuid);
        user.setActive(false);
        log.warn("Deactivating user with UUID: {}", uuid);
        userRepository.save(user);
        // v1.2.11 SECURITY: revoke all refresh tokens — otherwise the
        // deactivated user keeps a working session until expiry.
        refreshTokenService.deleteByUserId(user.getId());
    }

    @Override
    @Transactional
    public User restoreUser(UUID uuid) {
        User user = getUserByUuid(uuid);
        if (user.isActive()) {
            // Idempotent: an already-active account needs no write at all.
            log.debug("User with UUID {} is already active — restore is a no-op.", uuid);
        } else {
            user.setActive(true);
            userRepository.save(user);
            log.info("Restored (reactivated) user with UUID: {}", uuid);
        }
        // A deactivated user's cached public portfolio may predate the restore.
        evictPublicPortfolioCache(user.getSlug());
        return user;
    }

    @Override
    @Transactional
    public void hardDeleteUser(UUID uuid) {
        User user = getUserByUuid(uuid);

        // --- Safety guards (before ANY destructive write) ---
        Optional<User> currentUser = findCurrentAuthenticatedUser();
        if (currentUser.isPresent() && isSameAccount(currentUser.get(), user)) {
            throw new IllegalArgumentException("You cannot hard-delete your own account.");
        }
        if (user.isActive() && user.getRoles() != null && user.getRoles().contains(Role.ADMIN)
                && userRepository.countByRolesContainingAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Cannot hard-delete the last remaining active ADMIN account.");
        }

        String slug = user.getSlug();
        // Project UUIDs are the ref_id used by PROJECT_VIEW visitor stats — collect
        // them BEFORE the user's projects are cascade-deleted.
        List<String> projectStatRefIds = projectRepository.findByUser(user).stream()
                .map(Project::getUuid)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .toList();

        // --- Explicit child cleanup, ordered for real MySQL FKs ---
        // 1. Sessions first: no token may survive pointing at a removed user.
        refreshTokenService.deleteByUserId(user.getId());
        // 2. Announcement read receipts (FK user_id has no ON DELETE CASCADE).
        announcementReadRepository.deleteByUser(user);
        // 3. Announcements SENT by this user: keep the platform announcement and
        //    detach it (sender_id is nullable, no cascade).
        announcementRepository.clearSender(user);
        // 4. Guest passes issued by this owner (DB has ON DELETE CASCADE, but the
        //    association is not part of User, so delete explicitly).
        guestPassRepository.deleteByOwner(user);
        // 5. Job applications; their notes are removed by the entity cascade.
        jobApplicationRepository.deleteByOwner(user);
        // 6. Per-user analytics configuration.
        userAnalyticsConfigRepository.deleteByUserId(user.getId());
        // 7. Contact messages (also cascaded from User — removed explicitly so
        //    the two paths agree).
        contactMessageRepository.deleteByUser(user);
        // 8. Visitor stats have no FK at all: remove counters keyed by the
        //    portfolio slug and by this user's project UUIDs.
        if (slug != null && !slug.isBlank()) {
            visitorStatsRepository.deleteByTypeAndRefIds(VisitorStatType.PORTFOLIO_VIEW, List.of(slug));
        }
        if (!projectStatRefIds.isEmpty()) {
            visitorStatsRepository.deleteByTypeAndRefIds(VisitorStatType.PROJECT_VIEW, projectStatRefIds);
        }

        // 9. Finally the user row: Hibernate cascades portfolio profile, projects,
        //    skills, experience, qualifications, testimonials, settings, roles and
        //    any remaining child collections.
        userRepository.delete(user);
        userRepository.flush();
        evictPublicPortfolioCache(slug);
        log.warn("Hard-deleted user with UUID {} (email: {}).", uuid, user.getEmail());
    }

    /** Identity check used by the self-delete guard (DB id is authoritative). */
    private static boolean isSameAccount(User a, User b) {
        if (a.getId() != null && b.getId() != null) {
            return Objects.equals(a.getId(), b.getId());
        }
        return a.getUuid() != null && Objects.equals(a.getUuid(), b.getUuid());
    }

    /** Best-effort cache invalidation — a Redis hiccup must not fail the operation. */
    private void evictPublicPortfolioCache(String slug) {
        if (slug == null || slug.isBlank() || cacheManager == null) {
            return;
        }
        try {
            Cache cache = cacheManager.getCache(PUBLIC_PORTFOLIO_CACHE);
            if (cache != null) {
                cache.evict(slug);
            }
        } catch (RuntimeException e) {
            log.warn("Could not evict public portfolio cache for slug '{}': {}", slug, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findBySlug(String slug) {
        return userRepository.findBySlugAndActiveTrue(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findBySlugWithAllPortfolioData(String slug) {
        return userRepository.findBySlugWithProfile(slug);
    }

    /**
     * Finds a user by their email address.
     *
     * @param email The email to search for.
     * @return An {@link Optional} containing the {@link User} if found.
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUuid(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User with UUID: " + uuid));
    }

    @Override
    @Transactional
    public void createOrUpdateAdminUser(String email, String password, String firstName, String lastName) {
        Optional<User> existingAdmin = userRepository.findByEmail(email);

        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            // Set-once bootstrap: ADMIN_PASSWORD seeds a NEW admin only. An
            // existing admin keeps whatever password they set in the UI, so
            // a restart can never silently revert it to the environment
            // value (and a leaked env var cannot reset a live account).
            log.info("Admin user '{}' already exists. Ensuring roles are present; leaving the stored password unchanged.", email);
            admin.getRoles().add(Role.ADMIN);
            admin.getRoles().add(Role.USER);
            userRepository.save(admin);
        } else {
            log.info("Admin user '{}' not found. Creating new admin user.", email);
            User admin = new User();
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setFirstName(firstName);
            admin.setLastName(lastName);
            admin.setSlug(slugService.generateUniqueSlug(firstName + " " + lastName));
            admin.setRoles(new HashSet<>(Set.of(Role.ADMIN, Role.USER)));
            admin.setActive(true);
            admin.setProvider(AuthProvider.LOCAL); // Set provider for admin creation
            // passwordLastChangedAt will be null, forcing a password change on first login
            User savedAdmin = userRepository.save(admin);
            createDefaultProfileForUser(savedAdmin);
            sendAdminWelcomeMessage(savedAdmin); // Specific message about changing password
            sendWelcomeMessage(savedAdmin); // Standard message about setting up portfolio
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyCurrentUserPassword(String rawPassword) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser.getPassword() == null || rawPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, currentUser.getPassword());
    }

    /**
     * Derives the effective announcement-targeting role of the current user.
     * An ADMIN falls back to the USER role unless overridden; this keeps the
     * role decision out of the controller (NAG-01).
     */
    @Override
    @Transactional(readOnly = true)
    public Role getEffectiveRoleOfCurrentUser() {
        User currentUser = getCurrentAuthenticatedUser();
        return currentUser.getRoles() != null && currentUser.getRoles().contains(Role.ADMIN)
                ? Role.ADMIN
                : Role.USER;
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(String newPassword) {
        User currentUser = getCurrentAuthenticatedUser();
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        currentUser.setPasswordLastChangedAt(Instant.now());
        userRepository.save(currentUser);
        log.info("User {} successfully changed their password.", currentUser.getEmail());
    }

    @Override
    @Transactional
    public void resetUserPassword(java.util.UUID uuid, String newPassword) {
        User target = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User with UUID: " + uuid));
        target.setPassword(passwordEncoder.encode(newPassword));
        target.setPasswordLastChangedAt(java.time.Instant.now());
        userRepository.save(target);
        log.info("Password reset for user {} by administrator.", target.getEmail());
    }

    @Override
    @Transactional
    public void acceptTermsForCurrentUser() {
        User currentUser = getCurrentAuthenticatedUser();
        currentUser.setTermsAcceptedAt(Instant.now());
        // For audit purposes, we should store which version of the terms was accepted.
        // A simple date stamp is a good default. This could be externalized to config
        // later.
        currentUser.setTermsVersion("v" + java.time.LocalDate.now());
        userRepository.save(currentUser);
        log.info("User {} has accepted the terms and conditions.", currentUser.getEmail());
    }

    /**
     * Creates and saves a welcome message for a new administrator.
     * This message guides them on the next steps and informs them about the
     * mandatory password change.
     *
     * @param admin The newly created admin user.
     */
    private void sendAdminWelcomeMessage(User admin) {
        ContactMessage welcomeMessage = new ContactMessage();
        welcomeMessage.setUser(admin);
        welcomeMessage.setName("ForkMyFolio System");
        welcomeMessage.setEmail("system@forkmyfolio.com");
        welcomeMessage.setMessage(
                "Welcome, Administrator!\n\n" +
                        "Your admin account has been successfully set up. For security reasons, you are required to change your temporary password immediately.\n\n"
                        +
                        "Once you've updated your password, you will have full access to the admin dashboard where you can manage users, view system statistics, and perform other administrative tasks.\n\n"
                        +
                        "Thank you for keeping the system secure.\n\n" +
                        "The ForkMyFolio Team");
        welcomeMessage.setRead(false);
        welcomeMessage.setArchived(false);
        welcomeMessage.setReplied(false);
        welcomeMessage.setPriority(MessagePriority.HIGH);
        contactMessageRepository.save(welcomeMessage);
        log.info("Admin welcome message sent to {}.", admin.getEmail());
    }

    /**
     * Creates a default, public-facing portfolio profile for a new user.
     * This ensures that new users have a profile to edit immediately and that it's
     * visible by default.
     *
     * @param user The newly registered user.
     */
    private void createDefaultProfileForUser(User user) {
        PortfolioProfile profile = new PortfolioProfile();
        profile.setUser(user);
        profile.setPubliclyVisible(true); // Make the portfolio public by default.
        profile.setVisible(true); // Ensure the profile section itself is visible.
        profile.setHeadline("Welcome to Your New Portfolio!");
        profile.setSummary(
                "This is your new portfolio summary. You can edit this text to tell visitors about yourself, your skills, and your professional goals. Make it engaging and unique!");
        portfolioProfileRepository.save(profile);
        log.info("Created default portfolio profile for user {}.", user.getEmail());
    }

    /**
     * Creates and saves a standard welcome message for a new user.
     * This message guides them on the next steps and informs them about the default
     * public visibility.
     *
     * @param user The newly registered user.
     */
    private void sendWelcomeMessage(User user) {
        ContactMessage welcomeMessage = new ContactMessage();
        welcomeMessage.setUser(user);
        welcomeMessage.setName("The ForkMyFolio Team");
        welcomeMessage.setEmail("welcome@forkmyfolio.com");
        welcomeMessage.setMessage(
                "Welcome to ForkMyFolio! We're excited to have you on board.\n\nYour account has been created successfully. Here are a few next steps to get your portfolio looking great:\n\n1.  **Complete Your Profile:** Navigate to the 'My Portfolio' sections in the dashboard to add your work experience, projects, skills, and more.\n2.  **Customize Your Look:** Check out the settings to choose a theme and personalize your public page.\n\n**Important Note:** Your portfolio is set to **public** by default so you can share it right away. If you're not ready for the world to see it yet, you can easily make it private. Just go to **Display Settings** and toggle the **'Portfolio is Public'** switch to the OFF position.\n\nWe can't wait to see what you create!\n\nBest,\nThe ForkMyFolio Team");
        welcomeMessage.setRead(false); // Mark as unread
        welcomeMessage.setArchived(false);
        welcomeMessage.setReplied(false);
        welcomeMessage.setPriority(MessagePriority.HIGH);
        contactMessageRepository.save(welcomeMessage);
        log.info("Standard welcome message sent to {}.", user.getEmail());
    }
}