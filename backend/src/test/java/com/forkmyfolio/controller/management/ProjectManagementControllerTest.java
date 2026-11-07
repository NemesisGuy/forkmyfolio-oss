package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.create.CreateProjectRequest;
import com.forkmyfolio.dto.response.ProjectDto;
import com.forkmyfolio.dto.update.UpdateProjectRequest;
import com.forkmyfolio.mapper.ProjectMapper;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.ProjectService;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProjectService projectService;
    @Mock
    private UserService userService;
    @Mock
    private UserSkillService userSkillService;
    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectManagementController projectManagementController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectManagementController).build();
        objectMapper = new ObjectMapper();
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    @WithMockUser
    void getMyProjects_shouldReturnProjects() throws Exception {
        Project project = new Project();
        List<Project> projects = Collections.singletonList(project);
        ProjectDto projectDto = new ProjectDto();
        projectDto.setTitle("Test Project");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(projectService.getProjectsForUser(eq(testUser))).thenReturn(projects);
        when(userSkillService.getUserSkillLookupMap(eq(testUser))).thenReturn(Collections.emptyMap());
        when(projectMapper.toDto(eq(project), any())).thenReturn(projectDto);

        mockMvc.perform(get("/api/v1/me/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Test Project"));
    }

    @Test
    @WithMockUser
    void createMyProject_shouldCreateAndReturnProject() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("New Project");
        request.setDescription("This is a test project description with enough length.");
        request.setVisible(true);
        request.setDisplayOrder(0);
        request.setSkills(Collections.emptySet());

        Project newProjectDetails = new Project();
        Project createdProject = new Project();
        ProjectDto createdProjectDto = new ProjectDto();
        createdProjectDto.setTitle("New Project");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(projectMapper.toEntity(any(CreateProjectRequest.class))).thenReturn(newProjectDetails);
        when(projectService.createProject(eq(newProjectDetails), any(), eq(testUser), any())).thenReturn(createdProject);
        when(userSkillService.getUserSkillLookupMap(eq(testUser))).thenReturn(Collections.emptyMap());
        when(projectMapper.toDto(eq(createdProject), any())).thenReturn(createdProjectDto);

        mockMvc.perform(post("/api/v1/me/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("New Project"));
    }

    @Test
    @WithMockUser
    void updateMyProject_shouldUpdateAndReturnProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setTitle("Updated Project");
        request.setDescription("This is an updated test project description with enough length.");
        request.setVisible(true);
        request.setDisplayOrder(0);
        request.setSkills(Collections.emptySet());

        Project updatedProjectData = new Project();
        Project updatedProject = new Project();
        ProjectDto updatedProjectDto = new ProjectDto();
        updatedProjectDto.setTitle("Updated Project");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(projectMapper.toEntity(any(UpdateProjectRequest.class))).thenReturn(updatedProjectData);
        when(projectService.updateProject(eq(projectId), eq(updatedProjectData), any(), any(), eq(testUser)))
                .thenReturn(updatedProject);
        when(userSkillService.getUserSkillLookupMap(eq(testUser))).thenReturn(Collections.emptyMap());
        when(projectMapper.toDto(eq(updatedProject), any())).thenReturn(updatedProjectDto);

        mockMvc.perform(put("/api/v1/me/projects/{uuid}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Project"));
    }

    @Test
    @WithMockUser
    void deleteMyProject_shouldReturnNoContent() throws Exception {
        UUID projectId = UUID.randomUUID();

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        doNothing().when(projectService).deleteProject(eq(projectId), eq(testUser));

        mockMvc.perform(delete("/api/v1/me/projects/{uuid}", projectId))
                .andExpect(status().isNoContent());

        verify(projectService, times(1)).deleteProject(eq(projectId), eq(testUser));
    }
}
