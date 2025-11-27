package com.dev.education_nearby_server.exceptions.common;

import org.springframework.http.HttpStatus;

/**
 * 401 error indicating the request lacks valid authentication credentials.
 */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
