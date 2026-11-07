package com.forkmyfolio.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to indicate that a controller method's response
 * should not be wrapped by the {@link com.forkmyfolio.exception.GlobalExceptionHandler}
 * <p>
 * This is typically used for methods that return raw data, such as file downloads.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipApiResponseWrapper {
}