package com.forkmyfolio.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Optional;

@Data
@Schema(name = "UpdateTestimonialRequest", description = "Request body for updating an existing testimonial. All fields are optional.")
public class UpdateTestimonialRequest {

    private Optional<@Size(max = 1000) String> quote = Optional.empty();
    private Optional<@Size(max = 255) String> authorName = Optional.empty();
    private Optional<@Size(max = 255) String> authorTitle = Optional.empty();
    private Optional<Boolean> visible = Optional.empty();
}