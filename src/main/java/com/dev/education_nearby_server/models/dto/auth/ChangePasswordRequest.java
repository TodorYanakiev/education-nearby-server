package com.dev.education_nearby_server.models.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.dev.education_nearby_server.validation.FieldMatch;

/**
 * Payload for updating a user's password by providing the current password and matching new values.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldMatch(first = "newPassword", second = "confirmationPassword", message = "Passwords do not match!")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password must not be blank!")
    private String currentPassword;

    @NotBlank(message = "New password must not be blank!")
    @Size(min = 8, message = "New password must be at least 8 characters long!")
    private String newPassword;

    @NotBlank(message = "Confirmation password must not be blank!")
    private String confirmationPassword;
}
