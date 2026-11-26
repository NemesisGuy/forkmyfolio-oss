package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.Policy;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.ContactMessageRepository;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.PolicyService;
import com.forkmyfolio.service.SlugService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SlugService slugService;
    @Mock
    private PolicyService policyService;
    @Mock
    private PortfolioProfileRepository portfolioProfileRepository;
    @Mock
    private ContactMessageRepository contactMessageRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                slugService,
                policyService,
                portfolioProfileRepository,
                contactMessageRepository);
        authService.setSelf(authService);
    }

    @Test
    void registerUser_ShouldCreateUserAndPrivateProfileVerified() {
        // Arrange
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");

        String rawPassword = "password123";
        String termsVersion = "v1.0";

        Policy policy = new Policy("v1.0", "Terms", "Content");
        when(policyService.getTermsOfService()).thenReturn(policy);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        // FIX: AuthServiceImpl calls slugService.toSlug(fullName), not
        // generateUniqueSlug
        when(slugService.toSlug(anyString())).thenReturn("john-doe");

        User savedUserMock = new User();
        savedUserMock.setId(1L);
        savedUserMock.setFirstName("John");
        savedUserMock.setSlug("john-doe");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUserMock);

        // Act
        User result = authService.registerUser(user, rawPassword, termsVersion);

        // Assert
        assertNotNull(result);

        // Verify User Save
        verify(userRepository, times(1)).saveAndFlush(any(User.class));

        // Verify Profile Creation
        ArgumentCaptor<PortfolioProfile> profileCaptor = ArgumentCaptor.forClass(PortfolioProfile.class);
        verify(portfolioProfileRepository, times(1)).save(profileCaptor.capture());

        PortfolioProfile createdProfile = profileCaptor.getValue();
        assertEquals(savedUserMock, createdProfile.getUser());
        assertFalse(createdProfile.isPubliclyVisible(), "Portfolio should be PRIVATE by default");
        assertTrue(createdProfile.isVisible(), "Profile section should be VISIBLE to owner");
        assertNotNull(createdProfile.getHeadline(), "Headline should be populated");
        assertNotNull(createdProfile.getSummary(), "Summary should be populated");
        assertEquals("Welcome to Your New Portfolio!", createdProfile.getHeadline());
    }

    /**
     * Regression: slug pre-check exhaustion must not surface as HTTP 500.
     *
     * <p>The candidate probe used to increment the OUTER retry counter from
     * inside its while-loop. With {@code test-user} plus {@code test-user-2..-10}
     * all occupied (the state the e2e suite leaves behind), the probe walked
     * {@code attempt} up to 10 — past {@code maxRetries - 1} — so the
     * {@code DataIntegrityViolationException} raised by the next save was
     * rethrown instead of retried, and {@code POST /auth/register} returned 500
     * for an otherwise perfectly valid registration.
     *
     * <p>Pin: the probe must stay independent of the retry counter, so the
     * registration still succeeds once a free candidate is reached.
     */
    @Test
    void registerUser_ShouldRecoverWhenSlugProbeExhaustsKnownOccupiedSlugs() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("regression-slug-probe@example.com");

        String rawPassword = "password123";
        String termsVersion = "v1.0";

        when(policyService.getTermsOfService()).thenReturn(new Policy("v1.0", "Terms", "Content"));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(slugService.toSlug(anyString())).thenReturn("test-user");
        // Every candidate the pre-check walks is taken, and the slug itself is
        // valid — this is the "test-user + test-user-2..-11 occupied" state.
        when(slugService.isValidSlug(anyString())).thenReturn(true);
        when(userRepository.existsBySlug(anyString())).thenAnswer(invocation -> {
            String slug = invocation.getArgument(0);
            return !"test-user-12".equals(slug); // -2..-11 (and base) taken; -12 free
        });

        // The unique-constraint violation fires exactly for the occupied slugs
        // the pre-check cannot skip: the base slug (probe 0 skips the pre-check).
        // Everything after that is walked over by the pre-check, which must
        // continue until it finds a FREE candidate.
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            if (!"test-user-12".equals(toSave.getSlug())) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"uk_users_slug\"");
            }
            User saved = new User();
            saved.setId(1L);
            saved.setFirstName(toSave.getFirstName());
            saved.setSlug(toSave.getSlug());
            return saved;
        });

        User result = authService.registerUser(user, rawPassword, termsVersion);

        assertNotNull(result, "Registration must succeed once a free slug candidate is reached");
        assertEquals("test-user-12", result.getSlug());
        // Attempt 0 inserts the (occupied) base slug; attempt 1 pre-check-walks
        // -2..-11 and saves -12 directly: two save attempts total.
        verify(userRepository, times(2)).saveAndFlush(any(User.class));
    }

    /**
     * Regression (e2e run D): the slug pre-check's probe WINDOW must keep
     * walking until a FREE candidate is found — it may never stop on a
     * candidate it knows is occupied.
     *
     * <p>The old loop broke out at {@code probe >= maxRetries + 5} (= 10),
     * leaving the last occupied candidate in place. DEV accumulates
     * {@code test-user-2..-16+} across e2e runs, so once probe passed 10 every
     * remaining transaction retry re-inserted a known-taken slug, burned all
     * attempts, and {@code POST /auth/register} returned HTTP 500
     * ("Duplicate entry 'test-user-16' for key 'users.slug'") while parallel
     * specs (navigation-matrix + testimonials) registered concurrently.
     *
     * <p>Pin: a free candidate far beyond the old window (-50, occupied through
     * -49) must be reached in a single pre-check walk.
     */
    @Test
    void registerUser_ShouldWalkPastOldProbeWindowToAFreeCandidate() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("probe-window-past-cap@example.com");

        when(policyService.getTermsOfService()).thenReturn(new Policy("v1.0", "Terms", "Content"));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(slugService.toSlug(anyString())).thenReturn("test-user");
        when(slugService.isValidSlug(anyString())).thenReturn(true);
        // Occupied far past the old probe-window cap: only -50 is free.
        when(userRepository.existsBySlug(anyString())).thenAnswer(invocation ->
                !"test-user-50".equals(invocation.getArgument(0)));

        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            if (!"test-user-50".equals(toSave.getSlug())) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"uk_users_slug\"");
            }
            User saved = new User();
            saved.setId(1L);
            saved.setFirstName(toSave.getFirstName());
            saved.setSlug(toSave.getSlug());
            return saved;
        });

        User result = authService.registerUser(user, "password123", "v1.0");

        assertNotNull(result, "Registration must reach a free candidate beyond the old probe window");
        assertEquals("test-user-50", result.getSlug());
        // Base slug collision, then ONE walk to -50: exactly two save attempts.
        verify(userRepository, times(2)).saveAndFlush(any(User.class));
    }
}
