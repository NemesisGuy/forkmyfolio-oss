package com.forkmyfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to represent a resource state conflict.
 * <p>
 * This exception should be thrown when an operation cannot be completed because
 * it would result in a conflict with the current state of the resource.
 * For example, attempting to create a resource that already exists (e.g., a user profile).
 * <p>
 * The {@link ResponseStatus} annotation ensures that when this exception is thrown
 * from a controller, Spring's default exception handler will automatically respond
 * with an HTTP 409 Conflict status code.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    /**
     * Constructor with a detail message.
     *
     * @param message The detail message explaining the conflict.
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Constructor with a detail message and a cause.
     *
     * @param message The detail message explaining the conflict.
     * @param cause   The underlying cause of the exception.
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}