package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.PortfolioDto;
import com.forkmyfolio.mapper.PortfolioMapper;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.UserSkillService;
import com.forkmyfolio.service.impl.VisitorStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 0.20 — unit tests for the READ-THROUGH cache decision logic in
 * {@link PortfolioController#getPortfolioBySlug}. The real Redis round-trip,
 * eviction and header behaviour are covered separately in
 * {@code PublicPortfolioCacheIntegrationTest}; here we pin the local decisions:
 * HIT serves the cached response JSON without touching the assembly pipeline,
 * MISS assembles, serializes and populates, a missing cache manager entry
 * means BYPASS, and Redis read/write failures degrade to assembly instead of
 * failing the view.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioControllerCacheTest {

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
    @Mock
    private Cache cache;
    @Mock
    private Cache.ValueWrapper wrapper;

    private PortfolioController portfolioController;

    private MockMvc mockMvc;

    private User user;
    private PortfolioDto dto;

    @BeforeEach
    void setUp() {
        // Real ObjectMapper (the controller serializes the wrapper on MISS);
        // everything else is the mock set above.
        portfolioController = new PortfolioController(portfolioService, portfolioMapper,
                userSkillService, visitorStatsService, cacheManager, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioController).build();
        dto = new PortfolioDto();
    }

    /** A user whose lazy collections are safe to iterate for plain tests. */
    private User initializedPublicUser(String slug) {
        User u = new User();
        u.setId(1L);
        u.setSlug(slug);
        u.setProjects(Collections.emptySet());
        u.setExperiences(Collections.emptySet());
        u.setUserSkills(Collections.emptySet());
        PortfolioProfile profile = new PortfolioProfile();
        profile.setPubliclyVisible(true);
        u.setPortfolioProfile(profile);
        return u;
    }

    @Test
    void hit_servesCachedBytes_withoutCallingTheAssemblyPipelineOrJackson() throws Exception {
        when(cacheManager.getCache("publicPortfolios")).thenReturn(cache);
        when(cache.get("unit-slug")).thenReturn(wrapper);
        // Phase 0.23: the cache unit is the pre-serialized response JSON.
        when(wrapper.get()).thenReturn("{\"status\":\"success\",\"data\":{\"slug\":\"unit-slug\"},\"errors\":[]}");

        mockMvc.perform(get("/api/v1/portfolios/{slug}", "unit-slug"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.slug").value("unit-slug"));

        // The whole point: on a HIT neither MySQL/Hibernate (services), the
        // mapper, nor any re-serialization cost may appear — the response is
        // served straight from the cached bytes.
        verify(portfolioService, never()).getPublicPortfolioUserBySlug(anyString());
        verify(userSkillService, never()).getUserSkillLookupMap(any());
        verify(portfolioMapper, never()).toDto(any(), any());
    }

    @Test
    void miss_assembles_thenStoresTheSerializedJson_headerSaysMiss() throws Exception {
        when(cacheManager.getCache("publicPortfolios")).thenReturn(cache);
        when(cache.get("unit-slug")).thenReturn(null);
        user = initializedPublicUser("unit-slug");
        when(portfolioService.getPublicPortfolioUserBySlug("unit-slug")).thenReturn(user);
        when(userSkillService.getUserSkillLookupMap(user)).thenReturn(Collections.emptyMap());
        when(portfolioMapper.toDto(eq(user), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/portfolios/{slug}", "unit-slug"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.status").value("success"));

        // Phase 0.23: the stored unit is the serialized JSON STRING, not the
        // DTO object, so a later HIT serves bytes with no Jackson work.
        ArgumentCaptor<String> cached = ArgumentCaptor.forClass(String.class);
        verify(cache).put(eq("unit-slug"), cached.capture());
        org.junit.jupiter.api.Assertions.assertTrue(cached.getValue().contains("\"status\":\"success\""),
                "the cached unit must be the serialized response JSON");
    }

    @Test
    void bypass_whenNoCacheIsAvailable_assemblesWithoutCaching() throws Exception {
        when(cacheManager.getCache("publicPortfolios")).thenReturn(null);
        user = initializedPublicUser("unit-slug");
        when(portfolioService.getPublicPortfolioUserBySlug("unit-slug")).thenReturn(user);
        when(userSkillService.getUserSkillLookupMap(user)).thenReturn(Collections.emptyMap());
        when(portfolioMapper.toDto(eq(user), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/portfolios/{slug}", "unit-slug"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "BYPASS"));

        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void cacheReadFailure_degradesToAssembly_neverFailsTheView() throws Exception {
        when(cacheManager.getCache("publicPortfolios")).thenReturn(cache);
        when(cache.get("unit-slug")).thenThrow(new IllegalStateException("Redis gone"));
        user = initializedPublicUser("unit-slug");
        when(portfolioService.getPublicPortfolioUserBySlug("unit-slug")).thenReturn(user);
        when(userSkillService.getUserSkillLookupMap(user)).thenReturn(Collections.emptyMap());
        when(portfolioMapper.toDto(eq(user), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/portfolios/{slug}", "unit-slug"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.status").value("success"));

        verify(portfolioMapper).toDto(eq(user), any());
    }

    @Test
    void cacheWriteFailure_stillServesTheAssembledResponse() throws Exception {
        when(cacheManager.getCache("publicPortfolios")).thenReturn(cache);
        when(cache.get("unit-slug")).thenReturn(null);
        doThrow(new IllegalStateException("Redis write refused")).when(cache).put(anyString(), any());
        user = initializedPublicUser("unit-slug");
        when(portfolioService.getPublicPortfolioUserBySlug("unit-slug")).thenReturn(user);
        when(userSkillService.getUserSkillLookupMap(user)).thenReturn(Collections.emptyMap());
        when(portfolioMapper.toDto(eq(user), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/portfolios/{slug}", "unit-slug"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.status").value("success"));
    }
}
