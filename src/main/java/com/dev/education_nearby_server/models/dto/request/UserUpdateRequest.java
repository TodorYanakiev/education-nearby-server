package com.dev.education_nearby_server.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for updating mutable user profile fields.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "The firstname should not be blank!")
    private String firstname;

    @NotBlank(message = "The lastname should not be blank!")
    private String lastname;

    @Email(message = "Invalid email!")
    @NotBlank(message = "The email should not be blank!")
    private String email;

    @NotBlank(message = "The username should not be blank!")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters!")
    private String username;

    @Size(max = 500, message = "Description must be at most 500 characters.")
    private String description;
}
