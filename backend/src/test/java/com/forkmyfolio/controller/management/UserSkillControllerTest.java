package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.create.CreateSkillRequest;
import com.forkmyfolio.dto.response.UserSkillDto;
import com.forkmyfolio.dto.update.UpdateUserSkillRequest;
import com.forkmyfolio.mapper.SkillMapper;
import com.forkmyfolio.mapper.UserSkillMapper;
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
class UserSkillControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserSkillService userSkillService;
    @Mock
    private UserService userService;
    @Mock
    private UserSkillMapper userSkillMapper;
    @Mock
    private SkillMapper skillMapper;

    @InjectMocks
    private UserSkillController userSkillController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userSkillController).build();
        objectMapper = new ObjectMapper();
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    @WithMockUser
    void getAllUserSkills_shouldReturnSkills() throws Exception {
        UserSkill userSkill = new UserSkill();
        userSkill.setId(1L);
        UUID userSkillUuid = UUID.randomUUID();
        userSkill.setUuid(userSkillUuid);
        List<UserSkill> userSkills = Collections.singletonList(userSkill);

        UserSkillDto userSkillDto = new UserSkillDto();
        userSkillDto.setUserSkillId(userSkillUuid);
        List<UserSkillDto> userSkillDtos = Collections.singletonList(userSkillDto);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(userSkillService.getAllSkillsForUser(eq(testUser))).thenReturn(userSkills);
        when(userSkillMapper.toDtoList(eq(userSkills))).thenReturn(userSkillDtos);

        mockMvc.perform(get("/api/v1/user/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userSkillId").value(userSkillUuid.toString()));
    }

    @Test
    @WithMockUser
    void createUserSkill_shouldCreateAndReturnSkill() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");
        request.setLevel(SkillLevel.ADVANCED);
        request.setVisible(true);

        Skill skillFromMapper = new Skill();
        UserSkill userSkillFromMapper = new UserSkill();

        UserSkill savedUserSkill = new UserSkill();
        savedUserSkill.setId(2L);
        UUID savedUserSkillUuid = UUID.randomUUID();
        savedUserSkill.setUuid(savedUserSkillUuid);

        UserSkillDto finalDto = new UserSkillDto();
        finalDto.setUserSkillId(savedUserSkillUuid);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(skillMapper.toSkillEntity(any(CreateSkillRequest.class))).thenReturn(skillFromMapper);
        when(skillMapper.toUserSkillEntity(any(CreateSkillRequest.class))).thenReturn(userSkillFromMapper);
        when(userSkillService.addSkillToUser(eq(testUser), eq(skillFromMapper), eq(userSkillFromMapper)))
                .thenReturn(savedUserSkill);
        when(userSkillMapper.toDto(eq(savedUserSkill))).thenReturn(finalDto);

        mockMvc.perform(post("/api/v1/user/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userSkillId").value(savedUserSkillUuid.toString()));
    }

    @Test
    @WithMockUser
    void updateUserSkill_shouldUpdateAndReturnSkill() throws Exception {
        UUID userSkillId = UUID.randomUUID();
        UpdateUserSkillRequest request = new UpdateUserSkillRequest();
        request.setLevel(SkillLevel.EXPERT);
        request.setVisible(true);

        UserSkill userSkillUpdatesFromMapper = new UserSkill();

        UserSkill updatedUserSkill = new UserSkill();
        updatedUserSkill.setId(3L);
        updatedUserSkill.setUuid(userSkillId);

        UserSkillDto finalDto = new UserSkillDto();
        finalDto.setUserSkillId(updatedUserSkill.getUuid());

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(skillMapper.toUserSkillEntity(any(UpdateUserSkillRequest.class))).thenReturn(userSkillUpdatesFromMapper);
        when(userSkillService.updateSkillForUser(eq(userSkillId), eq(userSkillUpdatesFromMapper), eq(testUser)))
                .thenReturn(updatedUserSkill);
        when(userSkillMapper.toDto(eq(updatedUserSkill))).thenReturn(finalDto);

        mockMvc.perform(put("/api/v1/user/skills/{userSkillId}", userSkillId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userSkillId").value(userSkillId.toString()));
    }

    @Test
    @WithMockUser
    void deleteUserSkill_shouldReturnNoContent() throws Exception {
        UUID userSkillId = UUID.randomUUID();

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        doNothing().when(userSkillService).removeSkillFromUser(eq(userSkillId), eq(testUser));

        mockMvc.perform(delete("/api/v1/user/skills/{userSkillId}", userSkillId))
                .andExpect(status().isNoContent());

        verify(userSkillService, times(1)).removeSkillFromUser(eq(userSkillId), eq(testUser));
    }
}
