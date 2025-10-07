package com.dev.education_nearby_server.exceptions.user;

import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import jakarta.validation.ConstraintViolation;

import java.util.Set;
import java.util.stream.Collectors;

public class UserCreateException extends BadRequestException {

    public UserCreateException(boolean isUnique) {
        super(
                isUnique
                        ? "User with such email already exists!"
                        : "Invalid user data!"
        );
    }

    public UserCreateException(Set<ConstraintViolation<?>> validationErrors) {
        super(
                validationErrors
                        .stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("\n"))
        );
    }

    public UserCreateException(String message) {
        super(message);
    }
}
