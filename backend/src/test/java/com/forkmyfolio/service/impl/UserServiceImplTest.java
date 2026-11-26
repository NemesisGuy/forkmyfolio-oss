package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.DuplicateResourceException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.RefreshTokenService;
import com.forkmyfolio.service.SlugService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
}
