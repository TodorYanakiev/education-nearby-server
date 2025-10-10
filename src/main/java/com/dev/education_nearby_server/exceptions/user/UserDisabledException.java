package com.dev.education_nearby_server.exceptions.user;

import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;

public class UserDisabledException extends AccessDeniedException {
    public UserDisabledException() {
        super("The user is disabled");
    }
}
