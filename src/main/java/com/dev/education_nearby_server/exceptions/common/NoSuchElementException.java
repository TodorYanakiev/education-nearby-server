package com.dev.education_nearby_server.exceptions.common;

import org.springframework.http.HttpStatus;

public class NoSuchElementException extends ApiException {
    public NoSuchElementException() {
        super("No such element!", HttpStatus.NOT_FOUND);
    }

    public NoSuchElementException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
