package com.forkmyfolio.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A custom validation annotation to ensure that an end date is not before a start date.
 * This annotation must be applied at the class level to a DTO or entity that contains
 * both a start date and an end date field.
 * <p>
 * = *
 * Example usage:
 * <pre>
 * {@code
 * @ValidDateRange(startDate = "startDate", endDate = "endDate", message = "End date must be after start date")
 * public class MyDto {
 *     private LocalDate startDate;
 *     private LocalDate endDate;
 *     // ... getters and setters
 * }
 * }
 * </pre>
 */
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

    /**
     * The default error message to be used if the validation fails.
     *
     * @return The error message.
     */
    String message() default "End date must be after or equal to start date";

    /**
     * The name of the field representing the start date.
     *
     * @return The start date field name.
     */
    String startDate();

    /**
     * The name of the field representing the end date.
     *
     * @return The end date field name.
     */
    String endDate();

    /**
     * Standard boilerplate for validation annotations.
     * Allows specifying validation groups.
     *
     * @return The validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Standard boilerplate for validation annotations.
     * Allows attaching custom payload information to a constraint.
     *
     * @return The payload.
     */
    Class<? extends Payload>[] payload() default {};
}