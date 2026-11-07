package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.ProjectDto;
import com.forkmyfolio.mapper.ProjectMapper;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.ProjectService;
import com.forkmyfolio.service.UserSkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PortfolioProjectsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserSkillService userSkillService;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private PortfolioProjectsController portfolioProjectsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioProjectsController).build();
    }

    @Test
    void getProjectsBySlug_whenUserExists_shouldReturnProjects() throws Exception {
        // given
        String slug = "test-slug";
        User user = new User();
        user.setSlug(slug);

        Project project = new Project();
        project.setVisible(true);
        project.setTitle("Test Project");

        ProjectDto projectDto = new ProjectDto();
        projectDto.setTitle("Test Project");

        when(portfolioService.getPublicPortfolioUserBySlug(slug)).thenReturn(user);
        when(projectService.getProjectsForUser(user)).thenReturn(Collections.singletonList(project));
        when(userSkillService.getUserSkillLookupMap(user)).thenReturn(new HashMap<>());
        when(projectMapper.toDto(any(Project.class), any())).thenReturn(projectDto);


        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{slug}/projects", slug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Project"));
    }
}
