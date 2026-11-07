package com.forkmyfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when an attempt is made to create a resource
 * that would violate a uniqueness constraint (e.g., creating a user with an
 * email that already exists, or adding a skill a user already has).
 * <p>
 * The {@link ResponseStatus} annotation allows Spring to automatically translate
 * this exception into a 409 Conflict HTTP status code, which is the appropriate
 * response for this type of error. This simplifies global exception handling.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}