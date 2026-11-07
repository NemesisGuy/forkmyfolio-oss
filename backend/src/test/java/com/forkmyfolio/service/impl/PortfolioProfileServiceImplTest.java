package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioProfileServiceImplTest {

    @Mock
    private PortfolioProfileRepository portfolioProfileRepository;

    @InjectMocks
    private PortfolioProfileServiceImpl portfolioProfileService;

    private User testUser;
    private PortfolioProfile existingProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setSlug("test-user");

        existingProfile = new PortfolioProfile();
        existingProfile.setId(10L);
        existingProfile.setUser(testUser);
        existingProfile.setHeadline("Old Headline");
    }

    @Test
    void getProfileByUser_shouldReturnExistingProfile_whenFound() {
        // Arrange
        when(portfolioProfileRepository.findByUser(testUser)).thenReturn(Optional.of(existingProfile));

        // Act
        PortfolioProfile result = portfolioProfileService.getProfileByUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(existingProfile.getId(), result.getId());
        verify(portfolioProfileRepository).findByUser(testUser);
        verify(portfolioProfileRepository, never()).save(any(PortfolioProfile.class));
    }

    @Test
    void getProfileByUser_shouldCreateAndReturnNewProfile_whenNotFound() {
        // Arrange
        when(portfolioProfileRepository.findByUser(testUser)).thenReturn(Optional.empty());
        // Mock the save operation to return the object that was passed to it
        when(portfolioProfileRepository.save(any(PortfolioProfile.class))).thenAnswer(invocation -> {
            PortfolioProfile newProfile = invocation.getArgument(0);
            newProfile.setId(20L); // Simulate DB assigning an ID
            return newProfile;
        });

        // Act
        PortfolioProfile result = portfolioProfileService.getProfileByUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertNotNull(result.getId()); // Ensure it has an ID from being 'saved'

        verify(portfolioProfileRepository).findByUser(testUser);
        verify(portfolioProfileRepository).save(any(PortfolioProfile.class));
    }

    @Test
    void createOrUpdateProfile_shouldUpdateExistingProfile() {
        // Arrange
        PortfolioProfile updates = new PortfolioProfile();
        updates.setHeadline("New Headline");
        updates.setSummary("New Summary");

        when(portfolioProfileRepository.findByUser(testUser)).thenReturn(Optional.of(existingProfile));
        when(portfolioProfileRepository.save(any(PortfolioProfile.class))).thenReturn(existingProfile);

        // Act
        portfolioProfileService.createOrUpdateProfile(updates, testUser);

        // Assert
        ArgumentCaptor<PortfolioProfile> profileCaptor = ArgumentCaptor.forClass(PortfolioProfile.class);
        verify(portfolioProfileRepository).save(profileCaptor.capture());

        PortfolioProfile savedProfile = profileCaptor.getValue();
        assertEquals("New Headline", savedProfile.getHeadline());
        assertEquals("New Summary", savedProfile.getSummary());
    }

    @Test
    void updateProfileVisibility_shouldSetPublicFlag() {
        // Arrange
        existingProfile.setPubliclyVisible(false);
        when(portfolioProfileRepository.findByUser(testUser)).thenReturn(Optional.of(existingProfile));
        when(portfolioProfileRepository.save(any(PortfolioProfile.class))).thenReturn(existingProfile);

        // Act
        portfolioProfileService.updateProfileVisibility(testUser, true);

        // Assert
        ArgumentCaptor<PortfolioProfile> profileCaptor = ArgumentCaptor.forClass(PortfolioProfile.class);
        verify(portfolioProfileRepository).save(profileCaptor.capture());

        assertTrue(profileCaptor.getValue().isPubliclyVisible());
    }
}
