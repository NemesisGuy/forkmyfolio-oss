package com.forkmyfolio.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDate;

/**
 * Implements the validation logic for the {@link ValidDateRange} annotation.
 * This validator checks if a given end date is not before a start date within an object.
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startDateFieldName;
    private String endDateFieldName;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startDateFieldName = constraintAnnotation.startDate();
        this.endDateFieldName = constraintAnnotation.endDate();
    }

    /**
     * Validates the date range of the given object.
     *
     * @param value   The object to validate.
     * @param context The context in which the constraint is evaluated.
     * @return {@code true} if the end date is not before the start date, or if either date is null; {@code false} otherwise.
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let other validators like @NotNull handle null objects.
        }

        // Using Spring's BeanWrapper to dynamically access properties by name.
        Object startDateValue = new BeanWrapperImpl(value).getPropertyValue(startDateFieldName);
        Object endDateValue = new BeanWrapperImpl(value).getPropertyValue(endDateFieldName);

        // If the end date is null (e.g., for a current job), the range is considered valid.
        // If the start date is null, @NotNull on the field itself should catch it.
        if (startDateValue == null || endDateValue == null) {
            return true;
        }

        if (!(startDateValue instanceof LocalDate startDate) || !(endDateValue instanceof LocalDate endDate)) {
            // This is a developer error; the annotation is used on the wrong type.
            throw new IllegalArgumentException("Fields for @ValidDateRange must be of type java.time.LocalDate.");
        }

        // The validation logic: endDate must not be before startDate.
        return !endDate.isBefore(startDate);
    }
}