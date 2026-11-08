package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.PortfolioDto;
import com.forkmyfolio.model.User;
import com.forkmyfolio.mapper.PortfolioMapper;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.UserSkillService;
import com.forkmyfolio.service.impl.VisitorStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private UserSkillService userSkillService;

    @Mock
    private VisitorStatsService visitorStatsService;

    @Mock
    private CacheManager cacheManager;

    private PortfolioController portfolioController;

    @BeforeEach
    void setUp() {
        // Real ObjectMapper: the controller serializes its own MISS response.
        portfolioController = new PortfolioController(portfolioService, portfolioMapper,
                userSkillService, visitorStatsService, cacheManager, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioController).build();
    }

    @Test
    void getPortfolioBySlug_shouldReturnOk_withDefaultBypassCacheHeader() throws Exception {
        // Plain unit tests have no CacheManager-backed cache available
        // (getCache → null), so the honest default is BYPASS.
        User testUser = new User();
        testUser.setSlug("test-slug");
        testUser.setProjects(Collections.emptySet());
        testUser.setExperiences(Collections.emptySet());
        testUser.setUserSkills(Collections.emptySet());

        PortfolioDto portfolioDto = new PortfolioDto();

        when(portfolioService.getPublicPortfolioUserBySlug("test-slug")).thenReturn(testUser);
        when(userSkillService.getUserSkillLookupMap(testUser)).thenReturn(Collections.emptyMap());
        when(portfolioMapper.toDto(eq(testUser), any())).thenReturn(portfolioDto);

        mockMvc.perform(get("/api/v1/portfolios/{slug}", "test-slug"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "BYPASS"))
                .andExpect(jsonPath("$.status").value("success"));
    }
}
