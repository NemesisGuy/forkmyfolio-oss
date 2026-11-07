package com.forkmyfolio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for user login requests.
 * Contains the credentials required for authentication.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * The email address of the user attempting to log in.
     * Must not be blank and must be a valid email format.
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be a valid format")
    private String email;

    /**
     * The password of the user attempting to log in.
     * Must not be blank.
     */
    @NotBlank(message = "Password cannot be blank")
    private String password;
}
