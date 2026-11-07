package com.forkmyfolio.service;

import com.forkmyfolio.exception.TokenRefreshException;
import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;

import java.util.Optional;

/**
 * Service interface for managing refresh tokens.
 * Handles creation, validation, and rotation of refresh tokens.
 */
public interface RefreshTokenService {

    /**
     * Finds a refresh token by its token string and eagerly fetches the associated user and roles.
     *
     * @param token The token string.
     * @return An Optional containing the {@link RefreshToken} with a fully initialized User if found.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Creates and persists a new refresh token for the given user.
     * This enforces a one-active-token-per-user policy by deleting any pre-existing token.
     *
     * @param user The user for whom the refresh token is to be created.
     * @return The created {@link RefreshToken}.
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Verifies that the given refresh token is not expired.
     * Throws an exception if the token is invalid.
     *
     * @param token The refresh token to verify.
     * @return The verified {@link RefreshToken}.
     * @throws TokenRefreshException if the token is expired.
     */
    RefreshToken verifyExpiration(RefreshToken token) throws TokenRefreshException;

    /**
     * Deletes all refresh tokens associated with a specific user ID.
     *
     * @param userId The ID of the user whose refresh tokens are to be deleted.
     * @return The number of tokens deleted.
     */
    int deleteByUserId(Long userId);

    /**
     * Deletes a specific refresh token by its string value.
     *
     * @param token The refresh token string.
     */
    void deleteByToken(String token);

    /**
     * Atomically rotates a refresh token. It invalidates the old token and issues a new one
     * in a single transaction to prevent race conditions where a token could be used multiple times.
     *
     * @param oldToken The refresh token that was just used and needs to be invalidated.
     * @return The newly created and persisted RefreshToken.
     */
    RefreshToken rotateRefreshToken(RefreshToken oldToken);


}