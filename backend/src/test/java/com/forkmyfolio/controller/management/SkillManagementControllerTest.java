package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.create.CreateSkillRequest;
import com.forkmyfolio.dto.response.SkillDto;
import com.forkmyfolio.dto.update.UpdateUserSkillRequest;
import com.forkmyfolio.mapper.SkillMapper;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.UserSkillService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SkillManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserSkillService userSkillService;
    @Mock
    private UserService userService;
    @Mock
    private SkillMapper skillMapper;

    @InjectMocks
    private SkillManagementController skillManagementController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(skillManagementController).build();
        objectMapper = new ObjectMapper();
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    @WithMockUser
    void getMySkills_shouldReturnSkills() throws Exception {
        UserSkill userSkill = new UserSkill();
        List<UserSkill> userSkills = Collections.singletonList(userSkill);
        SkillDto skillDto = new SkillDto();
        skillDto.setName("Java");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(userSkillService.getAllSkillsForUser(eq(testUser))).thenReturn(userSkills);
        when(skillMapper.toDetailDto(any(UserSkill.class))).thenReturn(skillDto);

        mockMvc.perform(get("/api/v1/me/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    @WithMockUser
    void addSkillToMyPortfolio_shouldCreateAndReturnSkill() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");
        request.setLevel(SkillLevel.ADVANCED);
        request.setVisible(true);

        Skill skillFromMapper = new Skill();
        UserSkill userSkillFromMapper = new UserSkill();
        UserSkill savedUserSkill = new UserSkill();
        SkillDto finalDto = new SkillDto();
        finalDto.setName("Java");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(skillMapper.toSkillEntity(any(CreateSkillRequest.class))).thenReturn(skillFromMapper);
        when(skillMapper.toUserSkillEntity(any(CreateSkillRequest.class))).thenReturn(userSkillFromMapper);
        when(userSkillService.addSkillToUser(eq(testUser), eq(skillFromMapper), eq(userSkillFromMapper)))
                .thenReturn(savedUserSkill);
        when(skillMapper.toDetailDto(eq(savedUserSkill))).thenReturn(finalDto);

        mockMvc.perform(post("/api/v1/me/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Java"));
    }

    @Test
    @WithMockUser
    void updateMySkill_shouldUpdateAndReturnSkill() throws Exception {
        UUID userSkillId = UUID.randomUUID();
        UpdateUserSkillRequest request = new UpdateUserSkillRequest();
        request.setLevel(SkillLevel.EXPERT);
        request.setVisible(true);

        UserSkill userSkillUpdatesFromMapper = new UserSkill();
        UserSkill updatedUserSkill = new UserSkill();
        SkillDto updatedSkillDto = new SkillDto();
        updatedSkillDto.setLevel(SkillLevel.EXPERT);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(skillMapper.toUserSkillEntity(any(UpdateUserSkillRequest.class))).thenReturn(userSkillUpdatesFromMapper);
        when(userSkillService.updateSkillForUser(eq(userSkillId), eq(userSkillUpdatesFromMapper), eq(testUser)))
                .thenReturn(updatedUserSkill);
        when(skillMapper.toDetailDto(eq(updatedUserSkill))).thenReturn(updatedSkillDto);

        mockMvc.perform(put("/api/v1/me/skills/{uuid}", userSkillId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("EXPERT"));
    }

    @Test
    @WithMockUser
    void removeSkillFromMyPortfolio_shouldReturnNoContent() throws Exception {
        UUID userSkillId = UUID.randomUUID();

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        doNothing().when(userSkillService).removeSkillFromUser(eq(userSkillId), eq(testUser));

        mockMvc.perform(delete("/api/v1/me/skills/{uuid}", userSkillId))
                .andExpect(status().isNoContent());

        verify(userSkillService, times(1)).removeSkillFromUser(eq(userSkillId), eq(testUser));
    }
}
