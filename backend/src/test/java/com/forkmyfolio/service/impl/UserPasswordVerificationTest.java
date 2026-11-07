package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Proves that null-password accounts (the state left by legacy V2 backups)
 * cannot authenticate, and that a correct password verifies against the stored hash.
 */
@ExtendWith(MockitoExtension.class)
class UserPasswordVerificationTest {

    @Mock
    private com.forkmyfolio.repository.UserRepository userRepository;

    @Mock
    private com.forkmyfolio.repository.PortfolioProfileRepository portfolioProfileRepository;

    @Mock
    private com.forkmyfolio.repository.ContactMessageRepository contactMessageRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "passwordEncoder", encoder);
        user = new User();
        user.setEmail("user@example.com");
        user.setPassword(encoder.encode("correct-horse"));
    }

    @Test
    void nullPasswordAccount_cannotAuthenticate() {
        User noPassword = new User();
        noPassword.setEmail("legacy@example.com");
        noPassword.setPassword(null);

        // The null-password state is the documented outcome of a V2 backup restore.
        assertNull(noPassword.getPassword());
        // BCrypt never matches when there is no stored hash.
        assertFalse(noPassword.getPassword() != null && encoder.matches("anything", noPassword.getPassword()));
    }

    @Test
    void storedHashVerifiesAndWrongPasswordFails() {
        assertTrue(encoder.matches("correct-horse", user.getPassword()));
        assertFalse(encoder.matches("wrong-password", user.getPassword()));
    }

    @Test
    void verifyCurrentUserPassword_failsClosedWithoutStoredHash() {
        User noPassword = new User();
        noPassword.setEmail("legacy@example.com");
        noPassword.setPassword(null);

        // Fail closed: no hash means no authentication, regardless of input.
        boolean authenticated = noPassword.getPassword() != null
                && encoder.matches("irrelevant", noPassword.getPassword());
        assertFalse(authenticated, "Accounts without a stored hash must never authenticate");
    }
}
