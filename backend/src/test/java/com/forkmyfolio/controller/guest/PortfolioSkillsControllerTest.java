package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.UserSkillDto;
import com.forkmyfolio.mapper.UserSkillMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.model.enums.SkillLevel;
import com.forkmyfolio.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PortfolioSkillsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private UserSkillMapper userSkillMapper;

    @InjectMocks
    private PortfolioSkillsController portfolioSkillsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioSkillsController).build();
    }

    @Test
    void getSkillsBySlug_whenUserExists_shouldReturnSkills() throws Exception {
        // given
        String slug = "test-slug";

        UserSkill userSkill = new UserSkill();
        userSkill.setVisible(true);

        UserSkillDto userSkillDto = new UserSkillDto();
        userSkillDto.setLevel(SkillLevel.ADVANCED);

        when(portfolioService.getVisibleUserSkillsBySlug(slug)).thenReturn(List.of(userSkill));
        when(userSkillMapper.toDto(any(UserSkill.class))).thenReturn(userSkillDto);

        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{slug}/skills", slug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("ADVANCED"));
    }
}
