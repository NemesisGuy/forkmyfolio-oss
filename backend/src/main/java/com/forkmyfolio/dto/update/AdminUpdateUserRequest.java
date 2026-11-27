package com.forkmyfolio.dto.update;

import com.forkmyfolio.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(name = "AdminUpdateUserRequest", description = "Request body for an admin to update a user's details.")
public class AdminUpdateUserRequest {

    @NotBlank(message = "First name cannot be blank.")
    @Size(max = 50, message = "First name must not exceed 50 characters.")
    @Schema(description = "The user's first name.", example = "Jane", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name cannot be blank.")
    @Size(max = 50, message = "Last name must not exceed 50 characters.")
    @Schema(description = "The user's last name.", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "Slug cannot be blank.")
    @Size(max = 50, message = "Slug must not exceed 50 characters.")
    @Schema(description = "The user's unique URL slug.", example = "jane-doe-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String slug;

    /**
     * Optional: the admin edit form already exposes an Email field, but older
     * backend versions silently ignored it. A non-blank value replaces the
     * login email; null/blank leaves the current address untouched. The service
     * rejects an address owned by a different account.
     */
    @Email(message = "Email must be a valid email address.")
    @Size(max = 255, message = "Email must not exceed 255 characters.")
    @Schema(description = "Optional new login email; omit or leave blank to keep the current address.",
            example = "jane@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String email;

    @NotNull(message = "Active status is required.")
    @Schema(description = "The user's account status (true for active, false for deactivated).", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean active;

    @NotEmpty(message = "User must have at least one role.")
    @Schema(description = "The complete set of roles for the user.", example = "[\"USER\", \"ADMIN\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<Role> roles;
}