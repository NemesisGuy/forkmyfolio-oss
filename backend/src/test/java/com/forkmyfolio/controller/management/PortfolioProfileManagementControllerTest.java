package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.response.PortfolioProfileDto;
import com.forkmyfolio.dto.update.UpdatePortfolioProfileRequest;
import com.forkmyfolio.dto.update.UpdateProfileVisibilityRequest;
import com.forkmyfolio.mapper.PortfolioProfileMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PortfolioProfileManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PortfolioProfileService portfolioProfileService;
    @Mock
    private UserService userService;
    @Mock
    private PortfolioProfileMapper portfolioProfileMapper;

    @InjectMocks
    private PortfolioProfileManagementController portfolioProfileManagementController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioProfileManagementController).build();
        objectMapper = new ObjectMapper();
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    @WithMockUser
    void getMyProfile_shouldReturnProfile() throws Exception {
        PortfolioProfile profile = new PortfolioProfile();
        PortfolioProfileDto profileDto = new PortfolioProfileDto();
        profileDto.setHeadline("Test Headline");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(portfolioProfileService.getProfileByUser(eq(testUser))).thenReturn(profile);
        when(portfolioProfileMapper.toDto(eq(profile))).thenReturn(profileDto);

        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("Test Headline"));
    }

    @Test
    @WithMockUser
    void createOrUpdateMyProfile_shouldUpdateAndReturnProfile() throws Exception {
        UpdatePortfolioProfileRequest request = new UpdatePortfolioProfileRequest();
        request.setHeadline("Updated Headline");

        PortfolioProfile profileUpdates = new PortfolioProfile();
        PortfolioProfile updatedProfile = new PortfolioProfile();
        PortfolioProfileDto updatedProfileDto = new PortfolioProfileDto();
        updatedProfileDto.setHeadline("Updated Headline");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(portfolioProfileMapper.toEntity(any(UpdatePortfolioProfileRequest.class))).thenReturn(profileUpdates);
        when(portfolioProfileService.createOrUpdateProfile(eq(profileUpdates), eq(testUser)))
                .thenReturn(updatedProfile);
        when(portfolioProfileMapper.toDto(eq(updatedProfile))).thenReturn(updatedProfileDto);

        mockMvc.perform(put("/api/v1/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("Updated Headline"));
    }

    @Test
    @WithMockUser
    void updateMyProfileVisibility_shouldUpdateAndReturnProfile() throws Exception {
        UpdateProfileVisibilityRequest request = new UpdateProfileVisibilityRequest();
        request.setPubliclyVisible(true);

        PortfolioProfile updatedProfile = new PortfolioProfile();
        PortfolioProfileDto updatedProfileDto = new PortfolioProfileDto();
        updatedProfileDto.setPubliclyVisible(true);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(portfolioProfileService.updateProfileVisibility(eq(testUser), eq(true))).thenReturn(updatedProfile);
        when(portfolioProfileMapper.toDto(eq(updatedProfile))).thenReturn(updatedProfileDto);

        mockMvc.perform(put("/api/v1/me/profile/visibility")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublic").value(true));
    }
}
