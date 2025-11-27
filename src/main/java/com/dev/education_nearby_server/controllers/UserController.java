package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.services.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * User-facing endpoints for account management.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    /**
     * Allows an authenticated user to update their password.
     *
     * @param request validated payload containing current and new passwords
     * @param connectedUser authenticated principal performing the change
     * @return empty 200 OK when the password was updated
     */
    @PatchMapping
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal connectedUser
    ) {
        service.changePassword(request, connectedUser);
        return ResponseEntity.ok().build();
    }
}
