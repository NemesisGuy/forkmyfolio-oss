package com.forkmyfolio.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Administrator request to reset another user's password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin password reset request.")
public class AdminResetPasswordRequest {

    @NotBlank(message = "New password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters.")
    @Schema(description = "The new raw password. Encoded before storage; never logged.")
    private String newPassword;
}
