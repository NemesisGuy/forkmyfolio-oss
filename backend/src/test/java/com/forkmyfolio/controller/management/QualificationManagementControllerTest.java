package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.create.CreateQualificationRequest;
import com.forkmyfolio.dto.response.QualificationDto;
import com.forkmyfolio.dto.update.UpdateQualificationRequest;
import com.forkmyfolio.mapper.QualificationMapper;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.service.QualificationService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QualificationManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private QualificationService qualificationService;
    @Mock
    private com.forkmyfolio.service.UserService userService;
    @Mock
    private QualificationMapper qualificationMapper;

    @InjectMocks
    private QualificationManagementController qualificationManagementController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(qualificationManagementController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser
    void getMyQualifications_shouldReturnQualifications() throws Exception {
        Qualification qualification = new Qualification();
        List<Qualification> qualifications = Collections.singletonList(qualification);
        QualificationDto qualificationDto = new QualificationDto();
        qualificationDto.setQualificationName("BSc Computer Science");
        List<QualificationDto> qualificationDtos = Collections.singletonList(qualificationDto);

        when(qualificationService.getQualificationsForCurrentUser()).thenReturn(qualifications);
        when(qualificationMapper.toDtoList(eq(qualifications))).thenReturn(qualificationDtos);

        mockMvc.perform(get("/api/v1/me/qualifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].qualificationName").value("BSc Computer Science"));
    }

    @Test
    @WithMockUser
    void createMyQualification_shouldCreateAndReturnQualification() throws Exception {
        CreateQualificationRequest request = new CreateQualificationRequest();
        request.setQualificationName("New Degree");
        request.setInstitutionName("Test University");
        request.setStillStudying(false);
        request.setVisible(true);

        com.forkmyfolio.model.User user = new com.forkmyfolio.model.User();
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);

        Qualification createdQualification = new Qualification();
        QualificationDto createdQualificationDto = new QualificationDto();
        createdQualificationDto.setQualificationName("New Degree");

        when(qualificationService.createQualification(
                eq(request.getQualificationName()),
                eq(request.getInstitutionName()),
                eq(request.getInstitutionLogoUrl()),
                eq(request.getInstitutionWebsite()),
                eq(request.getFieldOfStudy()),
                eq(request.getLevel()),
                eq(request.getStartYear()),
                eq(request.getCompletionYear()),
                eq(request.getStillStudying()),
                eq(request.getGrade()),
                eq(request.getCredentialUrl()),
                eq(request.isVisible()),
                eq(user)))
                .thenReturn(createdQualification);

        when(qualificationMapper.toDto(eq(createdQualification))).thenReturn(createdQualificationDto);

        mockMvc.perform(post("/api/v1/me/qualifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qualificationName").value("New Degree"));
    }

    @Test
    @WithMockUser
    void updateMyQualification_shouldUpdateAndReturnQualification() throws Exception {
        UUID qualificationId = UUID.randomUUID();
        UpdateQualificationRequest request = new UpdateQualificationRequest();
        request.setQualificationName("Updated Degree");
        request.setInstitutionName("Updated University");
        request.setStillStudying(true);
        request.setVisible(false);

        com.forkmyfolio.model.User user = new com.forkmyfolio.model.User();
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);

        Qualification updatedQualification = new Qualification();
        QualificationDto updatedQualificationDto = new QualificationDto();
        updatedQualificationDto.setQualificationName("Updated Degree");

        when(qualificationService.updateQualification(
                eq(qualificationId),
                eq(request.getQualificationName()),
                eq(request.getInstitutionName()),
                eq(request.getInstitutionLogoUrl()),
                eq(request.getInstitutionWebsite()),
                eq(request.getFieldOfStudy()),
                eq(request.getLevel()),
                eq(request.getStartYear()),
                eq(request.getCompletionYear()),
                eq(request.getStillStudying()),
                eq(request.getGrade()),
                eq(request.getCredentialUrl()),
                eq(request.getVisible()),
                eq(user)))
                .thenReturn(updatedQualification);

        when(qualificationMapper.toDto(eq(updatedQualification))).thenReturn(updatedQualificationDto);

        mockMvc.perform(put("/api/v1/me/qualifications/{uuid}", qualificationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualificationName").value("Updated Degree"));
    }

    @Test
    @WithMockUser
    void deleteMyQualification_shouldReturnNoContent() throws Exception {
        UUID qualificationId = UUID.randomUUID();
        com.forkmyfolio.model.User user = new com.forkmyfolio.model.User();
        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);

        doNothing().when(qualificationService).deleteQualification(eq(qualificationId), eq(user));

        mockMvc.perform(delete("/api/v1/me/qualifications/{uuid}", qualificationId))
                .andExpect(status().isNoContent());

        verify(qualificationService, times(1)).deleteQualification(eq(qualificationId), eq(user));
    }
}
