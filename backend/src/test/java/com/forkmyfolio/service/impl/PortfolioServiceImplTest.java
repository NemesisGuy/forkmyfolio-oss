package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.PortfolioProfileService;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PortfolioProfileService portfolioProfileService;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private User testUser;
    private PortfolioProfile publicProfile;
    private PortfolioProfile privateProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setSlug("test-slug");
        // Initialize collections to avoid NullPointerExceptions when the service tries
        // to initialize them
        testUser.setProjects(Collections.emptySet());
        testUser.setExperiences(Collections.emptySet());
        testUser.setUserSkills(Collections.emptySet());

        publicProfile = new PortfolioProfile();
        publicProfile.setPubliclyVisible(true);
        testUser.setPortfolioProfile(publicProfile);

        privateProfile = new PortfolioProfile();
        privateProfile.setPubliclyVisible(false);
    }

    @Test
    void getPublicPortfolioUserBySlug_shouldReturnUser_whenProfileIsPublic() {
        // Arrange
        when(userService.findBySlug("test-slug")).thenReturn(Optional.of(testUser));
        when(portfolioProfileService.getProfileByUser(testUser)).thenReturn(publicProfile);

        // Act
        User result = portfolioService.getPublicPortfolioUserBySlug("test-slug");

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(userService).findBySlug("test-slug");
        verify(portfolioProfileService).getProfileByUser(testUser);
    }

    @Test
    void getPublicPortfolioUserBySlug_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userService.findBySlug("non-existent-slug")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            portfolioService.getPublicPortfolioUserBySlug("non-existent-slug");
        });

        verify(userService).findBySlug("non-existent-slug");
        verifyNoInteractions(portfolioProfileService);
    }

    @Test
    void getPublicPortfolioUserBySlug_shouldConcealPrivateProfileAsNotFound() {
        // Phase 7.4 (403/404 policy): a private portfolio must be
        // indistinguishable from a nonexistent slug for the anonymous surface —
        // the old 403 "This portfolio is private." disclosed that the slug
        // exists. Both branches throw ResourceNotFoundException now.
        // Arrange
        testUser.setPortfolioProfile(privateProfile);
        when(userService.findBySlug("test-slug")).thenReturn(Optional.of(testUser));
        when(portfolioProfileService.getProfileByUser(testUser)).thenReturn(privateProfile);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            portfolioService.getPublicPortfolioUserBySlug("test-slug");
        });

        verify(userService).findBySlug("test-slug");
        verify(portfolioProfileService).getProfileByUser(testUser);
    }
}
