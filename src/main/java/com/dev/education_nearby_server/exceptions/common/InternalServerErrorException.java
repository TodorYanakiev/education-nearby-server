package com.dev.education_nearby_server.exceptions.common;

import org.springframework.http.HttpStatus;

/**
 * Fallback 500 error for unexpected runtime failures.
 */
public class InternalServerErrorException extends ApiException {
    public InternalServerErrorException() {
        super("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerErrorException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
