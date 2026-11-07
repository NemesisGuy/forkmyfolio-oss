package com.forkmyfolio.dto.create;

import com.forkmyfolio.model.enums.QualificationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "Request body for creating a new qualification.")
public class CreateQualificationRequest {

    @NotBlank(message = "The name of the qualification cannot be blank.")
    @Size(max = 255)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String qualificationName;

    @NotBlank(message = "The institution name cannot be blank.")
    @Size(max = 255)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String institutionName;

    @Size(max = 512)
    @URL(message = "Institution logo URL must be a valid URL")
    private String institutionLogoUrl;

    @Size(max = 255)
    @URL(message = "Institution website must be a valid URL")
    private String institutionWebsite;

    @Size(max = 255)
    private String fieldOfStudy;

    private QualificationLevel level;

    private Integer startYear;

    private Integer completionYear;

    private Boolean stillStudying = false;

    @Size(max = 255)
    private String grade;

    @Size(max = 512)
    @URL(message = "Credential URL must be a valid URL")
    private String credentialUrl;

    private boolean visible = true;
}