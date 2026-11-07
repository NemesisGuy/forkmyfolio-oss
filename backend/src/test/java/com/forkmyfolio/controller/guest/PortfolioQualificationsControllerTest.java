package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.QualificationDto;
import com.forkmyfolio.mapper.QualificationMapper;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.User;
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
class PortfolioQualificationsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private QualificationMapper qualificationMapper;

    @InjectMocks
    private PortfolioQualificationsController portfolioQualificationsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioQualificationsController).build();
    }

    @Test
    void getPortfolioQualifications_whenUserExists_shouldReturnQualifications() throws Exception {
        // given
        String slug = "test-slug";

        Qualification qualification = new Qualification();
        qualification.setInstitutionName("Test Institution");

        QualificationDto qualificationDto = new QualificationDto();
        qualificationDto.setInstitutionName("Test Institution");

        when(portfolioService.getPublicQualificationsBySlug(slug)).thenReturn(List.of(qualification));
        when(qualificationMapper.toDto(any(Qualification.class))).thenReturn(qualificationDto);

        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{slug}/qualifications", slug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].institutionName").value("Test Institution"));
    }
}