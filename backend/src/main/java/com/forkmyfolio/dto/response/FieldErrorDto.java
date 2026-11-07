package com.forkmyfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object representing a single field error, typically used for validation errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorDto {
    /**
     * The name of the field that caused the error.
     */
    private String field;

    /**
     * The error message associated with the field.
     */
    private String message;
}
