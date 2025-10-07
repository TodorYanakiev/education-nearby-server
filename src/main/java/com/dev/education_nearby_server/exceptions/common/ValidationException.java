package com.dev.education_nearby_server.exceptions.common;

import jakarta.validation.ConstraintViolation;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.stream.Collectors;

public class ValidationException extends ApiException {
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
