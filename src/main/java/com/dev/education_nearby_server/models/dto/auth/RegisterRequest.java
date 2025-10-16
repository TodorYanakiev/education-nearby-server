package com.dev.education_nearby_server.models.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.dev.education_nearby_server.validation.FieldMatch;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldMatch(first = "password", second = "repeatedPassword", message = "Passwords do not match!")
public class RegisterRequest {

    @NotBlank(message = "The firstname should not be blank!")
    private String firstname;

    @NotBlank(message = "The lastname should not be blank!")
    private String lastname;

    @Email(message = "Invalid email!")
    @NotBlank(message = "The email should not be blank!")
    private String email;

    @NotBlank(message = "The password should not be blank!")
    @Size(min = 8, message = "Password must be at least 8 characters long!")
    private String password;

    @NotBlank(message = "The repeated password should not be blank!")
    private String repeatedPassword;

    @NotBlank(message = "The username should not be blank!")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters!")
    private String username;
}
