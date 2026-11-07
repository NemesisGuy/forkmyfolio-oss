package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for representing User information in API responses.
 * Excludes sensitive information like the password.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    /**
     * Unique identifier for the user.
     */
    private UUID id;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

    /**
     * User's slug.
     */
    private String slug;


    /**
     * URL to the user's profile image.
     */
    private String profileImageUrl;

    /**
     * The authentication provider used to register this user.
     */
    private AuthProvider provider;

    /**
     * The unique identifier from the external provider.
     * This will be null for users with LOCAL provider.
     */
    private String providerId;

    /**
     * Roles assigned to the user (e.g., ADMIN, USER).
     */
    private Set<Role> roles;

    /**
     * Indicates whether the user account is active.
     */
    private boolean active;

    private Instant passwordLastChangedAt;
    private Instant termsAcceptedAt;
    private String termsVersion;

    /**
     * Timestamp of when the user account was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp of when the user account was last updated.
     */
    private LocalDateTime updatedAt;
}