package com.forkmyfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when an error occurs during token refresh operations,
 * such as when a refresh token is expired, invalid, or not found.
 * Responds with HTTP status 401 (Unauthorized) or 403 (Forbidden) depending on the context
 * in the GlobalExceptionHandler. For simplicity here, it's a generic runtime exception.
 * Specific handling can be added in GlobalExceptionHandler.
 */
@ResponseStatus(HttpStatus.FORBIDDEN) // Or HttpStatus.UNAUTHORIZED, depending on desired client behavior
public class TokenRefreshException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new TokenRefreshException with the specified refresh token and detail message.
     * <p>
     * SECURITY (Phase 7.4 §10): the raw refresh token is deliberately NOT included in
     * the message. GlobalExceptionHandler both logs {@code getMessage()} and returns it
     * to the caller in the error payload, so a full token would be echoed back to the
     * client and persisted in log files. Only a short fingerprint is included for
     * correlation/debugging.
     *
     * @param token   The refresh token that caused the error (used for the fingerprint only).
     * @param message The detail message.
     */
    public TokenRefreshException(String token, String message) {
        super(String.format("%s [token: %s]", message, describe(token)));
    }

    /**
     * 8-character prefix fingerprint of a token, or a mask when too short to be
     * safely partialised.
     */
    private static String describe(String token) {
        if (token == null) {
            return "null";
        }
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }

    /**
     * Constructs a new TokenRefreshException with the specified detail message.
     *
     * @param message The detail message.
     */
    public TokenRefreshException(String message) {
        super(message);
    }
}
