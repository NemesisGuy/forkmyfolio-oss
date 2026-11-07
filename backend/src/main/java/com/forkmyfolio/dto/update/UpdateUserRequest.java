package com.forkmyfolio.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * DTO for updating the portfolio owner's profile information.
 * This class defines the fields that are permissible to change via the
 * PUT /api/v1/admin/profile endpoint.
 * Note: Email, password, and roles are intentionally excluded for security and simplicity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateUserRequest", description = "Request body for updating the admin user's profile information.")
public class UpdateUserRequest {

    @NotBlank(message = "First name cannot be blank.")
    @Size(max = 50, message = "First name must not exceed 50 characters.")
    @Schema(description = "The user's first name.", example = "Jane", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name cannot be blank.")
    @Size(max = 50, message = "Last name must not exceed 50 characters.")
    @Schema(description = "The user's last name.", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @URL(message = "PortfolioProfile image must be a valid URL.")
    @Size(max = 255, message = "PortfolioProfile image URL must not exceed 255 characters.")
    @Schema(description = "URL of the user's profile image. Can be null or empty.",
            example = "https://example.com/path/to/profile-image.png")
    private String profileImageUrl;

    //active bool


    // You could add other fields here in the future, for example:
    /*
    @Size(max = 5000, message = "Bio must not exceed 5000 characters.")
    @Schema(description = "A short biography or summary for the user's profile.", example = "Full-stack developer with a passion for creating beautiful and functional web applications.")
    private String bio;
    */
}