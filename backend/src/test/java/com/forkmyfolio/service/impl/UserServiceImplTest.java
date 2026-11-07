package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.DuplicateResourceException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.SlugService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

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
}
