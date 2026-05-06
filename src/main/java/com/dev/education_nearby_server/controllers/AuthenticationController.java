package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.AuthenticationRequest;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.ForgotPasswordRequest;
import com.dev.education_nearby_server.models.dto.auth.OAuth2CompleteRegistrationRequest;
import com.dev.education_nearby_server.models.dto.auth.PasswordResetCodeVerificationRequest;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.dto.auth.ResetForgottenPasswordRequest;
import com.dev.education_nearby_server.services.AuthenticationService;
import com.dev.education_nearby_server.services.oauth2.OAuth2AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Authentication endpoints for registration, login, token refresh, and password recovery flows.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final OAuth2AuthenticationService oAuth2AuthenticationService;

    /**
     * Registers a new user account and returns authentication tokens.
     *
     * @param request validated registration payload
     * @return access/refresh tokens with basic user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    /**
     * Authenticates an existing user with credentials and returns tokens.
     *
     * @param request validated login credentials
     * @return access/refresh tokens when authentication succeeds
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    /**
     * Issues a new access token using the refresh token from the request.
     *
     * @param request incoming HTTP request containing the refresh token
     * @param response response where the new token is written
     * @throws IOException if writing the response fails
     */
    @PostMapping("/refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        service.refreshToken(request, response);
    }

    /**
     * Completes OAuth2 registration by providing application-specific fields.
     *
     * @param request completion payload
     * @return access/refresh tokens on success
     */
    @PostMapping("/oauth2/complete")
    public ResponseEntity<AuthenticationResponse> completeOauth2Registration(
            @Valid @RequestBody OAuth2CompleteRegistrationRequest request
    ) {
        return ResponseEntity.ok(oAuth2AuthenticationService.completeRegistration(request));
    }

    /**
     * Starts the forgot-password flow by sending a verification code if the account exists.
     *
     * @param request email address for the reset flow
     * @return generic status message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(service.requestPasswordReset(request));
    }

    /**
     * Verifies that a forgot-password code is still valid for the supplied email.
     *
     * @param request email and verification code payload
     * @return confirmation message when the code can be used
     */
    @PostMapping("/forgot-password/verify")
    public ResponseEntity<String> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetCodeVerificationRequest request
    ) {
        return ResponseEntity.ok(service.verifyPasswordResetCode(request));
    }

    /**
     * Completes the forgot-password flow by replacing the account password.
     *
     * @param request verified code and new password payload
     * @return confirmation message after the password changes
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<String> resetForgottenPassword(
            @Valid @RequestBody ResetForgottenPasswordRequest request
    ) {
        return ResponseEntity.ok(service.resetForgottenPassword(request));
    }
}
