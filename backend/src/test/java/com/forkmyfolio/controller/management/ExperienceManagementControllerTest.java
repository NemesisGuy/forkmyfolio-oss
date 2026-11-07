package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.forkmyfolio.dto.create.CreateExperienceRequest;
import com.forkmyfolio.dto.response.ExperienceDto;
import com.forkmyfolio.dto.update.UpdateExperienceRequest;
import com.forkmyfolio.mapper.ExperienceMapper;
import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.ExperienceService;
import com.forkmyfolio.service.UserSkillService;
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

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExperienceManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ExperienceService experienceService;
    @Mock
    private UserService userService;
    @Mock
    private UserSkillService userSkillService;
    @Mock
    private ExperienceMapper experienceMapper;

    @InjectMocks
    private ExperienceManagementController experienceManagementController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(experienceManagementController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    @WithMockUser
    void getMyExperiences_shouldReturnExperiences() throws Exception {
        Experience experience = new Experience();
        List<Experience> experiences = Collections.singletonList(experience);
        ExperienceDto experienceDto = new ExperienceDto();
        experienceDto.setJobTitle("Software Engineer");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(experienceService.getExperiencesForUser(eq(testUser))).thenReturn(experiences);
        when(userSkillService.getUserSkillLookupMap(eq(testUser))).thenReturn(Collections.emptyMap());
        when(experienceMapper.toDto(eq(experience), any(Map.class))).thenReturn(experienceDto);

        mockMvc.perform(get("/api/v1/me/experiences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobTitle").value("Software Engineer"));
    }

    @Test
    @WithMockUser
    void createMyExperience_shouldCreateAndReturnExperience() throws Exception {
        CreateExperienceRequest request = new CreateExperienceRequest();
        request.setJobTitle("New Role");
        request.setCompanyName("New Company");
        request.setStartDate(LocalDate.now());
        request.setVisible(true);
        request.setDisplayOrder(0);
        request.setSkills(Collections.emptySet());

        Experience newExperienceDetails = new Experience();
        Experience createdExperience = new Experience();
        ExperienceDto createdExperienceDto = new ExperienceDto();
        createdExperienceDto.setJobTitle("New Role");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(experienceMapper.toEntity(any(CreateExperienceRequest.class))).thenReturn(newExperienceDetails);
        when(experienceService.createExperience(eq(newExperienceDetails), anySet(), eq(testUser)))
                .thenReturn(createdExperience);
        when(userSkillService.getUserSkillLookupMap(eq(testUser))).thenReturn(Collections.emptyMap());
        when(experienceMapper.toDto(eq(createdExperience), any(Map.class))).thenReturn(createdExperienceDto);

        mockMvc.perform(post("/api/v1/me/experiences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.jobTitle").value("New Role"));
    }

    @Test
    @WithMockUser
    void updateMyExperience_shouldUpdateAndReturnExperience() throws Exception {
        UUID experienceId = UUID.randomUUID();
        UpdateExperienceRequest request = new UpdateExperienceRequest();
        request.setJobTitle("Updated Role");
        request.setCompanyName("Updated Company");
        request.setStartDate(LocalDate.now());
        request.setVisible(true);
        request.setDisplayOrder(0);
        request.setSkills(Collections.emptySet());

        Experience updatedExperienceData = new Experience();
        Experience updatedExperience = new Experience();
        ExperienceDto updatedExperienceDto = new ExperienceDto();
        updatedExperienceDto.setJobTitle("Updated Role");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(experienceMapper.toEntity(any(UpdateExperienceRequest.class))).thenReturn(updatedExperienceData);
        when(experienceService.updateExperience(eq(experienceId), eq(updatedExperienceData), anySet(), eq(testUser)))
                .thenReturn(updatedExperience);
        when(userSkillService.getUserSkillLookupMap(eq(testUser))).thenReturn(Collections.emptyMap());
        when(experienceMapper.toDto(eq(updatedExperience), any(Map.class))).thenReturn(updatedExperienceDto);

        mockMvc.perform(put("/api/v1/me/experiences/{uuid}", experienceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobTitle").value("Updated Role"));
    }

    @Test
    @WithMockUser
    void deleteMyExperience_shouldReturnNoContent() throws Exception {
        UUID experienceId = UUID.randomUUID();

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        doNothing().when(experienceService).deleteExperience(eq(experienceId), eq(testUser));

        mockMvc.perform(delete("/api/v1/me/experiences/{uuid}", experienceId))
                .andExpect(status().isNoContent());

        verify(experienceService, times(1)).deleteExperience(eq(experienceId), eq(testUser));
    }
}
