package com.dev.education_nearby_server.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

/**
 * Compares two fields on the same bean and registers a violation on the second field when they differ.
 */
public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {

    private String firstField;
    private String secondField;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.firstField = constraintAnnotation.first();
        this.secondField = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object first = wrapper.getPropertyValue(firstField);
        Object second = wrapper.getPropertyValue(secondField);
        boolean matches = Objects.equals(first, second);

        if (!matches) {
            // Redirect violation to the second field so clients can display it next to the confirmation input.
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(secondField)
                    .addConstraintViolation();
        }

        return matches;
    }
}

