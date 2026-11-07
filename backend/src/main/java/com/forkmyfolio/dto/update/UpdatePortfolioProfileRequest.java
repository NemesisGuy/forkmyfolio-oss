package com.forkmyfolio.dto.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "Request body for creating or updating a user's portfolio profile.")
public class UpdatePortfolioProfileRequest {

    @Schema(description = "A professional headline.", example = "Full-Stack Java Developer")
    @Size(max = 100)
    private String headline;

    @Schema(description = "A summary of the user's professional background.")
    private String summary;

    @Schema(description = "A public-facing email address.", example = "contact.me@example.com")
    @Email
    private String publicEmail;

    @URL
    private String websiteUrl;
    @URL
    private String linkedinUrl;
    @URL
    private String githubUrl;
    @URL
    private String resumeUrl;
    @URL
    private String resumeImageUrl;
    @Size(max = 50)
    private String location;
    private String coverLetterTemplate;
    private boolean visible; // This field does not have the 'is' prefix, so it's fine.

    private boolean publiclyVisible;

    @JsonProperty("isPublic")
    public boolean isPubliclyVisible() {
        return publiclyVisible;
    }

    @JsonProperty("isPublic")
    public void setPubliclyVisible(boolean publiclyVisible) {
        this.publiclyVisible = publiclyVisible;
    }
}