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
}
