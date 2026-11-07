package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.ExperienceDto;
import com.forkmyfolio.mapper.ExperienceMapper;
import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.ExperienceService;
import com.forkmyfolio.service.PortfolioService;
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
class PortfolioExperienceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private ExperienceService experienceService;

    @Mock
    private UserSkillService userSkillService;

    @Mock
    private ExperienceMapper experienceMapper;

    @InjectMocks
    private PortfolioExperienceController portfolioExperienceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioExperienceController).build();
    }

    @Test
    void getPortfolioExperience_whenUserExists_shouldReturnExperience() throws Exception {
        // given
        String slug = "test-slug";
        User user = new User();
        user.setSlug(slug);

        Experience experience = new Experience();
        experience.setVisible(true);
        experience.setCompanyName("Test Company");

        ExperienceDto experienceDto = new ExperienceDto();
        experienceDto.setCompanyName("Test Company");

        when(portfolioService.getPublicPortfolioUserBySlug(slug)).thenReturn(user);
        when(experienceService.getExperiencesForUser(user)).thenReturn(Collections.singletonList(experience));
        when(userSkillService.getUserSkillLookupMap(user)).thenReturn(new HashMap<>());
        when(experienceMapper.toDto(any(Experience.class), any())).thenReturn(experienceDto);

        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{slug}/experience", slug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Test Company"));
    }
}
