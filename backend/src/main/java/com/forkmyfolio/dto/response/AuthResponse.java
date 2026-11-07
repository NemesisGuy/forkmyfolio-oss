package com.forkmyfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for authentication responses.
 * Contains the JWT access token and basic user information upon successful login or registration.
 * Returned after login/register. Contains the token and user DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * The JWT access token generated for the authenticated user.
     */
    private String accessToken;

    /**
     * The type of the token (e.g., "Bearer").
     */
    private String tokenType = "Bearer";

    /**
     * Basic information about the authenticated user.
     * See {@link UserDto} for details.
     */
    private UserDto user;

    /**
     * Constructor for creating an AuthResponse with only an access token and user DTO.
     * The tokenType defaults to "Bearer".
     *
     * @param accessToken The JWT access token.
     * @param user        The DTO representing the authenticated user.
     */
    public AuthResponse(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}
