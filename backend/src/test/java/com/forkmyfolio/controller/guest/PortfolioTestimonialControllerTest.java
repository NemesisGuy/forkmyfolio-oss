package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.TestimonialDto;
import com.forkmyfolio.mapper.TestimonialMapper;
import com.forkmyfolio.model.Testimonial;
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
class PortfolioTestimonialControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private TestimonialMapper testimonialMapper;

    @InjectMocks
    private PortfolioTestimonialController portfolioTestimonialController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioTestimonialController).build();
    }

    @Test
    void getPortfolioTestimonials_whenUserExists_shouldReturnTestimonials() throws Exception {
        // given
        String slug = "test-slug";

        Testimonial testimonial = new Testimonial();
        testimonial.setAuthorName("Test Giver");

        TestimonialDto testimonialDto = new TestimonialDto();
        testimonialDto.setAuthorName("Test Giver");

        when(portfolioService.getPublicTestimonialsBySlug(slug)).thenReturn(List.of(testimonial));
        when(testimonialMapper.toDto(any(Testimonial.class))).thenReturn(testimonialDto);

        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{slug}/testimonials", slug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorName").value("Test Giver"));
    }
}
