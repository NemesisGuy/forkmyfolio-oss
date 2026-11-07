package com.forkmyfolio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a refresh token stored in the database.
 * Each refresh token is associated with a user and has an expiry date.
 * Used to obtain new JWT access tokens without requiring the user to
 * re-authenticate.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RefreshToken {

    /**
     * Unique identifier for the refresh token.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user to whom this refresh token belongs.
     * Eagerly fetched as user information is often needed when validating the
     * token.
     * If a user is deleted, their refresh tokens should also be removed (cascade
     * can be configured).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Changed to LAZY as per best practice, fetch when needed
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    /**
     * The actual refresh token string. This should be a cryptographically secure
     * random string.
     * Marked as unique to prevent token collisions, though highly unlikely with
     * good generation.
     * The token itself is not hashed in the database in this design for simplicity
     * of lookup,
     * relying on its randomness and HttpOnly cookie + HTTPS for security.
     */
    @Column(nullable = false, unique = true, length = 512) // Increased length for secure random strings
    @NotNull
    private String token;

    /**
     * The expiry date and time of this refresh token.
     * After this time, the token is no longer valid.
     */
    @Column(nullable = false)
    @NotNull
    private Instant expiryDate;

    /**
     * Constructs a new RefreshToken.
     *
     * @param user       The user associated with this token.
     * @param token      The token string.
     * @param expiryDate The expiry date of the token.
     */
    public RefreshToken(User user, String token, Instant expiryDate) {
        this.user = user;
        this.token = token;
        this.expiryDate = expiryDate;
    }
}
