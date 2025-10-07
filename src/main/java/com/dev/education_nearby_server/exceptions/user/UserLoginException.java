package com.dev.education_nearby_server.exceptions.user;

import com.dev.education_nearby_server.exceptions.common.BadRequestException;

public class UserLoginException extends BadRequestException {
    public UserLoginException() {
        super("Invalid credentials!");
    }
}
