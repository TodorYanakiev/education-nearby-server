package com.dev.education_nearby_server.models.dto.auth;

import com.dev.education_nearby_server.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for completing a forgot-password flow with a verified code.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldMatch(first = "newPassword", second = "confirmationPassword", message = "Passwords do not match!")
public class ResetForgottenPasswordRequest {

    @NotBlank(message = "Email must not be blank!")
    @Email(message = "Invalid email!")
    private String email;

    @NotBlank(message = "Verification code must not be blank!")
    private String verificationCode;

    @NotBlank(message = "New password must not be blank!")
    @Size(min = 8, message = "New password must be at least 8 characters long!")
    private String newPassword;

    @NotBlank(message = "Confirmation password must not be blank!")
    private String confirmationPassword;
}
