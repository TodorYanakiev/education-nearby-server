package com.dev.education_nearby_server.exceptions.user;

import com.dev.education_nearby_server.exceptions.common.BadRequestException;

public class UserPasswordException extends BadRequestException {
    public UserPasswordException(String message) {
        super(message);
    }
}
