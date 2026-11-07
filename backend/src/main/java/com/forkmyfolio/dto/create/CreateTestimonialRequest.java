package com.forkmyfolio.dto.create;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateTestimonialRequest", description = "Request body for creating a new testimonial.")
public class CreateTestimonialRequest {

    @NotBlank(message = "Quote cannot be blank.")
    @Size(max = 1000)
    private String quote;

    @NotBlank(message = "Author name cannot be blank.")
    @Size(max = 255)
    private String authorName;

    @Size(max = 255)
    private String authorTitle;

    @NotNull(message = "Visibility must be specified.")
    @Schema(description = "Whether the testimonial is visible on the public portfolio.", defaultValue = "true")
    private boolean visible;
}