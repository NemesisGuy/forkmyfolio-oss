package com.forkmyfolio.dto.create;

import com.forkmyfolio.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(name = "AdminCreateUserRequest", description = "Request body for an admin to create a new user.")
public class AdminCreateUserRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "Email should be valid.")
    @Schema(description = "The new user's email address.", example = "new.user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
    @Schema(description = "The new user's password.", example = "strongPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "First name is required.")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters.")
    @Schema(description = "The new user's first name.", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters.")
    @Schema(description = "The new user's last name.", example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotEmpty(message = "User must have at least one role.")
    @Schema(description = "The set of roles to assign to the new user.", example = "[\"USER\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<Role> roles;

    @Schema(description = "The user's account status (true for active, false for deactivated). Defaults to true.", example = "true")
    private Boolean active = true;

}