package com.dev.education_nearby_server.exceptions.common;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
    public ConflictException() {
        super("Conflict", HttpStatus.CONFLICT);
    }

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

