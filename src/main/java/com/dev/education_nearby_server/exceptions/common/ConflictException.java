package com.dev.education_nearby_server.exceptions.common;

import org.springframework.http.HttpStatus;

/**
 * 409 error for operations that conflict with existing state (e.g. unique constraints).
 */
public class ConflictException extends ApiException {
    public ConflictException() {
        super("Conflict", HttpStatus.CONFLICT);
    }

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

