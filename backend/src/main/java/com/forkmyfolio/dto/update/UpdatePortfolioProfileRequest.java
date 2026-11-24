package com.forkmyfolio.dto.update;

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

    // Phase 0.31 (TASKS §H1 / KNOWN_ISSUES §14): `visible` and
    // `publiclyVisible` were REMOVED from this DTO on purpose. As primitive
    // booleans they defaulted to false for any absent JSON key, so a partial
    // profile save (e.g. the cover-letter editor, which never sends them)
    // silently set is_public=0 AND visible=0 — a silent unpublish with HTTP
    // 200. Visibility now has exactly two writers: PUT /me/profile/visibility
    // and PUT /me/profile/indexing. Sending these keys here is ignored
    // (Spring Boot does not fail on unknown properties), so legacy clients
    // degrade safely instead of taking the portfolio down.
}