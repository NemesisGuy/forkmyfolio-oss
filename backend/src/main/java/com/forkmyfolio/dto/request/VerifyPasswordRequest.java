package com.forkmyfolio.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request used to re-authenticate the current user before sensitive operations
 * such as exporting or restoring a backup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password re-verification request.")
public class VerifyPasswordRequest {

    @NotNull(message = "Password is required.")
    @Schema(description = "The current user's plaintext password. Never stored or logged.", example = "S3cur3P@ss!")
    private String password;
}
