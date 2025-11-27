package com.dev.education_nearby_server.exceptions.common;

import org.springframework.http.HttpStatus;

/**
 * 403 error used when an authenticated user lacks permission to access a resource.
 */
public class AccessDeniedException extends ApiException {
    public AccessDeniedException() {
        super("Access denied!", HttpStatus.FORBIDDEN);
    }

    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
