package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.DuplicateResourceException;
import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.enums.MessagePriority;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.repository.ContactMessageRepository;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.SlugService;
import com.forkmyfolio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlugService slugService;
    private final ContactMessageRepository contactMessageRepository;
    private final PortfolioProfileRepository portfolioProfileRepository;

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
    @CacheEvict(value = "publicPortfolios", key = "#currentUser.slug")
    public User updateUserProfile(String firstName, String lastName, String profileImageUrl) {
        User currentUser = getCurrentAuthenticatedUser();
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setProfileImageUrl(profileImageUrl);
        log.info("Updating profile for user: {}", currentUser.getEmail());
        return userRepository.save(currentUser);
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
            Boolean active) {
        User user = getUserByUuid(uuid);

        // --- Slug Validation ---
        if (!slugService.isSlugAvailable(slug, uuid)) {
            throw new DuplicateResourceException("Slug '" + slug + "' is either reserved or taken by another user.");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setSlug(slug);
        user.setRoles(roles);
        user.setActive(active);
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
            log.info("Admin user '{}' already exists. Ensuring roles and password are up to date.", email);
            admin.getRoles().add(Role.ADMIN);
            admin.getRoles().add(Role.USER);
            // Sync password with configuration
            admin.setPassword(passwordEncoder.encode(password));
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