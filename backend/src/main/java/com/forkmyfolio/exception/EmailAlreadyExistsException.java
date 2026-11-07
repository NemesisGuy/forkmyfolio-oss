package com.forkmyfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when an attempt is made to register a user
 * with an email address that already exists in the system.
 * Responds with HTTP status 400 (Bad Request).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new EmailAlreadyExistsException with the specified detail message.
     *
     * @param message the detail message.
     */
    public EmailAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Constructs a new EmailAlreadyExistsException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
