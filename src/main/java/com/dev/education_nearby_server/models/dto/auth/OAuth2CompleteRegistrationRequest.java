package com.dev.education_nearby_server.models.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for completing OAuth2 registration with application-specific fields.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2CompleteRegistrationRequest {

    @NotBlank(message = "Registration token must not be blank!")
    private String registrationToken;

    @NotBlank(message = "Username must not be blank!")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters!")
    private String username;

    @Email(message = "Invalid email!")
    private String email;

    private String firstname;

    private String lastname;

    @Size(max = 500, message = "Description must be at most 500 characters.")
    private String description;
}
