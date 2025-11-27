package com.dev.education_nearby_server.exceptions.common;

import org.springframework.http.HttpStatus;

/**
 * 404 error used when a requested resource cannot be found (separate from java.util.NoSuchElementException).
 */
public class NoSuchElementException extends ApiException {
    public NoSuchElementException() {
        super("No such element!", HttpStatus.NOT_FOUND);
    }

    public NoSuchElementException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
