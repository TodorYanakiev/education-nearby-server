package com.dev.education_nearby_server.models.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    @Email(message = "Invalid email!")
    @NotBlank(message = "The email should not be blank!")
    private String email;

    @NotBlank(message = "The password should not be blank!")
    @Size(min = 8, message = "Password must be at least 8 characters long!")
    private String password;
}
