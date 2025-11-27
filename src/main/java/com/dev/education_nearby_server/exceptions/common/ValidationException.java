package com.dev.education_nearby_server.exceptions.common;

import jakarta.validation.ConstraintViolation;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 400 error used for bean validation failures where individual constraint messages are aggregated.
 */
public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Builds a readable message by joining all constraint violation messages with new lines.
     */
    public ValidationException(Set<ConstraintViolation<?>> validationErrors) {
        super(
                validationErrors
                        .stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("\n")),
                HttpStatus.BAD_REQUEST
        );
    }
}
