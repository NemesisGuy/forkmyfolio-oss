package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.TestimonialDto;
import com.forkmyfolio.mapper.TestimonialMapper;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios/{slug}/testimonials")
public class PortfolioTestimonialController {

    private final PortfolioService portfolioService;
    private final TestimonialMapper testimonialMapper;

    public PortfolioTestimonialController(PortfolioService portfolioService, TestimonialMapper testimonialMapper) {
        this.portfolioService = portfolioService;
        this.testimonialMapper = testimonialMapper;
    }

    @GetMapping
    public ResponseEntity<List<TestimonialDto>> getPortfolioTestimonials(@PathVariable String slug) {
        List<Testimonial> testimonials = portfolioService.getPublicTestimonialsBySlug(slug);
        List<TestimonialDto> testimonialDtos = testimonials.stream()
                .map(testimonialMapper::toDto)
                .toList();
        return ResponseEntity.ok(testimonialDtos);
    }
}

