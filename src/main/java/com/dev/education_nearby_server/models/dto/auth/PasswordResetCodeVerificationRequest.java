package com.dev.education_nearby_server.models.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for verifying a forgot-password code before changing the password.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCodeVerificationRequest {

    @NotBlank(message = "Email must not be blank!")
    @Email(message = "Invalid email!")
    private String email;

    @NotBlank(message = "Verification code must not be blank!")
    private String verificationCode;
}
