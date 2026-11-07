package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.create.CreateTestimonialRequest;
import com.forkmyfolio.dto.response.TestimonialDto;
import com.forkmyfolio.dto.update.UpdateTestimonialRequest;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import org.springframework.stereotype.Component;

/**
 * Maps between Testimonial domain objects and their related DTOs.
 */
@Component
public class TestimonialMapper {

    /**
     * Converts a Testimonial entity to a TestimonialDto for API responses.
     */
    public TestimonialDto toDto(Testimonial testimonial) {
        if (testimonial == null) {
            return null;
        }
        TestimonialDto dto = new TestimonialDto();
        dto.setUuid(testimonial.getUuid());
        dto.setQuote(testimonial.getQuote());
        dto.setAuthorName(testimonial.getAuthorName());
        dto.setAuthorTitle(testimonial.getAuthorTitle());
        dto.setVisible(testimonial.isVisible());
        return dto;
    }

    /**
     * Converts a CreateTestimonialRequest DTO into a new Testimonial entity.
     */
    public Testimonial toEntity(CreateTestimonialRequest request, User owner) {
        if (request == null) {
            return null;
        }
        Testimonial testimonial = new Testimonial();
        testimonial.setQuote(request.getQuote());
        testimonial.setAuthorName(request.getAuthorName());
        testimonial.setAuthorTitle(request.getAuthorTitle());
        testimonial.setVisible(request.isVisible());
        testimonial.setUser(owner);
        return testimonial;
    }

    /**
     * Applies updates from an UpdateTestimonialRequest to an existing Testimonial entity.
     */
    public void applyUpdateFromRequest(UpdateTestimonialRequest request, Testimonial testimonial) {
        if (request == null || testimonial == null) {
            return;
        }
        request.getQuote().ifPresent(testimonial::setQuote);
        request.getAuthorName().ifPresent(testimonial::setAuthorName);
        request.getAuthorTitle().ifPresent(testimonial::setAuthorTitle);
        request.getVisible().ifPresent(testimonial::setVisible);
    }

    /**
     * Converts a TestimonialDto from a backup file into a new Testimonial entity.
     */
    public Testimonial toEntityFromDto(TestimonialDto dto, User owner) {
        if (dto == null) {
            return null;
        }
        Testimonial testimonial = new Testimonial();
        testimonial.setQuote(dto.getQuote());
        testimonial.setAuthorName(dto.getAuthorName());
        testimonial.setAuthorTitle(dto.getAuthorTitle());
        testimonial.setVisible(dto.isVisible());
        testimonial.setUser(owner);
        return testimonial;
    }
}