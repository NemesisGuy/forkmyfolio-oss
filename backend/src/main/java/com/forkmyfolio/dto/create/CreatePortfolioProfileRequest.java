package com.forkmyfolio.dto.create;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class CreatePortfolioProfileRequest {

    @NotBlank(message = "Headline cannot be blank")
    @Size(max = 100)
    private String headline;

    @NotBlank(message = "Summary cannot be blank")
    @Size(max = 5000)
    private String summary;

    @Email
    @Size(max = 255)
    private String publicEmail;

    @Size(max = 50)
    private String location;

    @URL
    @Size(max = 255)
    private String websiteUrl;

    @URL
    @Size(max = 255)
    private String linkedinUrl;

    @URL
    @Size(max = 255)
    private String githubUrl;

    @URL
    @Size(max = 255)
    private String resumeUrl;

    @URL
    @Size(max = 255)
    private String resumeImageUrl;

    @NotBlank(message = "Cover letter template cannot be blank")
    @Size(max = 10000)
    private String coverLetterTemplate;
}