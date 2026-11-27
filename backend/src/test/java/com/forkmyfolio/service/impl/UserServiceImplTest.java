package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.DuplicateResourceException;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.enums.VisitorStatType;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SlugService slugService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private ContactMessageRepository contactMessageRepository;
    @Mock
    private PortfolioProfileRepository portfolioProfileRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementReadRepository announcementReadRepository;
    @Mock
    private GuestPassRepository guestPassRepository;
    @Mock
    private JobApplicationRepository jobApplicationRepository;
    @Mock
    private UserAnalyticsConfigRepository userAnalyticsConfigRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private VisitorStatsRepository visitorStatsRepository;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("encodedPassword");
        testUser.setSlug("test-user");
        testUser.setRoles(Set.of(Role.USER));
        testUser.setActive(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_shouldCreateAndReturnNewUser() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(slugService.generateUniqueSlug(anyString())).thenReturn("new-user");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User newUser = userService.registerUser("new@example.com", "password", "New", "User", null, null, true);

        // Assert
        assertNotNull(newUser);
        assertEquals("new@example.com", newUser.getEmail());
        assertEquals("encodedPassword", newUser.getPassword());
        assertEquals("new-user", newUser.getSlug());
        assertEquals(AuthProvider.LOCAL, newUser.getProvider());
        assertTrue(newUser.getRoles().contains(Role.USER));
        assertTrue(newUser.isActive());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createOrUpdateAdminUser_shouldNotOverwriteAnExistingPassword() {
        // Arrange: an admin whose password was changed in the UI.
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPassword("ui-changed-password-hash");
        admin.setRoles(new HashSet<>(Set.of(Role.USER)));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        // Act
        userService.createOrUpdateAdminUser("admin@example.com", "env-bootstrap-password", "Admin", "User");

        // Assert: set-once bootstrap — the stored password survives restarts.
        assertEquals("ui-changed-password-hash", admin.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        assertTrue(admin.getRoles().contains(Role.ADMIN));
        assertTrue(admin.getRoles().contains(Role.USER));
        verify(userRepository).save(admin);
    }

    @Test
    void registerUser_shouldThrowException_whenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.registerUser("test@example.com", "password", "Test", "User", null, null, true);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findBySlug_shouldReturnUser_whenActiveUserExists() {
        // Arrange
        when(userRepository.findBySlugAndActiveTrue("test-user")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> foundUser = userService.findBySlug("test-user");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    void findBySlug_shouldReturnEmpty_whenUserNotExistsOrInactive() {
        // Arrange
        when(userRepository.findBySlugAndActiveTrue("inactive-user")).thenReturn(Optional.empty());

        // Act
        Optional<User> foundUser = userService.findBySlug("inactive-user");

        // Assert
        assertFalse(foundUser.isPresent());
    }

    // v1.2.11 SECURITY pins: deactivation must kill live refresh sessions,
    // via BOTH the dedicated endpoint and the admin PUT (a known bypass).
    @Test
    void deactivateUser_revokesAllRefreshTokens() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));

        userService.deactivateUser(uuid);

        assertFalse(testUser.isActive());
        verify(userRepository).save(testUser);
        verify(refreshTokenService).deleteByUserId(1L);
    }

    @Test
    void updateUserByAdmin_whenDeactivating_revokesAllRefreshTokens() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));
        when(slugService.isSlugAvailable("test-user", uuid)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUserByAdmin(uuid, "Test", "User", "test-user",
                Set.of(Role.USER), false);

        assertFalse(updated.isActive());
        verify(refreshTokenService).deleteByUserId(1L);
    }

    @Test
    void updateUserByAdmin_whenActivating_keepsRefreshTokens() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));
        when(slugService.isSlugAvailable("test-user", uuid)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserByAdmin(uuid, "Test", "User", "test-user", Set.of(Role.USER), true);

        verify(refreshTokenService, never()).deleteByUserId(anyLong());
    }

    // --- Restore (soft-delete reversal) ---

    @Test
    void restoreUser_setsActiveWithoutTouchingPasswordRolesOrSlug() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        testUser.setActive(false);
        testUser.setPassword("unchanged-hash");
        Set<Role> originalRoles = new HashSet<>(Set.of(Role.USER, Role.ADMIN));
        testUser.setRoles(originalRoles);
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));

        User restored = userService.restoreUser(uuid);

        assertTrue(restored.isActive());
        assertEquals("unchanged-hash", restored.getPassword());
        assertEquals(originalRoles, restored.getRoles());
        assertEquals("test-user", restored.getSlug());
        assertEquals("test@example.com", restored.getEmail());
        verify(userRepository).save(testUser);
        // Restoring must NOT revoke sessions or delete any portfolio data.
        verifyNoInteractions(refreshTokenService, contactMessageRepository, guestPassRepository,
                jobApplicationRepository, userAnalyticsConfigRepository, visitorStatsRepository);
    }

    @Test
    void restoreUser_whenAlreadyActive_isIdempotent() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));

        User restored = userService.restoreUser(uuid);

        assertSame(testUser, restored);
        assertTrue(restored.isActive());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Hard delete ---

    @Test
    void hardDeleteUser_removesUserAndAllExplicitlyCleanedChildRows() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));

        Project project = new Project();
        project.setUuid(UUID.randomUUID());
        when(projectRepository.findByUser(testUser)).thenReturn(List.of(project));

        userService.hardDeleteUser(uuid);

        verify(refreshTokenService).deleteByUserId(1L);
        verify(announcementReadRepository).deleteByUser(testUser);
        verify(announcementRepository).clearSender(testUser);
        verify(guestPassRepository).deleteByOwner(testUser);
        verify(jobApplicationRepository).deleteByOwner(testUser);
        verify(userAnalyticsConfigRepository).deleteByUserId(1L);
        verify(contactMessageRepository).deleteByUser(testUser);
        verify(visitorStatsRepository).deleteByTypeAndRefIds(
                eq(VisitorStatType.PORTFOLIO_VIEW), eq(List.of("test-user")));
        verify(visitorStatsRepository).deleteByTypeAndRefIds(
                eq(VisitorStatType.PROJECT_VIEW), eq(List.of(project.getUuid().toString())));
        verify(userRepository).delete(testUser);
        verify(userRepository).flush();
    }

    @Test
    void hardDeleteUser_refusesToDeleteTheCurrentlyAuthenticatedAdmin() {
        UUID targetUuid = UUID.randomUUID();
        testUser.setUuid(targetUuid);
        when(userRepository.findByUuid(targetUuid)).thenReturn(Optional.of(testUser));

        // The authenticated principal resolves to the SAME account as the target.
        testUser.setRoles(new HashSet<>(Set.of(Role.USER, Role.ADMIN)));
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com")
                .password("hash")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.hardDeleteUser(targetUuid));
        assertTrue(ex.getMessage().contains("own account"));

        verify(userRepository, never()).delete(any(User.class));
        verifyNoInteractions(refreshTokenService, guestPassRepository, announcementReadRepository);
    }

    @Test
    void hardDeleteUser_refusesToDeleteTheLastRemainingActiveAdmin() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        testUser.setRoles(new HashSet<>(Set.of(Role.ADMIN, Role.USER)));
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));
        when(userRepository.countByRolesContainingAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.hardDeleteUser(uuid));
        assertTrue(ex.getMessage().contains("last remaining active ADMIN"));

        verify(userRepository, never()).delete(any(User.class));
        verifyNoInteractions(refreshTokenService, guestPassRepository, announcementReadRepository);
    }

    @Test
    void hardDeleteUser_allowsDeletingAnAdminWhenAnotherActiveAdminRemains() {
        UUID uuid = UUID.randomUUID();
        testUser.setUuid(uuid);
        testUser.setRoles(new HashSet<>(Set.of(Role.ADMIN, Role.USER)));
        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(testUser));
        when(userRepository.countByRolesContainingAndActiveTrue(Role.ADMIN)).thenReturn(2L);
        when(projectRepository.findByUser(testUser)).thenReturn(List.of());

        userService.hardDeleteUser(uuid);

        verify(userRepository).delete(testUser);
    }
}
